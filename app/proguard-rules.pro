# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-optimizations
# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
-keepnames class * extends io.github.nexalloy.morphe.Fingerprint

# FemAlloy: the HideEndScreen patch registers UI preferences, a litho filter and platform-class
# hooks via constructor side effects; keep it intact in release builds regardless of R8's analysis.
-keep class io.github.nexalloy.morphe.youtube.layout.hide.endscreen.** { *; }

# FemAlloy: settings registered only by their constructor side effect look unused to R8,
# and the -assumenosideeffects rule below then drops the registration entirely in release
# builds, breaking Setting.getSettingFromPath(). The refresh-rate-type setting is declared
# in the parent SharedYouTubeSettings (EnumSetting) and is read by BaseAppRefreshRatePatch;
# keep it so the registration side effect survives.
-keepclassmembers class app.morphe.extension.shared.settings.SharedYouTubeSettings {
    public static final app.morphe.extension.shared.settings.EnumSetting APP_REFRESH_RATE_TYPE;
}
-keepclassmembers class **.* {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keep public class * extends android.graphics.drawable.Drawable { public <init>(...); }

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-assumenosideeffects class app.morphe.extension.shared.settings.* {
    public <init>(...);
}

#
-dontwarn io.github.libxposed.annotation.**
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}