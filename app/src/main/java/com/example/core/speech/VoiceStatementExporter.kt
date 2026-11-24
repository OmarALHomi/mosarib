package com.example.core.speech

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.core.database.AppDatabase
import com.example.core.util.FileSharingHelper
import com.example.features.customers.Customer
import com.example.features.sessions.WaterSession
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherType
import com.example.features.wellowners.WellOwnerPurchase
import com.example.features.wellowners.WellOwnerPurchaseMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Date

/**
 * Handles synthesis of voice statements into WAV, fast encoding to AAC M4A container,
 * and direct sharing via WhatsApp.
 */
object VoiceStatementExporter {

    private const val TAG = "VoiceStatementExporter"

    data class WavInfo(
        val sampleRate: Int,
        val channelCount: Int,
        val bitsPerSample: Int,
        val dataOffset: Long,
        val dataSize: Long
    )

    private fun parseWavHeader(file: File): WavInfo? {
        if (!file.exists() || file.length() < 44) return null
        return try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(44)
                val read = fis.read(header)
                if (read < 44) return null

                val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

                val riff = String(header, 0, 4)
                val wave = String(header, 8, 4)
                if (riff != "RIFF" || wave != "WAVE") return null

                var pos = 12L
                var sampleRate = 16000
                var channels = 1
                var bitsPerSample = 16
                var dataOffset = 44L
                var dataSize = file.length() - 44

                while (pos + 8L <= file.length()) {
                    val chunkHeader = ByteArray(8)
                    val chunkRead = FileInputStream(file).use { chunkFis ->
                        chunkFis.channel.position(pos)
                        chunkFis.read(chunkHeader)
                    }
                    if (chunkRead < 8) break

                    val chunkBuf = ByteBuffer.wrap(chunkHeader).order(ByteOrder.LITTLE_ENDIAN)
                    val chunkId = String(chunkHeader, 0, 4)
                    val chunkSize = chunkBuf.getInt(4).toLong() and 0xFFFFFFFFL

                    if (chunkId == "fmt " && chunkSize >= 16L) {
                        val fmtLen = chunkSize.coerceAtMost(40L).toInt()
                        val fmtData = ByteArray(fmtLen)
                        val fmtRead = FileInputStream(file).use { fmtFis ->
                            fmtFis.channel.position(pos + 8L)
                            fmtFis.read(fmtData)
                        }
                        if (fmtRead >= 16) {
                            val fmtBuf = ByteBuffer.wrap(fmtData).order(ByteOrder.LITTLE_ENDIAN)
                            channels = fmtBuf.getShort(2).toInt()
                            sampleRate = fmtBuf.getInt(4)
                            bitsPerSample = fmtBuf.getShort(14).toInt()
                        }
                    } else if (chunkId == "data") {
                        dataOffset = pos + 8L
                        dataSize = chunkSize
                        break
                    }
                    val step = 8L + chunkSize
                    if (step <= 0L || pos + step > file.length()) break
                    pos += step
                }

                WavInfo(sampleRate, channels, bitsPerSample, dataOffset, dataSize)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Fast encodes a raw PCM WAV file into an AAC M4A container (mono, 32 kbps).
     * Deletes the source WAV file on success or failure to avoid leaving orphans.
     */
    suspend fun convertWavToM4a(wavFile: File, m4aFile: File): Result<File> = withContext(Dispatchers.IO) {
        val encodeStartMs = System.currentTimeMillis()
        val wavInfo = parseWavHeader(wavFile)
        if (wavInfo == null) {
            wavFile.delete()
            return@withContext Result.failure(IOException("Failed to parse WAV header from ${wavFile.name}"))
        }

        val sampleRate = wavInfo.sampleRate
        val channels = wavInfo.channelCount.coerceAtLeast(1)

        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 32000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 32768)
        }

        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var pcmInputStream: FileInputStream? = null
        var trackIndex = -1
        var muxerStarted = false

        if (m4aFile.exists()) {
            m4aFile.delete()
        }

