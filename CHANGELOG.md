Change Log
---

### 6.1.1 (2018-07-20)

* Fix notification badge issues
* Allow non-SSL connections
* Allow user-defined certificate authorities
* Update Chinese translations (Atlantids)

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
* Update translations
  * Brazilian Portuguese - douglaslmr
  * Bulgarian - ddmdima96
  * Dutch - revdbrink
  * French - Florian_Dubois, MystEre84
  * German - @marmo, sNiXx, Strubbl
  * Hungarian - kaciokos
  * Italian - dfdario
  * Korean - timeforwarp
  * Lithuanian - gacuxz
  * Polish - hadogenes
  * Russian - @uryevich
  * Spanish - i2nm7s
  * Turkish - etc

### 6.0.6 (2018-04-28)

* Fix crash when creating shortcuts on pre-Oreo devices
* Fix crash when Google Task or CalDAV list is missing
* Downgrade Play Services for compatibility with MicroG
* Update translations
  * German - marmo

### 6.0.5 (2018-04-26)

* Fix crash when deleting 1000+ tasks at once
* Fix hidden dates in date picker
* Fix crash on bad response from billing client
* Report crash when database fails to open
* Update translations
  * Dutch - revdbrink
  * German - marmo

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

* Change to [annual subscription](http://tasks.org/subscribe) pricing
* [CalDAV synchronization](http://tasks.org/caldav)
* Sync with [multiple Google Task accounts](http://tasks.org/docs/google_tasks_intro.html)
* Default theme changed to blue
* Display Google Task and CalDAV chips on task list
* Display sync error icon in navigation drawer
* Move tasks between Google Task and CalDAV lists using multi-select
* Add "Don't Sync" option when choosing a Google Task or CalDAV list
* Add option to restrict background synchronization to unmetered connections
* Custom filters with due date criteria no longer set a due time of 23:59/11:59PM
* Internal improvements to notification scheduling should reduce notification delays
* Fix list animation bug
* Update translations
  * Bulgarian - ddmdima96
  * Dutch - revdbrink
  * French - Florian_Dubois
  * German - jens_neuss, Marmo
  * Hungarian - kaciokos
  * Italian - dfdario, Tsanten
  * Japanese - naofumi
  * Lithuanian - gacuxz
  * Polish - bilbolodz, porridge
  * Russian - gacuxz, uryevich
  * Slovak - Cuco
  * Spanish - i2nm7s
  * Turkish - etc
