package com.coremedia.iso.boxes.apple;

/**
 * itunes MetaData comment box.
 */
public class AppleStoreAccountTypeBox extends AbstractAppleMetaDataBox {
    public static final String TYPE = "akID";


    public AppleStoreAccountTypeBox() {
        super(TYPE);
        appleDataBox = AppleDataBox.getUint8AppleDataBox();
    }

    public String getReadableValue() {
        byte value = this.appleDataBox.getData()[0];
        switch (value) {
            case 0:
                return "iTunes Account";
            case 1:
                return "AOL Account";
            default:
                return "unknown Account";
        }

    }
}