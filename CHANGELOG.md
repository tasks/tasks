### 11.5.2 (2021-02-25)

* Fix CalDAV sync error
* Report errors when generating recurrence dates

### 11.5.1 (2021-02-24)

* Fix 'repeat until' date
* Fix repeat dates for UTC+13
  ([#1374](https://github.com/tasks/tasks/issues/1374))
* F-Droid: Handle null name in Nominatim reverse geocoder
  ([#1380](https://github.com/tasks/tasks/issues/1380))
* Update translations
  * Basque - Sergio Varela
  * Croatian - @ggdorman
  * Dutch - @fvbommel
  * French - @FlorianLeChat
  * Hungarian - kaciokos
  * Norwegian Bokmål - @comradekingu
  * Polish - @alex-ter
  * Russian - Nikita Epifanov
  * Simplified Chinese - @sr093906
  * Spanish - @FlorianLeChat
  * Turkish - Oğuz Ersen
  * Ukrainian - @IhorHordiichuk
  * Urdu - Maaz

### 11.5 (2021-02-17)

* Sync snooze time with Tasks.org, DAVx⁵, CalDAV, EteSync, and DecSync
  * Compatible with Thunderbird
* New map theme preference
* 10 new icons
* F-Droid: Use Nominatim for reverse geocoding
* Google Play: Use OpenStreetMap tiles when Play Services not available
* Google Play: Use Android location services when Play Services not available
* Tasks.org accounts: Use Google Places for map search
* Update translations
  * Dutch - @fvbommel
  * French - @FlorianLeChat
  * Hungarian - kaciokos
  * Indonesian - when we were sober
  * Simplified Chinese - @sr093906
  * Spanish - @FlorianLeChat
  * Ukrainian - @IhorHordiichuk

### 11.4 (2021-02-09)

* Sync collapsed subtask state with Tasks.org, DAVx⁵, CalDAV, EteSync, and
  DecSync ([#1339](https://github.com/tasks/tasks/issues/1339))
  * Compatible with Nextcloud and ownCloud
* F-Droid: Add location based reminders ([#770](https://github.com/tasks/tasks/issues/770))
* F-Droid: Replace Mapbox tiles with OpenStreetMap tiles ([#922](https://github.com/tasks/tasks/issues/922))
* Fix default start date ([#1350](https://github.com/tasks/tasks/issues/1350))

### 11.3.4 (2021-02-03)

* Adjust start times by one second during sync
  ([#1326](https://github.com/tasks/tasks/issues/1326))
  * Can now sync start time = due time with DAVx⁵, EteSync app, and DecSync CC
  * All day start date must come before all day due date with DAVx⁵, EteSync
    app, and DecSync CC
* 'Show unstarted' toggled on by default

### 11.3.3 (2021-01-30)

* Fix all-day due date synchronization
  ([#1325](https://github.com/tasks/tasks/issues/1325))

### 11.3.2 (2021-01-28)

* Fix recurrence sync issue
  ([#1323](https://github.com/tasks/tasks/issues/1323))

### 11.3.1 (2021-01-27)

* Improve support for recurring tasks with subtasks
  * Subtasks will be unchecked after completing a recurring task
  * Clear completed will not delete subtasks of recurring tasks
* Improve widget sort header when space is limited
* Add option to hide widget title
* Fix timezone conversions during synchronization
* Add Esperanto translations - @jakubfabijan

### 11.3 (2021-01-20)

* 'Hide until' is now 'Start date'
  * Synchronize start dates with Tasks.org, DAVx⁵, CalDAV, EteSync, and DecSync
  * New start date picker
  * New start date custom filter criteria
  * Add sort 'By start date'
  * Display start dates as chips
* Don't perform background sync when data saver enabled
* Preference changes
  * Add app and widget preferences to disable start date chips
  * Synchronization accounts displayed on main preference screen
  * Removed background sync and metered connection options (now respecting data
    saver mode)
  * Removed Google Tasks 'Custom order synchronization fix' (automatically
    performing full sync if 'My order' enabled)
* Bug fixes

### 11.2.2 (2021-01-07)

* Rename 'Lists' to 'Local lists' to clarify that they are not synchronized
* Tasks.org sign in improvements
* Miscellaneous improvements - Thanks @mhmdanas!

### 11.2.1 (2021-01-05)

* Fix Portuguese translation issue
* Report OpenTask sync errors
* Report Tasks.org sign in errors
* Don't crash on widget configuration error
* Purchase dialog changes

### 11.2 (2020-12-30)

* [Synchronize your Tasks.org account with third-party task and calendar apps, like Outlook,
  Thunderbird, or Apple Reminders](https://tasks.org/passwords)
* Miscellaneous improvements - Thanks @mhmdanas!

### 11.1.1 (2020-12-24)

* Fix compatibility issues with third-party clients
  * Completed tasks without completion dates
    ([222a34f](https://github.com/tasks/tasks/commit/222a34fc263816bb23f633bc9c79de78aeb3968d))
  * Tasks with start date but no due date
    ([7a1d566](https://github.com/tasks/tasks/commit/7a1d566bfb613b95d3fe1df46d8fa67200c91021))
* Miscellaneous improvements - Thanks @mhmdanas!

### 11.1 (2020-12-21)

* Add [DecSync CC synchronization](https://tasks.org/decsync)
* Fix rescheduling remotely completed recurring task
  ([5eb9370](https://github.com/tasks/tasks/commit/5eb9370294ef707b3e667c4a42851030419920d8))
* Miscellaneous code improvements - Thanks @mhmdanas!

### 11.0.1 (2020-12-17)

* Fix EteSync client issue with v2 accounts
  ([b761309](https://github.com/tasks/tasks/commit/b76130902ae0be6e1d580d588798a9ed0d7ff385))
* Fix multi-select 'Pick time' crash
* Fix default hide until due time
  ([#842](https://github.com/tasks/tasks/issues/842#issuecomment-746358382))
* Add Croatian translations - Garden Hose
* Add Urdu translations - Maaz

### 11.0 (2020-12-10)

* New Tasks.org synchronization service
* Multi-select rescheduling
* New task default settings
  * Default tags
  * Default recurrence
  * Default location
  * Hide until due time
* New custom filter criteria
  * Hidden tasks
  * Completed tasks
  * Subtasks
  * Parent tasks
  * Recurring tasks
* Added EteSync v2 support
* Deprecated EteSync v1 support
  * v1 accounts cannot be added to Tasks.org
  * v1 accounts can be added to the EteSync Android client
* Add ability to delete comments (Thanks to @romedius!)
* Add option to always display date (Thanks to @T0M0F!)
* Copy subtasks when copying tasks (Thanks to @supermzn!)
* Fix ring five times cutoff (Thanks to @przemhb!)
* Bug fixes
* Translation updates
  * Arabic - @mhmdanas
  * Basque - @osoitz, @ppasserini
  * Dutch - @fvbommel
  * French - @FlorianLeChat
  * German - @franconian, J. Lavoie, @myabc
  * Hebrew - @yarons
  * Hungarian - kaciokos
  * Indonesian - @andikatuluspangestu
  * Italian - @ppasserini, @Fs00, @pjammo
  * Korean - Sunjae Choi, @Hwaro-K
  * Norwegian Bokmål - @comradekingu
  * Polish - @alex-ter
  * Russian - Nikita Epifanov
  * Simplified Chinese - @sr093906
  * Spanish - @FlorianLeChat
  * Traditional Chinese - @realpineapplemilk
  * Turkish - @emintufan, Oğuz Ersen

### 10.4.1 (2020-11-09)

* Fix Mapbox Maps crash on Android 11 (F-Droid only)

### 10.4 (2020-10-09)

* New widget configuration options
  * Sort
  * Show hidden
  * Show completed
  * Header spacing
* Bug fixes

### 10.3 (2020-10-02)

* Collapsible sort groups in widget
* Add 'System default' widget theme
* Bug fixes

### 10.2 (2020-09-25)

* Display list, tag, and place chips on widgets
* Add option to disable list, tag, and place chips on widgets

### 10.1 (2020-09-23)

* Android 11 support
* Backup improvements
* Swipe-to-refresh initiates DAVx5/EteSync sync
* Show indicator when DAVx5/EteSync are synchronizing
* Bug fixes

### 10.0.3 (2020-09-16)

* Fix crash from calendar event snackbar
* Fix crash when setting Google Maps markers
* Fix invalid calendar entry creation

### 10.0.2 (2020-09-14)

* Fix crash from corrupted custom filter
* Fix crash in 'Astrid manual sorting' mode
* Fix missing 'Calendar event created' snackbar

### 10.0.1 (2020-09-05)

* Bug fixes
* Translation updates
  * Czech - @vitSkalicky
  * Danish - @ChMunk

### 10.0 (2020-08-31)

* PRO: DAVx⁵ support (requires [DAVx⁵ beta](https://tasks.org/davx5))
* PRO: EteSync client support
* [ToDo Agenda](https://play.google.com/store/apps/details?id=org.andstatus.todoagenda) integration
* Changed backstack behavior to follow Android conventions
* Major internal changes! Please report any bugs!
* Remove Mapbox tiles (Google Play only)
* Added 'Astrid manual sort' information to backup file
* Bug fixes
* Performance improvements
* Security improvements

[Older releases](https://github.com/tasks/tasks/blob/main/V06_09_CHANGELOG.md)