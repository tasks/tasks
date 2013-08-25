package com.coremedia.iso.boxes.apple;

/**
 * itunes MetaData comment box.
 */
public final class AppleGroupingBox extends AbstractAppleMetaDataBox {
    public static final String TYPE = "\u00a9grp";


    public AppleGroupingBox() {
        super(TYPE);
        appleDataBox = AppleDataBox.getStringAppleDataBox();
    }

}