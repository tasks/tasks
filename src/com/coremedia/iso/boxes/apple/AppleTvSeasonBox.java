package com.coremedia.iso.boxes.apple;

/**
 * Tv Season.
 */
public final class AppleTvSeasonBox extends AbstractAppleMetaDataBox {
    public static final String TYPE = "tvsn";


    public AppleTvSeasonBox() {
        super(TYPE);
        appleDataBox = AppleDataBox.getUint32AppleDataBox();
    }

}