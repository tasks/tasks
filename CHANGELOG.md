### 11.0 (2020-12-03)

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
* Update translations
  * French - @FlorianLeChat, @yannicka
  * Italian - @ppasserini
  * Polish - @alex-ter

### 10.3 (2020-10-02)

* Collapsible sort groups in widget
* Add 'System default' widget theme
* Bug fixes
* Update translations
  * Hebrew - @yarons
  * Italian - @ppasserini
  * Norwegian Bokmål - @comradekingu
  * Portuguese - @SantosSi
  * Turkish - @emintufan

### 10.2 (2020-09-25)

* Display list, tag, and place chips on widgets
* Add option to disable list, tag, and place chips on widgets

### 10.1 (2020-09-23)

* Android 11 support
* Backup improvements
* Swipe-to-refresh initiates DAVx5/EteSync sync
* Show indicator when DAVx5/EteSync are synchronizing
* Bug fixes
* Update translations
  * Arabic - @PrestoSole, @mhmdanas
  * Armenian - @aabgaryan
  * Czech - @nijel
  * Dutch - @fvbommel
  * French - @FlorianLeChat
  * German - J. Lavoie
  * Hebrew - @yarons, @omeritzics
  * Hungarian - kaciokos
  * Italian - @IvanDan
  * Kannada - @skomshe
  * Malayalam - @Vachan
  * Norwegian Bokmål - @comradekingu
  * Russian - @NikGreens
  * Simplified Chinese - @sr093906
  * Spanish - @FlorianLeChat, @marmonto
  * Turkish - @emintufan, Oğuz Ersen

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
* Update translations
  * Basque - @osoitz
  * Bengali - @Oymate
  * Brazilian Portuguese - Pedro Lucas Porcellis, @aevw
  * Chinese - WH Julie
  * Chuvash - İlle
  * Czech - @vitSkalicky, Radek Řehořek
  * Dutch - @fvbommel
  * Finnish - J. Lavoie
  * French - @FlorianLeChat, J. Lavoie, @sephrat
  * German - @franconian, @joshxtra
  * Hebrew - @yarons, @avipars
  * Hungarian - kaciokos
  * Interlingua - @softinterlingua
  * Italian - @ppasserini, J. Lavoie
  * Norwegian Bokmål - @comradekingu, Erlend Ydse
  * Polish - @alex-ter
  * Portuguese - @SantosSi
  * Russian - Nikita Epifanov
  * Simplified Chinese - @sr093906, @cccClyde
  * Spanish - @FlorianLeChat
  * Swedish - @bittin
  * Tamil - @balogic, @Thiya-velu
  * Turkish - @emintufan, Oğuz Ersen

### 9.7.3 (2020-07-07)

* Fix Google Task bugs

### 9.7.2 (2020-06-22)

* Downgrade Mapbox SDK to remove non-free library (F-Droid only)

### 9.7.1 (2020-06-19)

* Fix crash on backup import
* Fix CalDAV/EteSync subtask move bug

### 9.7 (2020-06-12)

* Added '☰ > Manage lists'
  * Drag and drop to rearrange the drawer
  * Tap to edit or delete a list
* Display 2 additional snooze options - @rangzen

### 9.6 (2020-06-06)

* Add support for offline lists. Offline lists support manual ordering and infinite-depth subtasks
* Rename 'My order' to 'Astrid manual sorting' for 'My Tasks', 'Today', and tags
* Add '⚙ > Look and feel > Disable sort groups'
* Add '⚙ > Look and feel > Open last viewed list'
* Add '⚙ > Look and feel > Chips' toggles for subtasks, places, lists, and tags
* Add '⚙ > Navigation drawer > Lists'
* Add '⚙ > Task defaults > Default list'
* Add '⚙ > Task defaults > New tasks on top'
* Add '⚙ > Advanced > Astrid manual sorting'
* Fix preference reset button

### 9.5 (2020-06-03)

