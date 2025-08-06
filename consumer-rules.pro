# Consumer ProGuard rules for WuKongEasySDK

# Keep public API classes and methods
-keep public class com.wukongim.easysdk.** {
    public *;
}

# Keep model classes for JSON serialization
-keep class com.wukongim.easysdk.model.** { *; }

# Keep enum values
-keepclassmembers enum com.wukongim.easysdk.enums.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
