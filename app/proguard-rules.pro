-keepattributes *Annotation*
-keep class com.securebank.app.** { *; }
-keep class com.securebank.app.models.** { *; }
-keepclassmembers class com.securebank.app.models.** { *; }
# Retrofit
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
# OkHttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
# Gson
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
