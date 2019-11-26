package org.tasks.caldav;

import static com.google.common.collect.Iterables.removeIf;
import static com.google.common.collect.Iterables.tryFind;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.newHashSet;

import androidx.annotation.Nullable;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.parameter.RelType;
import net.fortuna.ical4j.model.property.RelatedTo;
import org.tasks.data.TagData;
import org.tasks.data.TagDataDao;

public class CaldavUtils {
  private static final Predicate<RelatedTo> IS_PARENT =
      r -> r.getParameters().isEmpty() || r.getParameter(Parameter.RELTYPE) == RelType.PARENT;

  public static @Nullable at.bitfire.ical4android.Task fromVtodo(String vtodo) {
    List<at.bitfire.ical4android.Task> tasks =
        at.bitfire.ical4android.Task.Companion.tasksFromReader(new StringReader(vtodo));
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

  public static void setParent(at.bitfire.ical4android.Task remote, @Nullable String value) {
    LinkedList<RelatedTo> relatedTo = remote.getRelatedTo();
    if (Strings.isNullOrEmpty(value)) {
      removeIf(relatedTo, IS_PARENT);
    } else {
      Optional<RelatedTo> parent = tryFind(relatedTo, IS_PARENT);
      if (parent.isPresent()) {
        parent.get().setValue(value);
      } else {
        relatedTo.add(new RelatedTo(value));
      }
    }
  }

  public static @Nullable String getParent(at.bitfire.ical4android.Task remote) {
    LinkedList<RelatedTo> relatedTo = remote.getRelatedTo();
    Optional<RelatedTo> parent = tryFind(relatedTo, IS_PARENT);
    return parent.isPresent() ? parent.get().getValue() : null;
  }
}
