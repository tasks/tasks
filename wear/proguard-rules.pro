-dontobfuscate
-keepattributes SourceFile,LineNumberTable

-keep class org.tasks.** { *; }

# remove logging statements
-assumenosideeffects class timber.log.Timber* {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}

-dontwarn com.google.android.gms.**
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { <fields>; }
-keep class androidx.compose.material.icons.outlined.** { *; }
