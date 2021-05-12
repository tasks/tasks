### Translation

You can translate Tasks using [Weblate](https://hosted.weblate.org/projects/tasks/android). To get started, register a new account or login with your GitHub account if you have one.

### Opening issues

Before opening an issue, please make sure that your issue:
- is not a duplicate (i.e. it has not been reported before, closed or open)
- has not been fixed
- is in English (issues in a language other than English will be closed unless someone translates them)
- does not contain multiple feature requests/bug reports. Please open a separate issue for each one.

### Code contribution

#### To get started with development:
1. [Fork](https://help.github.com/articles/fork-a-repo/) and [clone](https://help.github.com/articles/cloning-a-repository/) the repository
2. Install and launch [Android Studio's canary build](https://developer.android.com/studio/preview) (Tasks depends on some bleeding-edge features of the canary build, but in the future when those features are stabilized, you will be able to use the stable release of Android Studio)
3. Select `File > Open`, select the Tasks directory, and accept prompts to install missing SDK components

#### Set up Mapbox
1. Register at [mapbox.com](https://www.mapbox.com)
2. Add `tasks_mapbox_key_debug="<your_api_key>"` to your [`gradle.properties`](https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_configuration_properties) file. You can create an access token or use your [default public token](https://docs.mapbox.com/help/glossary/access-token/#default-public-token)

#### Set up Google Tasks and Google Drive
1. Register at [cloud.google.com](https://cloud.google.com)
2. Enable [Google Tasks API](https://console.cloud.google.com/apis/library/tasks.googleapis.com) and [Google Drive API](https://console.cloud.google.com/apis/library/drive.googleapis.com)
3. [Create android authorization credentials](https://developers.google.com/identity/protocols/OAuth2InstalledApp#creatingcred)

#### Set up Google Maps and Google Places
1. Register at [cloud.google.com](https://cloud.google.com)
2. Enable [Google Maps SDK](https://console.cloud.google.com/apis/library/maps-android-backend.googleapis.com) and [Google Places API](https://console.cloud.google.com/apis/library/places-backend.googleapis.com)
3. [Set up an API key](https://cloud.google.com/video-intelligence/docs/common/auth#set_up_an_api_key)
4. Add `tasks_google_key_debug="<your_api_key>"` to your [`gradle.properties`](https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_configuration_properties) file
5. Select `Build > Select Build Variant` and choose the `googleplay` variant
