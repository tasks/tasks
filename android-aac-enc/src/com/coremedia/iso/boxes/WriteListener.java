package com.coremedia.iso.boxes;

/**
 * The <class>WriteListener</class> is used to get the offset of
 * a box before writing the box. This can be used if a box written
 * later needs an offset.
 */
public interface WriteListener {
    public void beforeWrite(long offset);
}
