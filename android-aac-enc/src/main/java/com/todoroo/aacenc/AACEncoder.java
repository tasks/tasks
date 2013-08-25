package com.todoroo.aacenc;

public class AACEncoder {

    /**
     * Native JNI - initialize AAC encoder
     *
     */
    public native void init(int bitrate, int channels,
            int sampleRate, int bitsPerSample, String outputFile);

    /**
     * Native JNI - encode one or more frames
     *
     */
    public native void encode(byte[] inputArray);

    /**
     * Native JNI - uninitialize AAC encoder and flush file
     *
     */
    public native void uninit();

    static {
        System.loadLibrary("aac-encoder");
    }

}
