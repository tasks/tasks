{ pkgs ? import <nixpkgs> { config.allowUnfree = true; } }:

let
  androidComposition = pkgs.androidenv.composeAndroidPackages {
    cmdLineToolsVersion = "13.0";
    buildToolsVersions = [ "34.0.0" ];
    platformVersions = [ "35" ];
    includeEmulator = false;
    includeSystemImages = false;
  };
in
pkgs.mkShell {
  name = "android-check";
  ANDROID_SDK_ROOT = "${androidComposition.androidsdk}/libexec/android-sdk";
}
