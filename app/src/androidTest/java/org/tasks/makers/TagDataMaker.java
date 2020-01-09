package org.tasks.makers;

import static com.natpryce.makeiteasy.Property.newProperty;
import static org.tasks.makers.Maker.make;

import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
import com.natpryce.makeiteasy.PropertyValue;
import org.tasks.data.TagData;

public class TagDataMaker {

  public static final Property<TagData, String> NAME = newProperty();
  public static final Property<TagData, String> UID = newProperty();

  private static final Instantiator<TagData> instantiator = lookup -> {
    TagData tagData = new TagData();
    tagData.setName(lookup.valueOf(NAME, "tag"));
    tagData.setRemoteId(lookup.valueOf(UID, (String) null));
    return tagData;
  };

  @SafeVarargs
  public static TagData newTagData(PropertyValue<? super TagData, ?>... properties) {
    return make(instantiator, properties);
  }
}
