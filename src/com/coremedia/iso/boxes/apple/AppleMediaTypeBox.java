package com.coremedia.iso.boxes.apple;

import java.util.HashMap;
import java.util.Map;

/**
 * itunes MetaData comment box.
 */
public class AppleMediaTypeBox extends AbstractAppleMetaDataBox {
    private static Map<String, String> mediaTypes = new HashMap<String, String>();

    static {
        mediaTypes.put("0", "Movie (is now 9)");
        mediaTypes.put("1", "Normal (Music)");
        mediaTypes.put("2", "Audiobook");
        mediaTypes.put("6", "Music Video");
        mediaTypes.put("9", "Movie");
        mediaTypes.put("10", "TV Show");
        mediaTypes.put("11", "Booklet");
        mediaTypes.put("14", "Ringtone");
    }

    public static final String TYPE = "stik";


    public AppleMediaTypeBox() {
        super(TYPE);
        appleDataBox = AppleDataBox.getUint8AppleDataBox();
    }

    public String getReadableValue() {
        if (mediaTypes.containsKey(getValue())) {
            return mediaTypes.get(getValue());
        } else {
            return "unknown media type " + getValue();
        }

    }
}