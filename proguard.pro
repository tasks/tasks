-dontobfuscate

# https://code.google.com/p/android/issues/detail?id=78293
-keep public class android.support.v7.widget.** { *; }
-keep public class android.support.v7.internal.widget.** { *; }
-keep public class android.support.v7.internal.view.menu.** { *; }
-keep public class * extends android.support.v4.view.ActionProvider {
    public <init>(android.content.Context);
}

# google-rfc-2445-20110304
-dontwarn com.google.ical.compat.jodatime.**

# https://github.com/JakeWharton/butterknife/blob/dd54788c2629e2bdd64232de7ccfa8d58910e06c/butterknife/proguard-rules.txt
-keep public class * implements butterknife.internal.ViewBinder { public <init>(); }
-keep class butterknife.*
-keepclasseswithmembernames class * { @butterknife.* <methods>; }
-keepclasseswithmembernames class * { @butterknife.* <fields>; }

# guava
-dontwarn sun.misc.Unsafe
-dontwarn java.lang.ClassValue
-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
