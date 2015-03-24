package org.tasks.makers;

import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.PropertyValue;

import static com.natpryce.makeiteasy.MakeItEasy.a;

public class Maker {
    @SuppressWarnings("unchecked")
    public static <T> T make(Instantiator<T> instantiator, PropertyValue<? super T, ?>... properties) {
        return com.natpryce.makeiteasy.MakeItEasy.make(a(instantiator, properties));
    }
}
