Change Log
---
### 6.7 (unreleased)

* Update translations
  * Bulgarian - ddmdima96
  * Dutch - revdbrink
  * French - Florian_Dubois
  * German - Strubbl
  * Hebrew - elazar
  * Hungarian - kaciokos
  * Japanese - naofumi
  * Lithuanian - gacuxz
  * Spanish - i2nm7s
  * Turkish - etc
  * Ukrainian - nathalier

### 6.6.4 (2019-05-21)

* Handle [breaking change](https://issuetracker.google.com/issues/133254108) in Google Tasks API

### 6.6.3 (2019-05-08)

* Fix backup import crash
* Fix crash when refreshing purchases
* Google Tasks synchronization bug fix
* Update translations
  * Polish - mujehu
  * Russian - Balbatoon
  * Slovak - Cuco

### 6.6.2 (2019-04-22)

* Backup and restore preferences
* Google Task performance improvements
* Google Task and Drive support added to F-Droid and Amazon
* Add third-party licenses, changelog, and version info
* Fix backup import crash
* Fix widget bugs
* Update translations
  * Dutch - revdbrink
  * German - marmo
  * French - Florian_Dubois, mathieufoucher
  * Hebrew - elazar
  * Hungarian - kaciokos
  * Japanese - naofumi
  * Lithuanian - gacuxz
  * Spanish - i2nm7s
  * Turkish - etc
  * Ukrainian - nathalier

### 6.6.1 (2019-04-15)

* Fix crash on devices running Android 5.1 and below
* Fix analytics opt-out
* Update translations
  * Italian - dfdario
  * Japanese - naofumi
  * Turkish - etc

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
* Update translations
  * Brazilian Portuguese - douglaslmr
  * Dutch - revdbrink
  * German - marmo
  * Hebrew - elazar
  * French - Florian_Dubois
  * Hungarian - kaciokos
  * Italian - dfdario
  * Japanese - naofumi
  * Lithuanian - gacuxz
  * Spanish - i2nm7s, Pum
  * Ukrainian - nathalier

### 6.5.6 (2019-03-27)

* Fix crash when clearing completed on a manually sorted Google Task list
* Update Ukrainian translations - nathalier

### 6.5.5 (2019-03-14)

* Bug fixes

### 6.5.4 (2019-03-11)

* Fix black screen issue
* Fix crash when task not found
* Update translations
  * Brazilian Portuguese - douglaslmr
  * Dutch - revdbrink
  * French - Florian_Dubois
  * German - @MPK44
  * Hebrew - elazar
  * Hungarian - kaciokos
  * Italian - passero
  * Japanese - naofumi
  * Lithuanian - gacuxz
  * Spanish - i2nm7s
  * Turkish - etc

### 6.5.3 (2019-02-19)

* Fix crash when upgrading from Android 7 to 8+
* Improve OneTask interoperability
* Performance improvement
* Update Russian translations - x32

### 6.5.2 (2019-02-11)

* Bug fixes
* Update translations
  * Bulgarian - ddmdima96
  * Dutch - revdbrink

### 6.5.1 (2019-02-10)

* Bug fixes
* Update German translations - Strubbl

### 6.5 (2019-02-08)

* Improve notification accuracy
* Performance improvements
* Bug fixes
* Add Tagalog translations - Topol
* Update translations
  * Bulgarian - ddmdima96
  * Brazilian Portuguese - douglaslmr
  * French - Florian_Dubois
  * Hebrew - elazar
  * Hungarian - kaciokos
  * Japanese - naofumi
  * Lithuanian - gacuxz
  * Spanish - i2nm7s
  * Turkish - etc

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
* Update translations
  * Brazilian Portuguese - douglaslmr
  * Bulgarian - ddmdima96
  * Dutch - revdbrink
  * French - DragoVaillant, Florian_Dubois
  * German - marmo, Strubbl
  * Hebrew - elazar
  * Hungarian - kaciokos
  * Italian - dfdario, passero
  * Japanese - naofumi
  * Korean - timeforwarp
  * Lithuanian - gacuxz
  * Slovak - Cuco
  * Spanish - i2nm7s, Pum
  * Turkish - etc
  * Ukrainian - nathalier

### 6.3.1 (2018-11-07)

* New location row in task edit screen
* Add location departure notifications
* Set CalDAV completion percentage and status
* Bug fixes
* Update translations
  * Brazilian Portuguese - douglaslmr
  * Bulgarian - ddmdima96
  * Dutch - revdbrink
  * French - Florian_Dubois
  * German - marmo, sNiXx, Strubbl
  * Hebrew - elazar
  * Hungarian - kaciokos
  * Italian - dfdario
  * Japanese - naofumi
  * Lithuanian - gacuxz
  * Polish - bilbolodz, hadogenes
  * Spanish - i2nm7s
  * Turkish - etc

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
* Update translations
  * Brazilian Portuguese - douglaslmr
  * Bulgarian - ddmdima96
  * Chinese - Atlantids
  * Dutch - revdbrink
  * French - Florian_Dubois
  * German - marmo
  * Hebrew - elazar
  * Hungarian - kaciokos
  * Italian - dfdario, passero
  * Lithuanian - gacuxz
  * Spanish - i2nm7s
  * Turkish - etc

### 6.1.3 (2018-10-22)

* Fix translation error

### 6.1.2 (2018-10-18)

* Remove missed call functionality due to Google Play Developer policy change
* Fix manual sort issue affecting Samsung Oreo devices
* Fix refresh issue affecting Pure Calendar Widget
* Fix memory leak
* Schedule jobs with WorkManager instead of android-job
* Update translations
  * French - Fabeuss, primokorn
  * Hebrew - elazar
  * Korean - timeforwarp

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
