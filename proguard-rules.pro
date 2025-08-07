# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep WuKongEasySDK public API
-keep public class com.githubim.easysdk.WuKongEasySDK {
    public *;
}

-keep public class com.githubim.easysdk.WuKongConfig {
    public *;
}

-keep public class com.githubim.easysdk.WuKongConfig$Builder {
    public *;
}

-keep public enum com.githubim.easysdk.enums.** {
    *;
}

-keep public interface com.githubim.easysdk.listener.** {
    *;
}

-keep public class com.githubim.easysdk.model.** {
    *;
}

-keep public class com.githubim.easysdk.exception.** {
    *;
}

# Keep Gson annotations
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
