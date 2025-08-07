# Consumer ProGuard rules for WuKongEasySDK

# Keep public API classes and methods
-keep public class com.githubim.easysdk.** {
    public *;
}

# Keep model classes for JSON serialization
-keep class com.githubim.easysdk.model.** { *; }

# Keep enum values
-keepclassmembers enum com.githubim.easysdk.enums.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
