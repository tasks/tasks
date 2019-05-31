[![Get it on Google Play](https://img.shields.io/github/release-pre/tasks/tasks.svg?label=google%20play)](https://play.google.com/store/apps/details?id=org.tasks&utm_source=global_co&utm_medium=prtnr&utm_content=Mar2515&utm_campaign=PartBadge&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1)
[![Get it on Amazon App Store](https://img.shields.io/github/release/tasks/tasks.svg?label=amazon)](https://www.amazon.com/gp/product/B00QHGTL7O/ref=mas_pm_tasks_astrid_to_do_list_clone)
[![Get it on F-Droid](https://img.shields.io/f-droid/v/org.tasks.svg)](https://f-droid.org/packages/org.tasks/)

[![Donate with Bitcoin](https://img.shields.io/badge/bitcoin-donate-yellow.svg?logo=bitcoin)](https://en.cryptobadges.io/donate/136mW34jW3cmZKhxuTDn3tHXMRwbbaRU8s)
[![PayPal donate button](https://img.shields.io/badge/paypal-donate-yellow.svg?logo=paypal)](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=alex@tasks.org)
[![Liberapay donate button](https://img.shields.io/liberapay/receives/tasks.svg?logo=liberapay)](https://liberapay.com/tasks/donate)

[![Build Status](https://travis-ci.org/tasks/tasks.svg?branch=master)](https://travis-ci.org/tasks/tasks) [![weblate](https://weblate.tasks.org/widgets/tasks/-/android/svg-badge.svg)](https://weblate.tasks.org/engage/tasks/?utm_source=widget) [![codecov](https://codecov.io/gh/tasks/tasks/branch/master/graph/badge.svg)](https://codecov.io/gh/tasks/tasks)

Please visit [tasks.org](https://tasks.org) for end user documentation and support

---

#### To get started with development:
1. [Fork](https://help.github.com/articles/fork-a-repo/) and [clone](https://help.github.com/articles/cloning-a-repository/) the repository
    * command line users: clone with `--recurse-submodules` or run `git submodule update --init` after cloning
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
