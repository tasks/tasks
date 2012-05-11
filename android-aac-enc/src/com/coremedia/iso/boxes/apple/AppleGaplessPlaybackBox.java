package com.coremedia.iso.boxes.apple;

/**
 * Gapless Playback.
 */
public final class AppleGaplessPlaybackBox extends AbstractAppleMetaDataBox {
    public static final String TYPE = "pgap";


    public AppleGaplessPlaybackBox() {
        super(TYPE);
        appleDataBox = AppleDataBox.getUint8AppleDataBox();
    }

}