* Drag and drop to change subtasks in all list types
* Drag and drop to reprioritize or reschedule tasks while sorting by due date
  or priority
* Bug fixes

### 9.4.1 (2020-06-01)

* Add 'Tasks settings > Advanced > Improve performance' toggle
* Bug fixes

### 9.4 (2020-05-27)

* Add collapsible group headers when sorting by due date, priority, created, or modified
* Update translations
  * Basque - @osoitz
  * Bengali - @Oymate
  * French - @FlorianLeChat
  * German - @franconian
  * Norwegian Bokmål - @comradekingu
  * Russian - @Eninspace
  * Spanish - @FlorianLeChat

### 9.3.1 (2020-05-26)

* Fix offline subtasks

### 9.3 (2020-05-22)

* Add manual sorting support for CalDAV and EteSync

### 9.2 (2020-05-13)

* 'New task' quick settings tile (Android 7+)
* Search results match place names and addresses, caldav list names, google task list names, and comments
* Fix duplicated search results
* Began migrating codebase to Kotlin

### 9.1 (2020-05-04)

* 'New task' launcher shortcut (Android 7.1+)
* Add option to disable subtask chip on widget

### 9.0 (2020-05-03)

* Show What's New after update
* Collapsible subtasks enabled by default
* 20 new icons
* Show subtask chip even if list chips are disabled
* Indent subtasks in 'Share' output
* Don't trigger location reminders for snoozed or hidden tasks
* Minimum supported version is now Android 6.0

### 8.11 (2020-04-27)

* Edit existing custom filters
* Drag and drop to rearrange filter criteria
* Swipe to delete filter criteria
* Tap on filter criteria to choose filter operator
* Offer additional built-in filters
* Add sort by creation time
* Choose any day as start of week

### 8.10 (2020-04-20)

* New widget features
  * Menu button to quickly change list
  * Expand and collapse subtasks
  * Click on due date to reschedule
  * Access widget settings from main app preferences
  * Show description
  * Show hidden task indicators
* New widget settings
  * Row spacing: default, compact, none
  * Due date: after title, below title, or hidden
  * Configure header, row, and footer opacity
  * Configure footer click behavior
  * Show full task title
  * Show full description
  * Hide dividers
* Improve widget touch targets
* Expand/collapse Google Task subtasks in 'My order' mode
* Fix bug when changing sort order to/from 'My order'
* Fix crash when switching to 'My order' list with subtasks disabled

### 8.9.2 (2020-04-10)

* Fix 'Add reminder' layout issues
* Fix move between EteSync lists
* Accept date time changes when dismissing dialog
* Improve date time picker behavior in landscape mode

### 8.9.1 (2020-04-08)

* Add option to always hide check button
* Hide check button for new tasks
* Rearrange multi-select buttons
* Allow more space for time buttons in date time picker
* Fix priority button layout on smaller devices
* Fix clicking on hidden task titles
* Fix tag picker checkbox tint on Android 4.4
* Fix EteSync crash on malformed iCalendar data

### 8.9 (2020-04-06)

* Add 'Select all' option to multi-select menu
* Add 'Share' to menu and multi-select menu
* Display 'Calendar event created' snackbar after creating a calendar event

### 8.8 (2020-04-01)

* New bottom sheet due date picker
  * Shortcuts and calendar displayed together (Android 6+)
* Click on due date in task list to reschedule
* Option to autoclose due date picker after selecting a date or time
* Redesigned title in edit screen
* 'Discard' in overflow menu when 'Back button saves task' enabled
* Add preference for linkifying edit screen
* Updated date and time formatting
* Minimum supported version is now Android 4.4
* Custom backup/attachment directory requires Android 5+

### 8.7.1 (2020-03-31)

* Fix multi-account Google Task synchronization

### 8.7 (2020-03-19)

* Places are now lists
  * Rename a place
  * Assign an icon and color to a place
