package com.coremedia.iso.boxes.apple;

import java.util.HashMap;
import java.util.Map;

/**
 * itunes MetaData comment box.
 */
public class AppleStoreCountryCodeBox extends AbstractAppleMetaDataBox {
    private static Map<String, String> countryCodes = new HashMap<String, String>();

    static {
        countryCodes.put("143460", "Australia");
        countryCodes.put("143445", "Austria");
        countryCodes.put("143446", "Belgium");
        countryCodes.put("143455", "Canada");
        countryCodes.put("143458", "Denmark");
        countryCodes.put("143447", "Finland");
        countryCodes.put("143442", "France");
        countryCodes.put("143443", "Germany");
        countryCodes.put("143448", "Greece");
        countryCodes.put("143449", "Ireland");
        countryCodes.put("143450", "Italy");
        countryCodes.put("143462", "Japan");
        countryCodes.put("143451", "Luxembourg");
        countryCodes.put("143452", "Netherlands");
        countryCodes.put("143461", "New Zealand");
        countryCodes.put("143457", "Norway");
        countryCodes.put("143453", "Portugal");
        countryCodes.put("143454", "Spain");
        countryCodes.put("143456", "Sweden");
        countryCodes.put("143459", "Switzerland");
        countryCodes.put("143444", "United Kingdom");
        countryCodes.put("143441", "United States");
    }

    public static final String TYPE = "sfID";


    public AppleStoreCountryCodeBox() {
        super(TYPE);
        appleDataBox = AppleDataBox.getUint32AppleDataBox();
    }


    public String getReadableValue() {
        if (countryCodes.containsKey(getValue())) {
            return countryCodes.get(getValue());
        } else {
            return "unknown country code " + getValue();
        }

    }
}