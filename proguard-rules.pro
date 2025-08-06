# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep WuKongEasySDK public API
-keep public class com.wukongim.easysdk.WuKongEasySDK {
    public *;
}

-keep public class com.wukongim.easysdk.WuKongConfig {
    public *;
}

-keep public class com.wukongim.easysdk.WuKongConfig$Builder {
    public *;
}

-keep public enum com.wukongim.easysdk.enums.** {
    *;
}

-keep public interface com.wukongim.easysdk.listener.** {
    *;
}

-keep public class com.wukongim.easysdk.model.** {
    *;
}

-keep public class com.wukongim.easysdk.exception.** {
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
