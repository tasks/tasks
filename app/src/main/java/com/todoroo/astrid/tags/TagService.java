/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.tags;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;

import android.text.TextUtils;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.tasks.data.TagDao;
import org.tasks.data.TagData;
import org.tasks.data.TagDataDao;
import org.tasks.injection.ApplicationScope;

/**
 * Provides operations for working with tags
 *
 * @author Tim Su <tim@todoroo.com>
 */
@ApplicationScope
public final class TagService {

  private final TagDataDao tagDataDao;
  private final TagDao tagDao;

  @Inject
  public TagService(TagDataDao tagDataDao, TagDao tagDao) {
    this.tagDataDao = tagDataDao;
    this.tagDao = tagDao;
  }

  /**
   * Return all tags ordered by given clause
   *
   * @return empty array if no tags, otherwise array
   */
  public TagData[] getGroupedTags() {
    List<TagData> tags = tagDataDao.tagDataOrderedByName();
    return tags.toArray(new TagData[tags.size()]);
  }

  public TagData tagFromUUID(String uuid) {
    return tagDataDao.getByUuid(uuid);
  }

  public List<TagData> getTagDataForTask(String uuid) {
    List<String> uuids = tagDao.getTagUids(uuid);
    return newArrayList(transform(uuids, this::tagFromUUID));
  }

  public ArrayList<TagData> getTagDataForTask(long taskId) {
    List<String> uuids = tagDao.getTagUids(taskId);
    return newArrayList(transform(uuids, this::tagFromUUID));
  }

  /** Return all tags (including metadata tags and TagData tags) in an array list */
  public List<TagData> getTagList() {
    final List<TagData> tagList = new ArrayList<>();
    for (TagData tagData : tagDataDao.tagDataOrderedByName()) {
      if (!TextUtils.isEmpty(tagData.getName())) {
        tagList.add(tagData);
      }
    }
    return tagList;
  }

  public TagData getTagByUuid(String uuid) {
    return tagDataDao.getByUuid(uuid);
  }

  /**
   * If a tag already exists in the database that case insensitively matches the given tag, return
   * that. Otherwise, return the argument
   */
  public String getTagWithCase(String tag) {
    TagData tagData = tagDataDao.getTagByName(tag);
    if (tagData != null) {
      return tagData.getName();
    }
    return tag;
  }

  public void rename(String uuid, String newName) {
    tagDataDao.rename(uuid, newName);
    tagDao.rename(uuid, newName);
  }
}
