package com.coremedia.iso.boxes.mdat;

public class Segment {
    public Segment(long offset, long size) {
        this.offset = offset;
        this.size = size;
    }

    public long offset;
    public long size;

}