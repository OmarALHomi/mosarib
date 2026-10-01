# ---------------------------------------------------------------------------
# مُسَرِب (Mosarib) - R8 / ProGuard rules for release builds
# ---------------------------------------------------------------------------

# Annotations, generic signatures and inner-class metadata are required at
# runtime by Room, Compose and the Kotlin standard library.
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Keep line numbers so release crash reports stay readable, but hide the
# original file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------------------
# Room (the whole persistence layer of the app)
# ---------------------------------------------------------------------------
# Room resolves the generated `AppDatabase_Impl` by name, therefore the
# generated database implementation has to survive shrinking untouched.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}

# ---------------------------------------------------------------------------
# Enumerations
# ---------------------------------------------------------------------------
# VoucherType / ToastType are persisted by name in the JSON backups and are
# restored through valueOf(), so their constants may not be renamed.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---------------------------------------------------------------------------
# AndroidX
# ---------------------------------------------------------------------------
# FileProvider is declared in the manifest and used to share the generated PDF
# reports and JSON backups with other applications.
-keep class androidx.core.content.FileProvider { *; }
-dontwarn androidx.compose.**

# ---------------------------------------------------------------------------
# Anti-Reverse Engineering & Code Obfuscation Hardening
# ---------------------------------------------------------------------------
# Flatten all classes into the root package to destroy original package hierarchy
-repackageclasses ''
-allowaccessmodification

# Hide source and debug information in release builds
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

