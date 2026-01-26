# Add project specific ProGuard rules here.

# Keep RunAnywhere SDK
-keep class com.runanywhere.sdk.** { *; }
-keepclassmembers class com.runanywhere.sdk.** { *; }

# Keep Room entities
-keep class com.aurafarmers.hetu.data.local.entity.** { *; }

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