        try {
            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            muxer = MediaMuxer(m4aFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            pcmInputStream = FileInputStream(wavFile)
            pcmInputStream.skip(wavInfo.dataOffset)

            val bufferInfo = MediaCodec.BufferInfo()
            val chunkBuffer = ByteArray(32768)
            var sawInputEOS = false
            var sawOutputEOS = false
            var presentationTimeUs = 0L
            val bytesPerSample = (wavInfo.bitsPerSample / 8).coerceAtLeast(1) * channels

            while (!sawOutputEOS) {
                if (!sawInputEOS) {
                    val inIdx = codec.dequeueInputBuffer(5000L)
                    if (inIdx >= 0) {
                        val inBuf = codec.getInputBuffer(inIdx)
                        if (inBuf != null) {
                            inBuf.clear()
                            val toRead = minOf(inBuf.remaining(), chunkBuffer.size)
                            val read = pcmInputStream.read(chunkBuffer, 0, toRead)
                            if (read <= 0) {
                                sawInputEOS = true
                                codec.queueInputBuffer(inIdx, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            } else {
                                inBuf.put(chunkBuffer, 0, read)
                                codec.queueInputBuffer(inIdx, 0, read, presentationTimeUs, 0)
                                val samples = read / bytesPerSample
                                presentationTimeUs += (samples * 1_000_000L) / sampleRate
                            }
                        }
                    }
                }

                // Drain all available output buffers in non-blocking loop
                while (true) {
                    val outIdx = codec.dequeueOutputBuffer(bufferInfo, 0L)
                    if (outIdx == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break
                    } else if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        val newFormat = codec.outputFormat
                        trackIndex = muxer.addTrack(newFormat)
                        muxer.start()
                        muxerStarted = true
                    } else if (outIdx >= 0) {
                        val outBuf = codec.getOutputBuffer(outIdx)
                        if (outBuf != null && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && bufferInfo.size > 0 && muxerStarted) {
                            outBuf.position(bufferInfo.offset)
                            outBuf.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(trackIndex, outBuf, bufferInfo)
                        }
                        codec.releaseOutputBuffer(outIdx, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            sawOutputEOS = true
                            break
                        }
                    }
                }
            }

            val encodeEndMs = System.currentTimeMillis()
            Log.i(TAG, "M4A encoding finished in ${encodeEndMs - encodeStartMs} ms")

            if (m4aFile.exists() && m4aFile.length() > 0) {
                wavFile.delete()
                Result.success(m4aFile)
            } else {
                wavFile.delete()
                Result.failure(IOException("Generated M4A file is empty"))
            }
        } catch (e: Exception) {
            wavFile.delete()
            m4aFile.delete()
            Result.failure(e)
        } finally {
            try { pcmInputStream?.close() } catch (_: Exception) {}
            try { codec?.stop() } catch (_: Exception) {}
            try { codec?.release() } catch (_: Exception) {}
            try {
                if (muxerStarted) {
                    muxer?.stop()
                }
            } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
        }
    }

