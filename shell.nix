{ pkgs ? import <nixpkgs> {} }:

(if pkgs ? buildFHSUserEnv then pkgs.buildFHSUserEnv else pkgs.buildFHSEnv) {
  name = "android-tasks-shell";
  targetPkgs = pkgs: (with pkgs; [
    jdk17
    git
    unzip
    wget
    zip
    glibc
    ncurses5
    zlib
    xorg.libX11
    xorg.libXext
    xorg.libXrender
    xorg.libXtst
    xorg.libXi
    freetype
    fontconfig
  ]);
  runScript = "bash";
  
  profile = ''
    export JAVA_HOME=${pkgs.jdk17.home}
    export ANDROID_SDK_ROOT=$HOME/Android/Sdk
  '';
}
