-dontshrink

# appcompat-v21 not compatible with some samsung 4.2.2 roms
# https://code.google.com/p/android/issues/detail?id=78377
-keep class !android.support.v7.internal.view.menu.**,** {*;}

-keepattributes **

-dontwarn **
