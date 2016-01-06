-dontshrink

# appcompat-v7 v23.1.1 not compatible with some samsung 4.2 roms
# https://code.google.com/p/android/issues/detail?id=78377
-keep class !android.support.v7.view.menu.**,** {*;}

-keepattributes **

-dontwarn **
