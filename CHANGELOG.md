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
