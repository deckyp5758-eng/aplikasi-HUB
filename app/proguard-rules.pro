# ProGuard & R8 Optimization Rules for HUB KEDIRI Fleet App

# Preserve source file and line numbers for stack trace de-obfuscation
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve Annotations & Signatures for Reflection / Serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# -----------------------------------------------------------------
# 1. Retrofit & OkHttp Rules
# -----------------------------------------------------------------
-keepattributes RuntimeVisible*Annotations, RuntimeInvisible*Annotations
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# -----------------------------------------------------------------
# 2. Moshi & Gson Rules (JSON Serialization)
# -----------------------------------------------------------------
-keepclassmembers class * {
    @com.squareup.moshi.* <fields>;
    @com.squareup.moshi.* <methods>;
}
-keepclasseswithmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep class com.squareup.moshi.** { *; }

# Preserve Data Models and Entities to prevent deserialization bugs
-keep class com.example.data.** { *; }
-keepclassmembers class com.example.data.** { *; }

# -----------------------------------------------------------------
# 3. Room Database Rules
# -----------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# -----------------------------------------------------------------
# 4. AndroidX Security Crypto (EncryptedSharedPreferences)
# -----------------------------------------------------------------
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# -----------------------------------------------------------------
# 5. Kotlin Coroutines & Jetpack Compose Rules
# -----------------------------------------------------------------
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
-keep class androidx.compose.** { *; }

