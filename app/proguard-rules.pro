# ---------------------------------------------------------------------------
# NEUROFIT keep rules.
#
# NOTE: minification is currently DISABLED in app/build.gradle.kts. These rules
# are written and kept correct so that isMinifyEnabled/isShrinkResources can be
# flipped to true later without a debugging session through CI logs.
# ---------------------------------------------------------------------------

# Keep line numbers for readable crash traces, hide the original file name.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Annotations are needed by Room, Hilt and kotlinx.serialization.
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions

# NOTE ON -dontwarn: a blanket -dontwarn over a whole package also suppresses the
# missing-class errors that are the only early warning that a keep rule is missing.
# When R8 is switched on, replace these with the exact lines R8 prints into
# app/build/outputs/mapping/release/missing_rules.txt rather than keeping them broad.

# --- Kotlin ---------------------------------------------------------------
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }

# --- Coroutines -----------------------------------------------------------
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# --- Compose --------------------------------------------------------------
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# --- kotlinx.serialization (added in a later phase) ------------------------
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keep, allowobfuscation, allowoptimization class <1> { *; }

# --- Room (added in a later phase) ----------------------------------------
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# --- Hilt (added in a later phase) ----------------------------------------
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$* { *; }

# --- Retrofit / OkHttp (added in a later phase) ---------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# --- WorkManager ----------------------------------------------------------
# The constructor is the part that matters: WorkManager instantiates workers
# reflectively through (Context, WorkerParameters). Keeping the class without
# pinning that constructor lets R8 remove it, and the result is a
# NoSuchMethodException on the phone when the job runs, with nothing visible in CI.
# Worker extends ListenableWorker, so this single rule covers both.
-keep class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

# --- ML Kit barcode (bundled model, added in a later phase) ---------------
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
