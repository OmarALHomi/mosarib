package com.baynana.features.pumps

import kotlinx.coroutines.flow.Flow

class PumpSourceRepository(
    private val pumpDao: PumpSourceDao
) {
    val allPumps: Flow<List<PumpSource>> = pumpDao.getAllPumps()

    suspend fun getPumpById(id: Long): PumpSource? = pumpDao.getPumpById(id)

    suspend fun insertPump(pump: PumpSource): Long = pumpDao.insertPump(pump)

    suspend fun updatePump(pump: PumpSource) = pumpDao.updatePump(pump)

    suspend fun deletePump(pump: PumpSource) = pumpDao.deletePump(pump)
}
