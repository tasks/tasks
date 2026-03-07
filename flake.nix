{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    devenv.url = "github:cachix/devenv";
  };

  outputs = { self, nixpkgs, devenv, ... } @ inputs:
    let
      systems = [ "x86_64-linux" "i686-linux" "x86_64-darwin" "aarch64-linux" "aarch64-darwin" ];
      forEachSystem = nixpkgs.lib.genAttrs systems;
    in
    {
      devShells = forEachSystem (system:
        let
          pkgs = import nixpkgs {
            inherit system;
            config.allowUnfree = true;
            config.android_sdk.accept_license = true;
          };
          androidSdk = pkgs.androidenv.composeAndroidPackages {
            cmdLineToolsVersion = "13.0";
            buildToolsVersions = [ "34.0.0" "35.0.0" ];
            platformVersions = [ "34" "35" "36" ];
            includeEmulator = false;
            includeSystemImages = false;
          };
          devenv-profile = "/home/repparw/code/tasks/.devenv/profile";
        in
        {
          default = devenv.lib.mkShell {
            inherit inputs pkgs;
            modules = [
              ({ lib, ... }: {
                devenv.root = "/home/repparw/code/tasks";
                languages.java.enable = true;
                languages.java.jdk.package = pkgs.jdk21;

                packages = with pkgs; [
                  androidSdk.androidsdk
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
                  protobuf
                  protoc-gen-grpc-java
                ];

                env.ANDROID_SDK_ROOT = lib.mkForce "${devenv-profile}/libexec/android-sdk";
                env.ANDROID_HOME = lib.mkForce "${devenv-profile}/libexec/android-sdk";
                env.PROTOC_GEN_GRPC_JAVA = "${pkgs.protoc-gen-grpc-java}/bin/protoc-gen-grpc-java";

                scripts.build-app.exec = "JAVA_HOME=${devenv-profile}/lib/openjdk ANDROID_HOME=${devenv-profile}/libexec/android-sdk ANDROID_SDK_ROOT=${devenv-profile}/libexec/android-sdk PROTOC_GEN_GRPC_JAVA=${pkgs.protoc-gen-grpc-java}/bin/protoc-gen-grpc-java ./gradlew assembleGenericDebug";
                scripts.test-app.exec = "JAVA_HOME=${devenv-profile}/lib/openjdk ANDROID_HOME=${devenv-profile}/libexec/android-sdk ANDROID_SDK_ROOT=${devenv-profile}/libexec/android-sdk PROTOC_GEN_GRPC_JAVA=${pkgs.protoc-gen-grpc-java}/bin/protoc-gen-grpc-java ./gradlew test";
                scripts.clean-app.exec = "JAVA_HOME=${devenv-profile}/lib/openjdk ANDROID_HOME=${devenv-profile}/libexec/android-sdk ANDROID_SDK_ROOT=${devenv-profile}/libexec/android-sdk ./gradlew clean";

                enterShell = ''
                  export PROTOC_GEN_GRPC_JAVA=${pkgs.protoc-gen-grpc-java}/bin/protoc-gen-grpc-java
                  if ! grep -q "android.aapt2FromMavenOverride" gradle.properties 2>/dev/null; then
                    echo "android.aapt2FromMavenOverride=${devenv-profile}/libexec/android-sdk/build-tools/34.0.0/aapt2" >> gradle.properties
                  fi
                  echo "Android Development Environment"
                  echo "Java: ${pkgs.jdk21.version}"
                  echo "Android SDK: $ANDROID_HOME"
                '';
              })
            ];
          };
        });
    };
}
