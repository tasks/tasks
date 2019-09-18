package org.tasks.makers;

import static com.natpryce.makeiteasy.Property.newProperty;
import static org.tasks.makers.Maker.make;

import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
import com.natpryce.makeiteasy.PropertyValue;
import com.todoroo.astrid.data.Task;
import org.tasks.data.Tag;
import org.tasks.data.TagData;

public class TagMaker {

  public static final Property<Tag, String> NAME = newProperty();
  public static final Property<Tag, TagData> TAGDATA = newProperty();
  public static final Property<Tag, Task> TASK = newProperty();

  private static final Instantiator<Tag> instantiator = lookup -> {
    Tag tag = new Tag();
    Task task = lookup.valueOf(TASK, (Task) null);
    assert(task != null);
    tag.setTask(task.getId());
    tag.setTaskUid(task.getUuid());
    TagData tagData = lookup.valueOf(TAGDATA, (TagData) null);
    assert(tagData != null);
    tag.setTagUid(tagData.getRemoteId());
    return tag;
  };

  @SafeVarargs
  public static Tag newTag(PropertyValue<? super Tag, ?>... properties) {
    return make(instantiator, properties);
  }
}
