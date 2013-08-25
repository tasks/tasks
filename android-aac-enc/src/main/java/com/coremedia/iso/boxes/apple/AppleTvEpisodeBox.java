package com.coremedia.iso.boxes.apple;

/**
 * Tv Episode.
 */
public class AppleTvEpisodeBox extends AbstractAppleMetaDataBox {
    public static final String TYPE = "tves";


    public AppleTvEpisodeBox() {
        super(TYPE);
        appleDataBox = AppleDataBox.getUint32AppleDataBox();
    }

}