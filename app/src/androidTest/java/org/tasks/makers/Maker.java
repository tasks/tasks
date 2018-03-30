package org.tasks.makers;

import static com.natpryce.makeiteasy.MakeItEasy.a;

import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.PropertyValue;

class Maker {

  @SuppressWarnings("unchecked")
  public static <T> T make(
      Instantiator<T> instantiator, PropertyValue<? super T, ?>... properties) {
    return com.natpryce.makeiteasy.MakeItEasy.make(a(instantiator, properties));
  }
}