    /**
     * Shares the synthesized M4A file via WhatsApp (or system chooser).
     */
    fun shareVoiceFile(
        context: Context,
        audioFile: File,
        phone: String? = null,
        title: String = "كشف حساب صوتي",
        caption: String? = null
    ) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                audioFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                if (!caption.isNullOrBlank()) {
                    putExtra(Intent.EXTRA_TEXT, caption)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val cleanPhone = if (!phone.isNullOrBlank()) FileSharingHelper.normalizePhone(phone) else ""
            if (cleanPhone.isNotEmpty()) {
                shareIntent.putExtra("jid", "$cleanPhone@s.whatsapp.net")
            }

            val pm = context.packageManager
            val isWhatsApp = isAppInstalled("com.whatsapp", pm)
            val isWhatsAppBiz = isAppInstalled("com.whatsapp.w4b", pm)

            if (isWhatsApp) {
                shareIntent.setPackage("com.whatsapp")
                context.startActivity(shareIntent)
            } else if (isWhatsAppBiz) {
                shareIntent.setPackage("com.whatsapp.w4b")
                context.startActivity(shareIntent)
            } else {
                val chooser = Intent.createChooser(shareIntent, title).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر مشاركة الكشف الصوتي: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isAppInstalled(pkg: String, pm: PackageManager): Boolean {
        return try {
            pm.getPackageInfo(pkg, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    private data class OpSortItem(val timestamp: Long, val sentence: String)

    /**
     * Generates a voice statement for the given customer, synthesizes audio,
     * fast encodes to M4A, and triggers WhatsApp sharing.
     */
    suspend fun generateAndShareVoiceStatement(
        context: Context,
        customer: Customer,
        balance: Double,
        isWellOwner: Boolean = customer.isWellOwner,
        payableBalance: Double = 0.0,
        receivableBalance: Double = 0.0,
        currencySymbol: String = "ر.ي",
        distributorName: String = "",
        sessions: List<WaterSession>? = null,
        vouchers: List<Voucher>? = null,
        purchases: List<WellOwnerPurchase>? = null,
        onStatus: (SpeechStatus) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val totalStartMs = System.currentTimeMillis()
        val status = MosaribSpeaker.prepare(context)
        if (status != SpeechStatus.Ready) {
            withContext(Dispatchers.Main) {
                onStatus(status)
            }
            return@withContext Result.failure(IllegalStateException("TTS not ready: $status"))
        }

        val db = AppDatabase.getDatabase(context)
        val finalSessions = sessions ?: db.waterSessionDao().getSessionsForCustomer(customer.id).first()
        val finalVouchers = vouchers ?: db.voucherDao().getVouchersForCustomerDirect(customer.id)
        val finalPurchases = purchases ?: if (isWellOwner) {
            db.wellOwnerPurchaseDao().getPurchasesForOwner(customer.id).first()
        } else emptyList()

        val dist = if (distributorName.isNotBlank()) distributorName else {
            db.appSettingDao().getSettingValue("distributor_name") ?: "المسرّب"
        }
        val distPhone = db.appSettingDao().getSettingValue("distributor_phone") ?: ""
        val curr = if (currencySymbol.isNotBlank()) currencySymbol else {
            db.appSettingDao().getSettingValue("currency_symbol") ?: "ر.ي"
        }

        val opItems = mutableListOf<OpSortItem>()

        for (s in finalSessions) {
            val sent = SpeechTextBuilder.buildStatementSessionLine(s, s.startTime)
            opItems.add(OpSortItem(s.startTime, sent))
        }

        for (v in finalVouchers) {
            val sent = when (v.type) {
                VoucherType.RECEIPT -> SpeechTextBuilder.buildStatementReceiptLine(v.amount, v.date)
                VoucherType.EXPENSE -> SpeechTextBuilder.buildStatementExpenseLine(v.amount, v.date)
                VoucherType.DISCOUNT -> SpeechTextBuilder.buildStatementDiscountLine(v.amount, v.date)
            }
            opItems.add(OpSortItem(v.date, sent))
        }

        for (p in finalPurchases) {
            val sent = SpeechTextBuilder.buildStatementPurchaseLine(p.durationMinutes, WellOwnerPurchaseMath.payableAmount(p), p.date)
            opItems.add(OpSortItem(p.date, sent))
        }

        val sortedOpSentences = opItems.sortedByDescending { it.timestamp }.take(10).map { it.sentence }

        val balanceSentence = SpeechTextBuilder.buildBalanceSentence(
            customerName = customer.name,
            balance = balance,
            currency = curr,
            isWellOwner = isWellOwner,
            payableBalance = payableBalance,
            receivableBalance = receivableBalance
        )

        val fullStatementText = SpeechTextBuilder.buildCustomerVoiceStatement(
            customerName = customer.name,
            distributorName = dist,
            date = Date(),
            balanceSentence = balanceSentence,
            operationSentences = sortedOpSentences
        )

        val voiceDir = File(context.cacheDir, "voice").apply { mkdirs() }
        // Clean up any 0-byte or orphaned WAV files
        voiceDir.listFiles()?.filter { it.extension == "wav" && it.length() == 0L }?.forEach { it.delete() }

        val timestamp = System.currentTimeMillis()
        val wavFile = File(voiceDir, "statement_${customer.id}_$timestamp.wav")

        val synthStartMs = System.currentTimeMillis()
        val synthResult = MosaribSpeaker.synthesizeToFile(fullStatementText, wavFile)
        val synthEndMs = System.currentTimeMillis()
        if (synthResult.isFailure) {
            if (wavFile.exists() && wavFile.length() == 0L) {
                wavFile.delete()
            }
            return@withContext Result.failure(synthResult.exceptionOrNull() ?: RuntimeException("TTS synthesis failed"))
        }

        Log.i(TAG, "TTS synthesis took ${synthEndMs - synthStartMs} ms for text (${fullStatementText.length} chars, ~${fullStatementText.split(' ').size} words)")

        val m4aFile = File(voiceDir, "statement_${customer.id}_$timestamp.m4a")
        val convertResult = convertWavToM4a(wavFile, m4aFile)
        if (convertResult.isFailure) {
            if (wavFile.exists()) wavFile.delete()
            return@withContext Result.failure(convertResult.exceptionOrNull() ?: RuntimeException("M4A conversion failed"))
        }

        val finalFile = convertResult.getOrThrow()
        val totalEndMs = System.currentTimeMillis()
        Log.i(TAG, "Full pipeline took ${totalEndMs - totalStartMs} ms. File: ${finalFile.name}, size: ${finalFile.length()} bytes (${finalFile.length() / 1024} KB)")

        val caption = SpeechTextBuilder.buildVoiceStatementCaption(
            customerName = customer.name,
            balance = balance,
            isWellOwner = isWellOwner,
            payableBalance = payableBalance,
            receivableBalance = receivableBalance,
            operationCount = sortedOpSentences.size,
            date = Date(),
            distributorName = dist,
            distributorPhone = distPhone,
            currencySymbol = curr
        )

        val shareResult = withContext(Dispatchers.Main) {
            try {
                shareVoiceFile(
                    context = context,
                    audioFile = finalFile,
                    phone = customer.phone,
                    title = "كشف حساب صوتي - ${customer.name}",
                    caption = caption
                )
                Result.success(finalFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        shareResult
    }
}
