Android AAC Encoder project
============================

Extraction of Android Stagefright VO AAC encoder with a nice Java API.

In addition, includes a patched [MP4Parser](http://code.google.com/p/mp4parser) Java library for wrapping AAC files in an MP4 container to produce M4A audio files playable by Google Chrome and Apple QuickTime.

This project is set up as a single Eclipse project with a Main.java example activity. AAC encoding logic is found in jni/aac-enc.c, which needs to be built with the [Android NDK](http://developer.android.com/sdk/ndk/index.html). I used NDK r7c, but any version should work.

Why?
----

- smaller code footprint than FFmpeg (< 500k compared to > 2M)
- less native code to compile = less work to support new architectures
- easiest way to make an M4A file


License
-------

This project is released under the [Apache License, version 2](http://www.apache.org/licenses/LICENSE-2.0)

Patents
-------

This project grants you no rights to any of the patents this technology may require. However, since Android version 4.0 and up ship with the Stagefright VO AAC encoder, it is my understanding that you can release code that depends on these libraries for any version of Android. Please note that I am not a lawyer.


Have changes?
-------------

Pull requests are accepted!