* Add new navigation drawer settings
  * Option to remove filters, tags, and places from drawer
  * Option to hide unused tags and places in drawer

### 8.6.1 (2020-03-19)

* Fix crash on startup

### 8.6 (2020-03-17)

* Expand and collapse navigation drawer groups

### 8.5 (2020-03-13)

* Synchronize locations with CalDAV and EteSync
* Fix crash when clearing completed from recently modified filter

### 8.4 (2020-03-11)

* New chip configuration options
  * Outlined or filled
  * Text and icon, text only, or icon only
* Add option to disable color desaturation
* Fix EteSync shared lists
* Google Task sync requires Android 4.4+

### 8.3 (2020-03-08)

* Synchronize CalDAV and EteSync colors
* Rename CalDAV and EteSync lists
* Update Turkish translations - @emintufan

### 8.2.1 (2020-03-07)

* Increase default chip text contrast
* New purchase activity
* Fix dividers on Android 4.x

### 8.2 (2020-03-04)

* Choose your own app and widget colors with a color wheel
* Dark theme now free for all
* New 'System default' theme
* New outlined chip style
* Dark theme is now darker
* Light theme is now lighter
* Desaturate theme colors in dark mode
* Improve dialog theming consistency
* Bug fixes

### 8.1 (2020-02-21)

* Updated app settings screen

### 8.0.1 (2020-02-16)

* Fix missing sync settings on fdroid

### 8.0 (2020-02-12)

* EteSync support

### 7.8 (2020-01-24)

* Android AutoBackup integration

### 7.7 (2020-01-21)

* Add support for offline multi-level subtasks
* Update Simplified Chinese translations - @sr093906

### 7.6.1 (2020-01-17)

* Fix long press in Google Task and CalDAV lists
* Fix bug when moving multi-level CalDAV subtasks
* Preserve remote VTODO when moving CalDAV tasks
* Add Interlingua translations - @softinterlingua

### 7.6 (2020-01-10)

* Change tags with multi-select
* Fix custom filter crash on deleted tag

### 7.5 (2020-01-07)

* New tag picker
* Support self-signed SSL certificates

### 7.4.2 (2019-12-30)

* Fix Tasker plugin settings

### 7.4.1 (2019-12-27)

* Add option to enable subtasks in task list
* Performance improvements
* Ask Play Services to update security provider
* Display custom icons in tag picker
* Fix case comparison when sorting navigation drawer

### 7.4 (2019-12-16)

* Add Google Task and CalDAV subtasks from the edit screen
* 'Recently modified' shows all modifications in past 24 hours
* Fix duplicated multi-level subtask count
* Increase checkbox touch target
* Naturally order lists and filters

### 7.3.2 (2019-12-12)

* Fix slow query for subtasks
* Fix setting icon on new CalDAV list
* Fix clear completed for subtasks
* Fix crash when clearing 1000+ tasks

### 7.3.1 (2019-12-05)

* Fix crash on missing filter

### 7.3 (2019-12-03)

* Expand and collapse subtasks

### 7.2.2 (2019-12-03)

* Fix Google Task sorting
* Fix crash when deleting 500+ tasks

### 7.2.1 (2019-11-27)

* Bug fixes and minor improvements

### 7.2 (2019-11-25)

* Display Google Task and CalDAV subtasks in all lists (Android 5+)
* Remove completed tasks immediately - @creywood

### 7.1.2 (2019-11-22)

* Add CalDAV account setting for repeating tasks
* Fix CalDAV repeating tasks
* Fix Google Tasks HTTP 400 response

### 7.1.1 (2019-11-18)

* Improve subtask query performance
* Fix crash when deleting 1000+ CalDAV tasks

### 7.1 (2019-11-14)

* Display subtasks on Google Task and CalDAV widgets (Android 5+)
* Fix subtasks after backup import
* Fix chained subtask completion

### 7.0 (2019-11-12)

