fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android deploy

```sh
[bundle exec] fastlane android deploy
```

Upload to a track, production rolls out to 5%

### android check_version_code

```sh
[bundle exec] fastlane android check_version_code
```

Fail if VERSION_CODE is not bumped

### android download_signed_apk

```sh
[bundle exec] fastlane android download_signed_apk
```

Download Play-signed universal APK

### android lint

```sh
[bundle exec] fastlane android lint
```

Lint

### android bundle

```sh
[bundle exec] fastlane android bundle
```

Bundle

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
