package com.coremedia.iso.boxes.apple;

/**
 *
 */
public class AppleRecordingYearBox extends AbstractAppleMetaDataBox {
    public static final String TYPE = "\u00a9day";


    public AppleRecordingYearBox() {
        super(TYPE);
        appleDataBox = AppleDataBox.getStringAppleDataBox();
    }


}