* Add support for CalDAV subtasks (Android 5+) - @creywood
* Display Google subtasks in all sort modes (Android 5+)

### 6.9.3 (2019-10-31)

* Fix disappearance of remotely completed recurring Google Tasks
* Fix '0 tasks' notification
* Limit to 20 active notifications due to change in Android 10

### 6.9.2 (2019-10-25)

* Fix bug forcing new Google Tasks to top
* Fix bug preventing deleted tasks from being synchronized - @creywood

### 6.9.1 (2019-10-09)

* Fix location reminders on Android 10
* Fix CalDAV time zone issue

### 6.9 (2019-09-23)

* Synchronize tags with CalDAV
* Target Android 10
* Bug fixes

### 6.8.1 (2019-08-05)

* Fix CalDAV filter migration
* Fix native date picker crash

### 6.8 (2019-07-30)

* Name your own subscription price! Upgrade, downgrade, or cancel at any time
* Choose icons for lists (requires [subscription](https://tasks.org/subscribe))
* Choose color for custom filters
* Performance improvements
* Allow duplicate CalDAV list names
* Fix duplicate tag name bug

### 6.7.3 (2019-07-16)

* Workaround for [list updated time bug](https://issuetracker.google.com/issues/136123247) in Google Tasks API
* Fix crash in CalDAV sync

### 6.7.2 (2019-07-08)

* Handle 404 errors when creating new Google Tasks
* Ignore 404 errors when deleting Google Drive files
* Don't report connection errors

### 6.7.1 (2019-07-05)

* Add location chip to task list
* Reduce chip sizes
* Accept 'send to' for more attachment types
* Synchronize multiple accounts in parallel
* Fix Google Task migration from older versions
* Fix corrupted checkbox issue
* Fix some RTL issues

### 6.7 (2019-06-13)

* Use drag and drop to indent tasks
* Add new Google Tasks to top or bottom
* Toggle hidden and completed in manually sorted Google Task lists
* Rearrange Google Tasks without a network connection
* Optional workaround for [custom order bug](https://issuetracker.google.com/issues/132432317) in Google Tasks API
* Include subtasks when moving or deleting Google Tasks
* Ignore 404 errors when fetching Google Drive folders
* Match tags in search results
* Fix stuck 'Generating notifications' notification
* Don't display sync indicator when there is no network connection
* Don't synchronize immediately after every change
* Added Estonian translations - Eraser

### 6.6.4 (2019-05-21)

* Handle [breaking change](https://issuetracker.google.com/issues/133254108) in Google Tasks API

### 6.6.3 (2019-05-08)

* Fix backup import crash
* Fix crash when refreshing purchases
* Google Tasks synchronization bug fix

### 6.6.2 (2019-04-22)

* Backup and restore preferences
* Google Task performance improvements
* Google Task and Drive support added to F-Droid and Amazon
* Add third-party licenses, changelog, and version info
* Fix backup import crash
* Fix widget bugs

### 6.6.1 (2019-04-15)

* Fix crash on devices running Android 5.1 and below
* Fix analytics opt-out

### 6.6 (2019-04-10)

* New location picker
  * Choose Mapbox or Google Maps tiles
  * Choose Mapbox or Google Places search
  * Google Places search restricted to subscribers due to new Google Maps pricing
  * Use Mapbox for reverse geocoding
  * Select from previously used locations
  * Dark maps
* Enable location picker in F-Droid build
* Resume support for Amazon App Store
* Fix Android Q background warning

### 6.5.6 (2019-03-27)

* Fix crash when clearing completed on a manually sorted Google Task list
* Update Ukrainian translations - nathalier

### 6.5.5 (2019-03-14)

* Bug fixes

### 6.5.4 (2019-03-11)

* Fix black screen issue
* Fix crash when task not found

### 6.5.3 (2019-02-19)

* Fix crash when upgrading from Android 7 to 8+
* Improve OneTask interoperability
* Performance improvement

### 6.5.2 (2019-02-11)

* Bug fixes

### 6.5.1 (2019-02-10)

* Bug fixes

### 6.5 (2019-02-08)

* Improve notification accuracy
* Performance improvements
* Bug fixes
* Add Tagalog translations - Topol

### 6.4.1 (2019-01-16)

* Limit number of active notifications
* Limit rate of notifications
* Fix Synology Calendar sync issue
* Fix exception when external storage is unavailable

### 6.4 (2019-01-10)

* Copy backups to Google Drive
* Improved search
* Use system file picker (Android 4.4+)
* Use system directory picker (Android 5.0+)
* Accept 'send' and 'send_multiple' actions with images
* File attachment bug fixes

### 6.3.1 (2018-11-07)

* New location row in task edit screen
* Add location departure notifications
* Set CalDAV completion percentage and status
* Bug fixes

### 6.2 (2018-10-29)

* New white theme color
* New icons
* New list and tag chips
* Linkify text when editing tasks
* Option to linkify text on task list
* Show description on task list
* Move due date next to title
* Updated hidden task visualization
* No longer require contacts permission (Oreo+)
* Dropped support for Android 4.0

### 6.1.3 (2018-10-22)

* Fix translation error

### 6.1.2 (2018-10-18)

* Remove missed call functionality due to Google Play Developer policy change
* Fix manual sort issue affecting Samsung Oreo devices
* Fix refresh issue affecting Pure Calendar Widget
* Fix memory leak
* Schedule jobs with WorkManager instead of android-job

### 6.1.1 (2018-07-20)

* Fix notification badge issues
* Allow non-SSL connections
* Allow user-defined certificate authorities

### 6.1 (2018-06-30)

* Customize launcher icon
* Customize shortcut widget icon and label
* Add custom text selection action (Android 6+)
* Target Android P
* Remove 'Tasks' from notification body
* Fix localization issues - @marmo
* Fix crash when calendar permissions are revoked
* Fix crash when opening task from widget
* Fix crash when recording audio note
* Fix crash when dismissing dialogs
* Fix crash in backup import
* Fix crash on invalid URL during CalDAV setup
* Fix crash when editing task

### 6.0.6 (2018-04-28)

* Fix crash when creating shortcuts on pre-Oreo devices
* Fix crash when Google Task or CalDAV list is missing
* Downgrade Play Services for compatibility with MicroG

### 6.0.5 (2018-04-26)

* Fix crash when deleting 1000+ tasks at once
* Fix hidden dates in date picker
* Fix crash on bad response from billing client
* Report crash when database fails to open

### 6.0.4 (2018-04-25)

* Fix crash caused by leftover Google Analytics campaign tracker

### 6.0.3 (2018-04-25)

* Fix crash when manually sorting Google Task lists
* Fix multi account Google Task sync issue

### 6.0.2 (2018-04-25)

* Fix crash caused by missing tag metadata
* Fix crash caused by missing Android System WebView
* Replace Google Analytics with Firebase Analytics
* Add Crashlytics exception reporting

### 6.0.1 (2018-04-23)

* Fix crash caused by missing Google Task metadata

### 6.0 (2018-04-23)

* Change to [annual subscription](https://tasks.org/subscribe) pricing
* [CalDAV synchronization](https://tasks.org/caldav)
* Sync with [multiple Google Task accounts](https://tasks.org/docs/google_tasks_intro.html)
* Default theme changed to blue
* Display Google Task and CalDAV chips on task list
* Display sync error icon in navigation drawer
* Move tasks between Google Task and CalDAV lists using multi-select
* Add "Don't Sync" option when choosing a Google Task or CalDAV list
* Add option to restrict background synchronization to unmetered connections
* Custom filters with due date criteria no longer set a due time of 23:59/11:59PM
* Internal improvements to notification scheduling should reduce notification delays
* Fix list animation bug
