package com.todoroo.astrid.subtasks;

import static org.tasks.Strings.isNullOrEmpty;
import static org.tasks.db.QueryUtils.showHiddenAndCompleted;

import android.content.Context;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.BuiltInFilterExposer;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.subtasks.SubtasksFilterUpdater.Node;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.tasks.data.TagData;
import org.tasks.data.TagDataDao;
import org.tasks.data.TaskListMetadata;
import org.tasks.data.TaskListMetadataDao;
import org.tasks.injection.ApplicationContext;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

public class SubtasksHelper {

  private final Context context;
  private final Preferences preferences;
  private final TaskDao taskDao;
  private final TagDataDao tagDataDao;
  private final TaskListMetadataDao taskListMetadataDao;

  @Inject
  public SubtasksHelper(
      @ApplicationContext Context context,
      Preferences preferences,
      TaskDao taskDao,
      TagDataDao tagDataDao,
      TaskListMetadataDao taskListMetadataDao) {
    this.context = context;
    this.preferences = preferences;
    this.taskDao = taskDao;
    this.tagDataDao = tagDataDao;
    this.taskListMetadataDao = taskListMetadataDao;
  }

  @Deprecated
  private static List<Long> getIdList(String serializedTree) {
    ArrayList<Long> ids = new ArrayList<>();
    String[] digitsOnly =
        serializedTree.split("[\\[\\],\\s]"); // Split on [ ] , or whitespace chars
    for (String idString : digitsOnly) {
      try {
        if (!isNullOrEmpty(idString)) {
          ids.add(Long.parseLong(idString));
        }
      } catch (NumberFormatException e) {
        Timber.e(e);
      }
    }
    return ids;
  }

  static String[] getStringIdArray(String serializedTree) {
    ArrayList<String> ids = new ArrayList<>();
    String[] values = serializedTree.split("[\\[\\],\"\\s]"); // Split on [ ] , or whitespace chars
    for (String idString : values) {
      if (!isNullOrEmpty(idString)) {
        ids.add(idString);
      }
    }
    return ids.toArray(new String[0]);
  }

  /** Takes a subtasks string containing local ids and remaps it to one containing UUIDs */
  public static String convertTreeToRemoteIds(TaskDao taskDao, String localTree) {
    List<Long> localIds = getIdList(localTree);
    Map<Long, String> idMap = getIdMap(taskDao, localIds);
    idMap.put(-1L, "-1"); // $NON-NLS-1$

    Node tree = SubtasksFilterUpdater.buildTreeModel(localTree, null);
    remapLocalTreeToRemote(tree, idMap);
    return SubtasksFilterUpdater.serializeTree(tree);
  }

  private static void remapTree(Node root, Map<Long, String> idMap, TreeRemapHelper helper) {
    ArrayList<Node> children = root.children;
    for (int i = 0; i < children.size(); i++) {
      Node child = children.get(i);
      Long key = helper.getKeyFromOldUuid(child.uuid);
      String uuid = idMap.get(key);
      if (!Task.isValidUuid(uuid)) {
        children.remove(i);
        children.addAll(i, child.children);
        i--;
      } else {
        child.uuid = uuid;
        remapTree(child, idMap, helper);
      }
    }
  }

  private static void remapLocalTreeToRemote(Node root, Map<Long, String> idMap) {
    remapTree(
        root,
        idMap,
        uuid -> {
          long localId = -1L;
          try {
            localId = Long.parseLong(uuid);
          } catch (NumberFormatException e) {
            Timber.e(e);
          }
          return localId;
        });
  }

  private static Map<Long, String> getIdMap(TaskDao taskDao, List<Long> keys) {
    List<Task> tasks = taskDao.fetch(keys);
    Map<Long, String> map = new HashMap<>();
    for (Task task : tasks) {
      map.put(task.getId(), task.getUuid());
    }
    return map;
  }

  public String applySubtasksToWidgetFilter(Filter filter, String query) {
    if (filter.supportsAstridSorting() && preferences.isAstridSort()) {
      TagData tagData = tagDataDao.getTagByName(filter.listingTitle);
      TaskListMetadata tlm = null;
      if (tagData != null) {
        tlm = taskListMetadataDao.fetchByTagOrFilter(tagData.getRemoteId());
      } else if (BuiltInFilterExposer.isInbox(context, filter)) {
        tlm = taskListMetadataDao.fetchByTagOrFilter(TaskListMetadata.FILTER_ID_ALL);
      } else if (BuiltInFilterExposer.isTodayFilter(context, filter)) {
        tlm = taskListMetadataDao.fetchByTagOrFilter(TaskListMetadata.FILTER_ID_TODAY);
      }
      if (tlm != null) {
        query = query.replaceAll("ORDER BY .*", "");
        query = query + String.format(" ORDER BY %s", getOrderString(tagData, tlm));
        query = showHiddenAndCompleted(query);
        filter.setFilterQueryOverride(query);
      }
    }
    return query;
  }

  private String getOrderString(TagData tagData, TaskListMetadata tlm) {
    String serialized;
    if (tlm != null) {
      serialized = tlm.getTaskIds();
    } else if (tagData != null) {
      serialized = convertTreeToRemoteIds(taskDao, tagData.getTagOrdering());
    } else {
      serialized = "[]"; // $NON-NLS-1$
    }

    return SubtasksFilterUpdater.buildOrderString(getStringIdArray(serialized));
  }

  interface TreeRemapHelper {

    Long getKeyFromOldUuid(String uuid);
  }
}
