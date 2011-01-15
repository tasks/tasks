[Astrid](http://www.weloveastrid.com/) - Task Management Done Right
================================  
Astrid strives to be a simple and effective organization tool for Google Android phones. It comes with features like reminders, tagging, widgets, and integration with online synchronization services.

Interested in using Astrid? Search "astrid" in Android Market. Look for the smiling pink squid!

If you would like to help out with the Astrid project, you're in the right place.

Getting Started With Development
---------------

1. Create your own fork of Astrid and astridApi by clicking on Github's *Fork* button above, and the same button in the [astridApi project](https://github.com/todoroo/astridApi) (you may have to log in first - [github help](http://help.github.com/forking/)).

2. Install the following: 
 • *[git](http://git.or.cz/)*
 • *[Eclipse](http://eclipse.org)* (preferred: Eclipse IDE for Java Developers)
 • *[Android SDK](http://developer.android.com/sdk/index.html)*
 • *[ADT Plugin for Eclipse](http://developer.android.com/sdk/eclipse-adt.html)*

3. Use **git** to clone your forked repositories 

`git clone git@github.com:yourgithubid/astrid.git` 
`git clone git@github.com:yourgithubid/astridApi.git` 

(see Github's instructions if you need help). Follow the [Github Line Ending Help](http://help.github.com/dealing-with-lineendings/)

4. Open up **eclipse** and import the *astrid*, *astridApi*, and *astrid-tests* projects. There should be no compilation errors. If there are, check the Android page of Eclipse Project Properties to verify the astridApi project was found. You might have to **clean** the projects from within **eclipse** 

*from Eclipse: project-menu -> clean -> select projects -> check "start a build immediately" if "automatic build" is not enabled*

5. Launch the *astrid* project as an **Android Application**, or the *astrid-tests* project as an **Android JUnit Test Suite**.

6. Join the [Astrid Power-Users List](http://groups.google.com/group/astrid-power).

7. Check out the [Product Roadmap](http://wiki.github.com/todoroo/astrid/) and [Issues](http://github.com/todoroo/astrid/issues), and look for something you'd want to tackle.

8. Follow the following contributors workflow and help make Astrid better!

9. Sign a [Contributors License Agreement](https://github.com/downloads/todoroo/astrid/Contributors%20Licensing%20Agreement.pdf) and send it to astrid AT todoroo.com 

Testing on a device - Debugging
---------------
How to debug/test on a USB device: (JoshuaGross Jan 14, 2011)

1. Plug in your device and make sure you can see it by running `adb devices`

2. If you cannot see the device, or want to restart debugging, do:

`adb kill-server`

`adb usb`

`adb devices` (make sure your device is listed)

`adb uninstall com.todoroo.astrid.tests` (you should see "Failure" if this was uninstalled already)

`adb uninstall com.todoroo.astrid` (you should see "Failure" if this was uninstalled already)

3. If you have not already, set up Run configurations in Eclipse; either under the "Run" or "Debug" menus. To test the application, set up "Android Application". To run JUnit tests, set up "Android JUnit Tests". Settings should autofill for you, you should be able to open a new configuration and run it. If you run it in debug mode, see #2 above for getting adb to work in case it is finicky.

4. When running unit tests: make sure to *close all JUnit windows* before running tests. JUnit/Eclipse/Android work together in a very, very janky way. You may get incorrect results if you do not close the window out before running tests. You have been warned.

5. Make sure to commit changes both to the "astrid" project and the "astridApi" project while developing.

Contributors workflow
---------------

**Setup:**

`git clone git@github.com:yourgithubid/astrid.git`

`git clone git@github.com:yourgithub/astridApi.git`

`git remote add upstream git://github.com/todoroo/astrid.git`

`git remote add upstream git://github.com/todoroo/astridApi.git`

**Working on new features/fixes:**

`git checkout -b my-new-features`  

work, work, work! 
  
`git commit` (a separate commit for each bug fix, feature change, style or copy edit please!)
  
`git fetch upstream`

`git rebase -i upstream/master` (i like to rebase -i to verify what i'm committing and squish small commits)
  
`git push origin my-new-features`
  
then go to github and submit a pull request!  

Contact
-------
For support requests, use the Astrid issue tracker. For development questions, contact [timsu](http://github.com/timsu) via e-mail.

Astrid also has an IRC channel, irc.freenode.net #astrid
