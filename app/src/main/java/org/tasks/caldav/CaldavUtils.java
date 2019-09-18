package org.tasks.caldav;

import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.newHashSet;

import androidx.annotation.Nullable;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.tasks.data.TagData;
import org.tasks.data.TagDataDao;

public class CaldavUtils {
  public static @Nullable at.bitfire.ical4android.Task fromVtodo(String vtodo) {
    List<at.bitfire.ical4android.Task> tasks =
        at.bitfire.ical4android.Task.Companion.fromReader(new StringReader(vtodo));
    return tasks.size() == 1 ? tasks.get(0) : null;
  }

  public static List<TagData> getTags(TagDataDao tagDataDao, List<String> categories) {
    if (categories.isEmpty()) {
      return Collections.emptyList();
    }
    List<TagData> selectedTags = tagDataDao.getTags(categories);
    Set<String> toCreate =
        difference(newHashSet(categories), newHashSet(transform(selectedTags, TagData::getName)));
    for (String name : toCreate) {
      TagData tag = new TagData(name);
      tagDataDao.createNew(tag);
      selectedTags.add(tag);
    }
    return selectedTags;
  }
}
