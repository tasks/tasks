### 14.7.3 (2025-06-13)

* Fix dynamic color
* Fix Microsoft To Do sync failure
* Fix crash after deleting last list
* Fix notifications when 'Alarms & reminders' not allowed
* Update translations
  * Bulgarian - 109247019824
  * Dutch - @fvbommel
  * Esperanto - Don Zouras
  * French - @FlorianLeChat
  * Hebrew - Xo
  * Japanese - M_Haruki
  * Persian - @theuser17
  * Portuguese - @nero-bratti
  * Romanian - @ygorigor
  * Russian - @yurtpage
  * Spanish - @orionn333
  * Swedish - @Nicklasfox
  * Turkish - @emintufan

### 14.7.2 (2025-05-23)

* Remove Microsoft Authentication Library from F-Droid builds [#3581](https://github.com/tasks/tasks/issues/3581)
* Remove contacts permission added by Microsoft Authentication Library
* Enable video attachments
* Fix wallpaper theme
* Fix handling multiple attachments
* Update translations
  * Arabic - abdelbasset jabrane
  * Bulgarian - 109247019824
  * Catalan - @Crashillo
  * Czech - @Fjuro
  * Danish - @catsnote
  * Dutch - @fvbommel
  * Esperanto - Don Zouras
  * Estonian - Priit Jõerüüt
  * Hungarian - Kaci
  * Italian - @ppasserini
  * Turkish - @emintufan
  * Ukrainian - @IhorHordiichuk

### 14.7.1 (2025-05-04)

* Fix app closing itself automatically [#3366](https://github.com/tasks/tasks/issues/3366)
* Automatically set default list when connecting Microsoft To Do account
* Update translations
  * Arabic - abdelbasset jabrane, @kemo-1
  * Brazilian Portuguese - Jose Delvani
  * Chinese (Simplified) - Sketch6580
  * French - @FlorianLeChat
  * German - @Kachelkaiser

### 14.7 (2025-05-03)

* Add support for Microsoft To Do work & school accounts [#3267](https://github.com/tasks/tasks/issues/3267)
* Add ability to rename or delete local account
* Prompt to sign in or import backup on first launch
* @BeaterGhalio: Fix back button closing app after search [#3426](https://github.com/tasks/tasks/issues/3426)
* @codokie: Automirrored icons fix [#3499](https://github.com/tasks/tasks/pull/3499)
* @codokie: Fix ltr-rtl alignment for text input [#3489](https://github.com/tasks/tasks/pull/3489)
* Use system language picker on Android 33+
* Don't show 'due date' as a start date option for DAVx5, EteSync, DecSync CC [#1558](https://github.com/tasks/tasks/issues/1558)
* Prevent attempts to delete or rename Microsoft To Do default list
* Don't handle system 'Clear storage' button
* Update minimum Android version to 8
* Fix backup import dropping tags [#3556](https://github.com/tasks/tasks/issues/3556)
* Fix start date chip when grouping by start date [#3509](https://github.com/tasks/tasks/issues/3509)
* Update translations
  * Brazilian Portuguese - @sobeitnow0, dedakir923
  * Czech - @Fjuro
  * Dutch - Jay Tromp
  * German - min7-i
  * Hebrew - Xo
  * Portuguese - @wm-pucrs
  * Russian - @hady-exc, Maksim_220 Кабанов
  * Slovak - @jose1711
  * Spanish - Nucl3arSnake, @diamondtipdr
  * Tamil - @TamilNeram

### 14.6.2 (2025-04-06)

* Show error indicators if 'When started' or 'When due' reminders are used
  without start or due times
* Fix delay when saving tasks
* Fix populating clock picker with initial value instead of 00:00
* Fix displaying selected calendar month
* Fix grouping by start date in descending order
* Update translations
  * Arabic - abdelbasset jabrane
  * Danish - @catsnote
  * Esperanto - Don Zouras
  * German - @Kachelkaiser
  * Hebrew - @elid34
  * Italian - @Fs00
  * Slovak - @jose1711
  * Turkish - @emintufan

### 14.6.1 (2025-03-30)

* Restore default sort mode for existing installs
* Fix grouping by due date descending
* Remove shadow from launcher icons

### 14.6 (2025-03-25)

* Add dynamic theme color - requires pro subscription
* Update translation
  * Brazilian Portuguese - dedakir923
  * Bulgarian - 109247019824
  * Chinese (Simplified) - Sketch6580
  * Estonian - Priit Jõerüüt
  * Italian - @ppasserini
  * Japanese - YuzuMikan
  * Swedish - @Ziron
  * Ukrainian - @IhorHordiichuk

### 14.5.4 (2025-03-24)

* Updated remaining date and time pickers to Material 3
  * App will remember if you change calendar or clock to text input
  * Text input now supported on start and due date pickers
  * Remove calendar and clock mode settings
* Open date picker to currently selected month
* Replaced upgrade pop-up with a banner [#1429](https://github.com/tasks/tasks/issues/1429)
* @hady-exc: Fix date picker time zone issues [#3248](https://github.com/tasks/tasks/pull/3248)
* Fix date time picker font scaling issues [#3437](https://github.com/tasks/tasks/issues/3437)
* Fix save task on keyboard done [#3288](https://github.com/tasks/tasks/issues/3288)
* Fix applying date time when dismissing date time pickers
* Fix 3 button navigation bar padding in landscape mode
* Fix out of memory errors in backup import/export
* Update translations
  * Brazilian Portuguese - dedakir923
  * Bulgarian - 109247019824
  * Chinese (Simplified) - Sketch6580
  * Dutch - @fvbommel
  * Estonian - Priit Jõerüüt
  * French - @FlorianLeChat
  * German - @franconian
  * Hungarian - Kaci
  * Italian - @ppasserini
  * Romanian - @ygorigor
  * Tamil - @TamilNeram
  * Turkish - @emintufan

### 14.5.3 (2025-03-20)

* Updated date and time pickers to Material 3
* Remove 'Start of week' preference
  * This feature can't be supported with Material 3 calendars

### 14.5.2 (2025-03-15)

* Fix items hidden under menu search bar [#3406](https://github.com/tasks/tasks/issues/3406)
* Attempt to fix layout on some foldables
* Fix checking for tasks.org account [#3397](https://github.com/tasks/tasks/issues/3397)
* Slightly reduce donation nagging frequency [#3397](https://github.com/tasks/tasks/issues/3397)
* Update translations
  * Danish - Øjvind Fritjof Arnfred
  * Hungarian - Kaci
  * Malayalam - Clouds Liberty
  * Russian - @GREAT-DNG
  * Swedish - @bittin
  * Tamil - @TamilNeram

### 14.5.1 (2025-03-11)

* Fix performance issue when opening search
* Fix Microsoft To Do authentication crash
* Fix crash on task list screen
* Update translation
  * Brazilian Portuguese - dedakir923
  * Bulgarian - 109247019824
  * Chinese (Simplified) - 大王叫我来巡山
  * Dutch - @fvbommel
  * Esperanto - Don Zouras
  * Estonian - Priit Jõerüüt
  * French - @FlorianLeChat
  * German - Colorful Rhino
  * Italian - @ppasserini
  * Kannada - Abilash S
  * Persian - @mamad-zahiri
  * Ukrainian - @IhorHordiichuk

### 14.5 (2025-03-04)

* Material 3 - work in progress
* Side navigation drawer
* Improve support for foldables
* Improve edge-to-edge support
* Remove options for top app bar and disabling collapsing app bar
  * Some features are being removed in order to make development easier for the
    upcoming desktop app. The features may return again in a future release.
* Save backup files and attachments to Nextcloud [#1289](https://github.com/tasks/tasks/issues/1289)
* Dismiss notification dialog when pressing cancel [#2116](https://github.com/tasks/tasks/issues/2116)
* Performance improvements
* Fix Microsoft To Do sync failure
* Fix missing list chips for subtasks in custom filters
* Fix for database timeouts
* Fix infinite subtask recursion
* Update translations
  * Belarusian - @fobo66
  * Estonian - Priit Jõerüüt
  * German - Colorful Rhino
  * Japanese - M_Haruki
  * Nahuatl - Benjamin Bruce
  * Slovak - @jose1711
  * Ukrainian - @IhorHordiichuk

### 14.4.8 (2025-02-04)

* Performance improvements
* Update translations
  * German - Colorful Rhino, @Kachelkaiser
  * Nepali - Sagun Khatri

### 14.4.7 (2025-02-01)

* Database improvements
* Update translations
  * Estonian - Priit Jõerüüt
  * German - @Kachelkaiser

### 14.4.6 (2025-01-29)

* Database performance improvements
* Additional debug logging
* Update translations
  * Danish - ERYpTION
  * Estonian - Priit Jõerüüt
  * German - @franconian, Colorful Rhino, @Kachelkaiser
  * Italian - @ppasserini
  * Korean - Sunjae Choi
  * Nepali - Sagun Khatri
  * Slovak - @jose1711
  * Swedish - Nick Wick

### 14.4.5 (2025-01-22)

* Performance improvements
* DAVx5 sync performance improvements
* Update translations
  * Bosnian - @hasak
  * Esperanto - Don Zouras
  * Estonian - Priit Jõerüüt, @dermezl
  * Italian - @ppasserini
  * Nepali - @sagunkhatri


### 14.4.4 (2025-01-19)

* Fix list pickers [#3269](https://github.com/tasks/tasks/issues/3269)

### 14.4.3 (2025-01-18)

* Preserve reminder recurrence when copying tasks
* Refresh task list after changing settings
* Fix missing chips for local lists
* Fix changes being lost when completing task from edit screen
* Update translations
  * German - @franconian
  * Turkish - @emintufan
  * Ukrainian - @IhorHordiichuk

### 14.4.2 (2025-01-16)

* Fix crash on missing account
* Update translations
  * Bulgarian - 109247019824
  * Chinese (Simplified) - Sketch6580
  * Croatian - @milotype
  * Dutch - @fvbommel
  * Esperanto - Don Zouras
  * French - @FlorianLeChat, @CennoxX
  * German - @franconian
  * Hungarian - Kaci
  * Italian - @ppasserini
  * Russian - @hady-exc
  * Slovak - @jose1711
  * Ukrainian - @IhorHordiichuk

### 14.4.1 (2025-01-11)

* Microsoft To Do support [#2011](https://github.com/tasks/tasks/issues/2011)
  * This feature is in early access, please report any bugs!
  * Enable under 'Advanced' settings
* Add configuration option for new lines in titles
* @TonSilver - Copy comments to clipboard with long press [#3212](https://github.com/tasks/tasks/pull/3212)
* @jheld - Attempt to fix F-Droid build with colorpicker fork [#2028](https://github.com/tasks/tasks/issues/2028)
* Subscription changes
  * Multiple Google Task accounts are now free to use
  * Tasker plugins are now free to use
* Fix crash on empty shortcut labels
* Fix missing settings button on Android 10 and below
* Update translations
  * Bulgarian - 109247019824
  * Chinese (Simplified) - 大王叫我来巡山, Sketch6580
  * Czech - @AtmosphericIgnition
  * Dutch - @fvbommel
  * Esperanto - Don Zouras
  * French - @FlorianLeChat, @lfavole
  * German - @franconian, Colorful Rhino
  * Hungarian - Kaci
  * Italian - @ppasserini
  * Slovak - @jose1711
  * Swedish - @Ziron, @bittin
  * Turkish - @emintufan

### 14.3.1 (2025-01-02)

* Fix edit screen disappearing on rotation
* Fix notification bundling issue
* Fix scrolling in custom filter settings
* Remove map theme and desaturation options
* Update translations
  * Bulgarian - @StoyanDimitrov
  * Chinese (Simplified) - 大王叫我来巡山
  * Dutch - @fvbommel
  * French - @FlorianLeChat
  * German - @p-rogalski
  * Italian - @ppasserini
  * Korean - Sunjae Choi
  * Swedish - @bittin

### 14.3 (2024-12-24)

* "Add widget to home screen" shortcut in list settings
* "Add shortcut to home screen" shortcut in list settings
  * Shortcuts use list icon and color
* Fix long running sync indicators [#3045](https://github.com/tasks/tasks/issues/3045)
* @hady-exc: Migrate list setting screens to Compose [#3163](https://github.com/tasks/tasks/pull/3163)
* Update translations
  * Bosnian - @hasak
  * Bulgarian - @StoyanDimitrov
  * Chinese (Simplified) - 大王叫我来巡山
  * Croatian - @milotype
  * Esperanto - Don Zouras
  * Finnish - @pHamala, @Ricky-Tigg
  * German - @p-rogalski, @franconian, @Atalanttore
  * Hungarian - Kaci
  * Italian - @ppasserini
  * Korean - Sunjae Choi
  * Spanish - gallegonovato
  * Swedish - Nick Wick

### 14.2.1 (2024-12-03)

* Fix save button when 'Back button saves task' is enabled [#3149](https://github.com/tasks/tasks/issues/3149)
* Fix customizing edit screen order screen

### 14.2 (2024-12-02)

* Updated edit screen task title
  * Show full title
  * Removed collapse on scroll
  * Removed floating action button
* Add separate alarms and reminders warning
* Capitalize tag picker text field
* Update translations
  * Bulgarian - @StoyanDimitrov
  * Catalan - raulmagdalena
  * Chinese (Simplified) - 大王叫我来巡山
  * Dutch - @fvbommel
  * French - @FlorianLeChat
  * Italian - @ppasserini
  * Spanish - gallegonovato
  * Ukrainian - @nathalier

### 14.1.1 (2024-11-26)

* Show warning when quiet hours are in effect
* Fix escape character in some localizations [#3046](https://github.com/tasks/tasks/issues/3046)
* Fix comment delete button color [#3102](https://github.com/tasks/tasks/issues/3102)
* Update translations
  * Bosnian - @hasak
  * Bulgarian - @StoyanDimitrov
  * Catalan - raulmagdalena
  * Chinese (Simplified) - 大王叫我来巡山
  * Croatian - @milotype
  * Dutch - @fvbommel
  * Esperanto - Don Zouras
  * French - @FlorianLeChat
  * Hungarian - Kaci
  * Italian - @ppasserini
  * Polish - @rom4nik
  * Spanish - gallegonovato
  * Swedish - Nick Wick

### 14.1 (2024-11-20)

* Add 'Help & Feedback > Send application logs'
* Delete snoozed reminders when completing tasks
* Fix duplicated tasks when using 'Share' [#2404](https://github.com/tasks/tasks/issues/2404)
* Don't show sync indicator on startup when sync is not used
* Update translations
  * Bosnian - @hasak
  * Brazilian Portuguese - kowih83264
  * Croatian - @milotype
  * German - min7-i

### 14.0.1 (2024-11-10)

* Fix widget crash
* Fix EteSync sync failure [#3092](https://github.com/tasks/tasks/issues/3092)
* Minor Wear OS improvements
* Update translations
  * Hungarian - Kaci
  * Italian - @ppasserini
  * Kannada - @historicattle
  * Marathi - @historicattle
  * Spanish - gallegonovato
  * Swedish - @bittin

### 14.0 (2024-11-05)

* Wear OS support (Google Play only)
* Move drawer items to top unless searching
* Fix drawer item layout issues
* Update translations
  * Brazilian Portuguese - Nicolas Suzuki, pogoyar888
  * Bulgarian - @StoyanDimitrov
  * Chinese (Simplified) - 大王叫我来巡山
  * Chinese (Traditional) - hugoalh
  * Dutch - Luna, @fvbommel
  * French - @FlorianLeChat
  * German - @p-rogalski, @franconian
  * Hungarian - Kaci
  * Italian - @ppasserini
  * Spanish - gallegonovato
  * Swedish - @bittin
  * Turkish - @oersen
  * Ukrainian - @IhorHordiichuk

### 13.11.2 (2024-09-29)

* Target Android 14
* Fix crash in location picker [#2990](https://github.com/tasks/tasks/issues/2990)
* Fix SQLite crash [#3045](https://github.com/tasks/tasks/issues/3045)
* Update translations
  * Arabic - @sanabel-al-firdaws
  * Belarusian - @katalim
  * Brazilian Portuguese - Jose Delvani
  * Catalan - raulmagdalena, @truita
  * Chinese (Traditional) - @abc0922001
  * Croatian - @milotype
  * Czech - atmosphericignition
  * Danish - Tntdruid, Luna
  * Dutch - @VIMVa
  * Esperanto - Don Zouras
  * Estonian - @dermezl
  * German - @Atalanttore, @tct123
  * Italian - @ppasserini
  * Norwegian Bokmål - @RonnyAL
  * Swedish - @JonatanWick, @bittin

### 13.11.1 (2024-07-15)

* Fix crash when collapsing list picker sections
* Fix crash in database migration
* Enabled Managed DAVx5
* Update translations
  * Bulgarian - @StoyanDimitrov

### 13.11 (2024-07-14)

* New icon picker with over 2,100 icons! (pro feature)
* Fix Todo Agenda Widget integration [todoagenda/#145](https://github.com/andstatus/todoagenda/issues/145)
* Fix menu search bar on Android 10 and below [#2966](https://github.com/tasks/tasks/issues/2966)
* Update translations
  * Brazilian Portuguese - Jose Delvani
  * Bulgarian - @StoyanDimitrov
  * Catalan - @Seveorr, @jtorrensamer
  * Chinese (Simplified) - 大王叫我来巡山
  * Chinese (Traditional) - hugoalh
  * French - @FlorianLeChat
  * Spanish - gallegonovato
  * Turkish - @oersen
  * Ukrainian - @IhorHordiichuk

### 13.10 (2024-07-05)

* Add search bar to drawer
* Add search bar to list picker
* Move 'Manage drawer' to ⚙️ > Navigation drawer
* Android 13+ users must grant additional reminder permissions
* Fix completing task multiple times from notification
* Fix deleting new subtasks from edit screen
* ~~Enable Managed DAVx5~~
* Update translations
  * Arabic - @islam2hamy
  * Brazilian Portuguese - Jose Delvani
  * Chinese (Simplified) - 大王叫我来巡山
  * Chinese (Traditional) - hugoalh
  * Croatian - @milotype
  * Finnish - Rami Lehtinen, @CSharpest
  * German - min7-i
  * Spanish - gallegonovato
  * Turkish - @oersen

### 13.9.9 (2024-05-30)

* Fix import backup crashes
* Fix showing completed subtasks in edit screen

### 13.9.7 (2024-05-23)

* Add default reminders when adding start/due dates to existing tasks [#1846](https://github.com/tasks/tasks/issues/1846)
* Fix import backup crash

### 13.9.6 (2024-05-18)

* Fix widget crash [#2873](https://github.com/tasks/tasks/issues/2873)
* Fix recurrence unable to finish [#2874](https://github.com/tasks/tasks/issues/2874)
* Fix edit screen being cleared when reopening app [#2857](https://github.com/tasks/tasks/issues/2857)
* Fix performance regressions
* Simplified internal alarm scheduling logic
* Update translations
  * Arabic - @islam2hamy
  * Bulgarian - @StoyanDimitrov

### 13.9 (2024-05-01)

* @elmuffo: Add swipe-to-snooze [#2839](https://github.com/tasks/tasks/pull/2839)
* @IlyaBizyaev: Add option to use quick tile without unlocking device [#2847](https://github.com/tasks/tasks/pull/2847)
* @liz-desartiges: Add support for Z Flip 5 cover screen [#2843](https://github.com/tasks/tasks/pull/2843)
* @purushyb: Fix drawer not updating after editing items [#2855](https://github.com/tasks/tasks/pull/2855)
* @hady-exc: Migrate tag picker screen to Compose [#2849](https://github.com/tasks/tasks/pull/2849)
* @yurtpage: Add Russian app store description [#2848](https://github.com/tasks/tasks/pull/2848)
* Fix duplicate notifications [#2835](https://github.com/tasks/tasks/issues/2835)
* Fix adding '(Completed)' to calendar entries [#2832](https://github.com/tasks/tasks/issues/2832)
* Fix hiding empty items from drawer [#2831](https://github.com/tasks/tasks/issues/2831)
* Exclude old snoozed tasks from snoozed task filter
* Update translations
  * Brazilian Portuguese - @mayhmemo, @gorgonun
  * Chinese (Simplified) - 大王叫我来巡山
  * Croatian - @milotype
  * Esperanto - Don Zouras
  * French - Lionel HANNEQUIN
  * German - sorifukobexomajepasiricupuva33, min7-i
  * Portuguese - @fparri, @laralem
  * Spanish - gallegonovato
  * Swedish - @JonatanWick
  * Turkish - @emintufan, @oersen

### 13.8.1 (2024-03-24)

* Fix copy causing duplicate Google Tasks
* Fix navigation drawer crash
* Fix backup import dropping tasks

### 13.8 (2024-03-22)

* Dynamic widget theme (name-your-price subscription required)
* Replace 'until' with 'ends on' for repeating tasks [#2797](https://github.com/tasks/tasks/pull/2797) - @akwala
* Fix loading selected list on startup [#2777](https://github.com/tasks/tasks/issues/2777)
* Fix repeating tasks ending one day early
* Fix repeating task crash
* Fix backup import crash
* Fix Astrid manual ordering crash in widget
* Update translations
  * Brazilian Portuguese - @mayhmemo
  * Bulgarian - @StoyanDimitrov
  * Catalan - @ferranpujolcamins
  * Chinese (Simplified) - 大王叫我来巡山
  * Croatian - @milotype
  * Czech - Odweta
  * German - @macpac59
  * Italian - @ppasserini
  * Spanish - gallegonovato
  * Swedish - @bittin
  * Ukrainian - @IhorHordiichuk
  * Vietnamese - @ngocanhtve

### 13.7 (2024-02-07)

* Fix returning to previous filter after search [#2700](https://github.com/tasks/tasks/pull/2700)
* Fix wearable notifications on Android 14+
* Fix issue causing repeating tasks to not repeat
* Fix dragging a task into a subtask in another list
* Rewrote navigation drawer in Jetpack Compose
* Internal changes to navigation
* Enable multi-select when adding attachments
* Show count of tasks to be deleted when clearing completed
* Include hidden subtasks when clearing completed [#2724](https://github.com/tasks/tasks/issues/2724)
* Don't show hidden or completed tasks in snoozed filter
* Remove markdown from repeating task snackbar
* Update translations
  * Azerbaijani - Shaban Mamedov
  * Bulgarian - @StoyanDimitrov
  * Catalan - raulmagdalena
  * Chinese (Simplified) - 大王叫我来巡山
  * Chinese (Traditional) - @abc0922001
  * Croatian - @milotype
  * Dutch - @mm4c
  * Esperanto - Don Zouras
  * Finnish - @millerii
  * French - J. Lavoie
  * German - @CennoxX
  * Hebrew - @elig0n
  * Interlingua - @softinterlingua
  * Odia - @SubhamJena
  * Persian - @Monirzadeh
  * Spanish - gallegonovato
  * Swedish - @bittin
  * Turkish - @oersen
  * Ukrainian - Сергій
  * Vietnamese - @ngocanhtve

### 13.6.3 (2023-11-25)

* Revert "Preserve modification times on initial sync" [#2460](https://github.com/tasks/tasks/issues/2640)
* Fix unnecessary DecSync work

### 13.6.2 (2023-10-30)

* Fix updating modification timestamp on edits

### 13.6.1 (2023-10-27)

* Push pending changes when app is backgrounded
* Don't require internet connection for DAVx5/EteSync/DecSync sync
* Don't perform background sync for DAVx5/EteSync/DecSync
  * Background sync is performed by the sync app
* Preserve modification times on initial sync [#2496](https://github.com/tasks/tasks/issues/2496)
* Replace deprecated method call [#2547](https://github.com/tasks/tasks/pull/2547) - @kmj-99
* Improve task list scrolling performance
* Fix hourly recurrence bug
* Update translations
  * Chinese (Simplified) - Eric
  * Croatian - @milotype
  * Czech - @ceskyDJ
  * Finnish - @millerii
  * French - Lionel HANNEQUIN, Bruno Duyé
  * Japanese - Kazushi Hayama
  * Portuguese - @loucurapt
  * Romanian - @ygorigor
  * Swedish - @bittin

### 13.6 (2023-10-07)

* Change priority with multi-select [#2257](https://github.com/tasks/tasks/pull/2452) - @vulewuxe86
* Automatically select newly copied tasks [#2246](https://github.com/tasks/tasks/pull/2446) - @vulewuxe86
* Reduce minimum size for widgets [#2436](https://github.com/tasks/tasks/pull/2436) - @histefanhere
* Replace deprecated method call [#2526](https://github.com/tasks/tasks/pull/2526) - @kmj-99
* Improve handling text shared to Tasks [#2485](https://github.com/tasks/tasks/issues/2485)
* Use notification audio stream for completion sound
* Notification preference 'More settings' opens channel settings directly
* Respect 'New tasks on top' preference when creating subtasks
* Automatically add due dates for recurring tasks
* Fix crash on startup
* Update translations
  * Brazilian Portuguese - @gorgonun
  * Bulgarian - @StoyanDimitrov, @salif
  * Catalan - Joan Montané
  * Chinese (Simplified) - Poesty Li
  * Chinese (Traditional) - @abc0922001
  * Dutch - @fvbommel
  * French - @FlorianLeChat
  * German - @qwerty287, deep map, @franconian
  * Hungarian - Kaci
  * Italian - @ppasserini
  * Japanese - Kazushi Hayama, Naga
  * Spanish - @FlorianLeChat
  * Swedish - @Anaemix, @bittin
  * Turkish - @emintufan, @oersen
  * Ukrainian - @IhorHordiichuk

### 13.5.1 (2023-08-02)

* Fix crash when importing Google Tasks from a backup file
* Added Burmese translations - @htetoh
* Update translations
  * Chinese (Simplified) - Poesty Li
  * Croatian - @milotype
  * Japanese - Kazushi Hayama
  * Polish - @alex-ter
  * Russian - @alex-ter
  * Ukrainian - @IhorHordiichuk
  * Vietnamese - @unbiaseduser

### 13.5 (2023-07-28)

* New custom recurrence picker
* Update translations
  * Bulgarian - @StoyanDimitrov
  * Czech - @ceskyDJ
  * Dutch - @fvbommel
  * French - @FlorianLeChat
  * Italian - @ppasserini
  * Spanish - @FlorianLeChat

### 13.4 - (2023-07-16)

* Sorting improvements
  * Add subtask sort configuration
  * Update sort menu button design
* Don't show subtasks of hidden tasks in 'My Tasks'
* Fix Google Tasks sync issue
* Update translations
  * Bulgarian - @StoyanDimitrov
  * Catalan - @and4po, Eudald Puy Polls
  * Croatian - @milotype
  * Dutch - @fvbommel
  * German - @schneidr
  * Hungarian - Kaci
  * Japanese - Naga
  * Korean - Sunjae Choi
  * Portuguese - @laralem
  * Swedish - @bittin

### 13.3.2 - (2023-06-02)

* Sorting improvements
  * Configure sort grouping
  * Configure sorting within sort group
  * Configure completed task sorting
* Fix Google Task list chips showing on widget
* Update translations
  * Bulgarian - @StoyanDimitrov
  * Catalan - @and4po
  * Chinese (Simplified) - Poesty Li
  * Croatian - @milotype
  * Dutch - @fvbommel
  * French - @FlorianLeChat
  * German - @qwerty287, @franconian
  * Hungarian - Kaci
  * Italian - @ppasserini
  * Spanish - @FlorianLeChat
  * Ukrainian - @IhorHordiichuk

### 13.2.4 - (2023-05-24)
* Add 'By list' sort mode [#1265](https://github.com/tasks/tasks/issues/1265)
* Save task when pressing done [#2125](https://github.com/tasks/tasks/pull/2125)
* Use ISO 8601 date formatting for backup filenames [#1550](https://github.com/tasks/tasks/pull/1550)
* Fix filter sorting bug [#1561](https://github.com/tasks/tasks/issues/1561)
* Fix manual sorting crash [#2141](https://github.com/tasks/tasks/issues/2141)
* Fix manual sorting bug [#2101](https://github.com/tasks/tasks/issues/2101)
* Fix multiple accounts on same server [#2301](https://github.com/tasks/tasks/issues/2301)
* Don't set `COUNT=0` on recurrence rules [#2158](https://github.com/tasks/tasks/issues/2158)
* Improve task list performance [#2062](https://github.com/tasks/tasks/issues/2062)
* Attempt to hide inactive widgets in settings [#2145](https://github.com/tasks/tasks/issues/2145)
* Disable persistent reminders on Android 14+
  * Android 14+ no longer supports persistent reminders 😢
* Fix notifications on Android 14
* Fix crash when missing exact alarm permissions
* Update logic for adding default reminders during sync
  * Don't add reminders on initial sync
  * Don't add reminders if other client supports reminder sync
* Internal database changes
  * You will need to reconfigure any widgets that were set to display a Google
    Task list or filter. Sorry for the interruption!
* Add Odia translations - @SubhamJena
* Update translations
  * Brazilian Portuguese - @lnux-usr
  * Bulgarian - @StoyanDimitrov
  * Catalan - @and4po
  * Chinese (Simplified) - Poesty Li
  * Chinese (Traditional) - Chih-Hsuan Yen
  * Croatian - @milotype
  * Dutch - @fvbommel
  * Esperanto - Don Zouras
  * Finnish - @millerii
  * French - @FlorianLeChat
  * Italian - @ppasserini
  * Japanese - @kisaragi-hiu, Naga
  * Korean - Sunjae Choi, @o20n3
  * Romanian - @simonaiacob
  * Russian - @AHOHNMYC
  * Spanish - @FlorianLeChat
  * Turkish - @ersen0
  * Ukrainian - @IhorHordiichuk

### 13.1.2 (2023-02-02)

* Add default reminders to incoming iCalendar tasks [#1984](https://github.com/tasks/tasks/issues/1984)
* Sync when brought to the foreground [#2096](https://github.com/tasks/tasks/issues/2096)
* Update translations
  * Arabic - haidarah esmander
  * Czech - @SlavekB
  * Danish - Tntdruid
  * Esperanto - Don Zouras, @J053Fabi0
  * Finnish - @millerii
  * German - @franconian
  * Italian - @ppasserini
  * Japanese - Kazushi Hayama
  * Korean - @o20n3
  * Polish - @gnu-ewm
  * Vietnamese - @unbiaseduser

### 13.1.1 (2022-12-06)

* Fix crash when opening notification settings
* Fix IAP errors in some locales
* Update translations
  * Italian - @ppasserini
  * Japanese - Kazushi Hayama

### 13.1.0 (2022-11-30)

* Support for DAVx5 and CalDAV read-only lists [#931](https://github.com/tasks/tasks/issues/931)
* Use default Android network security configuration
* Update translations
  * Bulgarian - @StoyanDimitrov
  * Chinese (Simplified) - Eric
  * Croatian - @milotype
  * Dutch - @fvbommel
  * Finnish - @millerii
  * French - @FlorianLeChat
  * German - @helloworldtest123
  * Hungarian - Kaci
  * Italian - @ppasserini
  * Lithuanian - @70h
  * Russian - Nikita Epifanov
  * Spanish - @FlorianLeChat
  * Turkish - @ersen0
  * Ukrainian - @IhorHordiichuk

### 13.0.2 (2022-11-22)

* Fix persistent notifications on Android 13
* Fix Samsung crash on too many reminders (DAVx5, EteSync, DecSync CC)
* Fix crash on too many tasks for Astrid Manual Sorting
* Fix RTL text in task edit customization screen
* Fix priority button order

### 13.0.1 (2022-10-20)

* 🚨 Major internal changes to task edit screen. Please report any bugs! 🚨
* Show thumbnails for attachments
* Tap on existing alarms to replace them
* Add task info row to edit screen [#1839](https://github.com/tasks/tasks/pull/1839)
* Add option to disable reminders for all-day tasks [#2003](https://github.com/tasks/tasks/pull/2003)
* Updated chip style
* Show geofence circle in place settings
* Fix removing preferences [#1981](https://github.com/tasks/tasks/pull/1981)
* Set user-agent on HTTP requests [#1978](https://github.com/tasks/tasks/issues/1978)
* Preserve HTTP session cookies [#1978](https://github.com/tasks/tasks/issues/1978)
* Sort selected tags at top of tag picker
* Android 13 support
  * Runtime notification permissions
  * Language preference
* Improvements to copying tasks
  * Don't forget parent when copying tasks [#1964](https://github.com/tasks/tasks/pull/1964)
  * Copy attachments when duplicating tasks [#812](https://github.com/tasks/tasks/issues/812)
  * Fix duplicating subtasks
* Fix some missing reminders
  * Incoming Google Tasks
  * Tasker tasks [#1937](https://github.com/tasks/tasks/issues/1937)
  * New subtasks [#1914](https://github.com/tasks/tasks/issues/1914)
* Fix Google Task creation time
* Fix EteSync stops synchronizing [#1893](https://github.com/tasks/tasks/issues/1893)
* Don't overwrite coordinates when synchronizing locations [#1667](https://github.com/tasks/tasks/issues/1667)
* Update translations
  * Asturian - @enolp
  * Basque - Sergio Varela
  * Bulgarian - @StoyanDimitrov
  * Chinese (Simplified) - Eric
  * Croatian - @milotype
  * Czech - Shimon
  * Dutch - @fvbommel
  * French - @FlorianLeChat, J. Lavoie
  * German - @qwerty287
  * Italian - @ppasserini
  * Norwegian Bokmål - @comradekingu
  * Persian - @latelateprogrammer
  * Polish - @ebogucka
  * Portuguese - @laralem
  * Romanian - @simonaiacob
  * Russian - @Allineer, Nikita Epifanov
  * Sinhala - @Dilshan-H
  * Spanish - @FlorianLeChat
  * Turkish - @ersen0
  * Ukrainian - @IhorHordiichuk, @artemmolotov
  * Vietnamese - @unbiaseduser

[Older releases](https://github.com/tasks/tasks/blob/main/V10_12_CHANGELOG.md)
