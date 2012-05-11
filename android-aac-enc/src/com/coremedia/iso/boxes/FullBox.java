package com.coremedia.iso.boxes;

import com.coremedia.iso.boxes.Box;

/**
 * The <code>FullBox</code> contains all getters and setters specific
 * to a so-called full box according to the ISO/IEC 14496/12 specification.
 */
public interface FullBox extends Box {
    int getVersion();

    void setVersion(int version);

    int getFlags();

    void setFlags(int flags);
}
