# Development Environment Setup

## Using Nix

This project uses Nix for development environment management. There are multiple ways to set up the development environment:

### 1. Existing devenv Environment

The project has an existing devenv profile that includes necessary development tools:

```bash
# Use the existing profile directly
JAVA_HOME=".devenv/profile/lib/openjdk" PATH=".devenv/profile/bin:$PATH" ./gradlew build
```

### 2. Using nix-shell

```bash
# Quick access to Java and build tools
nix-shell -p openjdk --run "./gradlew build"

# For more complex development, include Android tools
nix-shell -p openjdk android-studio --run "./gradlew build"
```

### 3. Creating a devenv.nix

Create `devenv.nix` for a proper development environment:

```nix
{ pkgs ? import <nixpkgs> {} }:

{
  # Package definition
  pkgs ? import <nixpkgs> {} : pkgs
}.devenv ({
  shell = {
    packages = with pkgs; [
      openjdk
      android-sdk
      android-studio
      gradle
      git
    ];
    
    env = {
      ANDROID_HOME = "${pkgs.android-sdk}/libexec/android-sdk";
      ANDROID_SDK_ROOT = "${pkgs.android-sdk}/libexec/android-sdk";
      JAVA_HOME = "${pkgs.openjdk}/lib/openjdk";
    };
    
    enterShell = true;
  };
})
```

Then use:
```bash
devenv shell
```

### 4. Using the existing .devenv profile

The project already has a built devenv profile in `.devenv/profile/`:

```bash
# Activate the profile
source .devenv/profile/etc/profile.d/devenv-env.sh

# Or use tools directly
.devenv/profile/bin/java -version
.devenv/profile/bin/adb devices
```

## Build Commands

Once environment is set up:

```bash
# Test
./gradlew test

# Build debug APK
./gradlew assembleDebug

# Install to connected device
adb install app/build/outputs/apk/generic/debug/app-generic-debug.apk
```

## Troubleshooting

### Android SDK License Issues

If you encounter Android SDK license issues:

1. Accept licenses manually:
```bash
ANDROID_HOME="/path/to/sdk" yes | sdkmanager --licenses
```

2. Or bypass with local.properties:
```properties
sdk.dir=/path/to/android-sdk
```

### Java Issues

If JAVA_HOME is not set:
```bash
# Find Java in devenv profile
JAVA_HOME=".devenv/profile/lib/openjdk"

# Or use nix-shell
nix-shell -p openjdk --run "java -version"
```

## Recommended Workflow

For day-to-day development:

1. Use the existing devenv profile (quickest):
```bash
JAVA_HOME=".devenv/profile/lib/openjdk" PATH=".devenv/profile/bin:$PATH" ./gradlew build
```

2. Or create a proper devenv.nix for reproducible environment

3. Ensure your local.properties points to the correct Android SDK path if needed
