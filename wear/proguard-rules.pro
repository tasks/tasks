# remove logging statements
-assumenosideeffects class timber.log.Timber* {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}

-dontwarn com.franmontiel.persistentcookiejar.**
-dontwarn com.github.erosb.jsonsKema.**
-dontwarn com.google.android.gms.**
-dontwarn groovy.**
-dontwarn java.beans.Transient
-dontwarn javax.cache.**
-dontwarn org.codehaus.groovy.**
-dontwarn org.jparsec.**
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { <fields>; }
