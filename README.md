Astrid was a popular cross-platform productivity service that was [acquired](https://web.archive.org/web/20130811052500/http://blog.astrid.com/blog/2013/05/01/yahoo-acquires-astrid/) and [discontinued](https://techcrunch.com/2013/07/06/astrid-goes-dark-august-5-goodnight-sweet-squid/) in 2013. The source code from Astrid's open source Android app serves as the basis of Tasks.

[<img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png"
    alt="Get it on Google Play"
    height="80">](https://play.google.com/store/apps/details?id=org.tasks)
[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
    alt="Get it on F-Droid"
    height="80">](https://f-droid.org/packages/org.tasks)

Please visit [tasks.org](https://tasks.org) for end user documentation and support

---

[![Donate with Bitcoin](https://img.shields.io/badge/bitcoin-donate-yellow.svg?logo=bitcoin)](https://en.cryptobadges.io/donate/136mW34jW3cmZKhxuTDn3tHXMRwbbaRU8s)
[![PayPal donate button](https://img.shields.io/badge/paypal-donate-yellow.svg?logo=paypal)](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=alex@tasks.org)
[![Liberapay donate button](https://img.shields.io/liberapay/receives/tasks.svg?logo=liberapay)](https://liberapay.com/tasks/donate)

[![Build Status](https://travis-ci.com/tasks/tasks.svg?branch=main)](https://travis-ci.com/tasks/tasks) [![weblate](https://hosted.weblate.org/widgets/tasks/-/android/svg-badge.svg)](https://hosted.weblate.org/engage/tasks/?utm_source=widget) [![codecov](https://codecov.io/gh/tasks/tasks/branch/main/graph/badge.svg)](https://codecov.io/gh/tasks/tasks) [![codebeat badge](https://codebeat.co/badges/07924fca-2f18-4eff-99a3-120ec5ac2d5f)](https://codebeat.co/projects/github-com-tasks-tasks-main)

#### To get started with development:
1. [Fork](https://help.github.com/articles/fork-a-repo/) and [clone](https://help.github.com/articles/cloning-a-repository/) the repository
2. Install and launch [Android Studio](https://developer.android.com/studio/index.html)
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
