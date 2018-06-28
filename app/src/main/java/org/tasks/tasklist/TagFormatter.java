package org.tasks.tasklist;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.transform;

import android.content.Context;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.Ordering;
import com.todoroo.astrid.tags.TagService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.CaldavCalendar;
import org.tasks.data.CaldavDao;
import org.tasks.data.GoogleTaskList;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.data.TagData;
import org.tasks.injection.ForApplication;
import org.tasks.themes.ThemeCache;
import org.tasks.themes.ThemeColor;

public class TagFormatter {

  private static final char SPACE = '\u0020';
  private static final char HAIR_SPACE = '\u200a';
  private static final int MAX_TAGS = 4;

  private final Map<String, ColoredString> tagMap = new HashMap<>();
  private final Map<String, ColoredString> googleTaskLists = new HashMap<>();
  private final Map<String, ColoredString> caldavCalendars = new HashMap<>();
  private final TagService tagService;
  private final ThemeCache themeCache;
  private final GoogleTaskListDao googleTaskListDao;
  private final CaldavDao caldavDao;
  private final float tagCharacters;
  private final Ordering<ColoredString> orderByName =
      new Ordering<ColoredString>() {
        @Override
        public int compare(ColoredString left, ColoredString right) {
          return left.name.compareTo(right.name);
        }
      };
  private final Ordering<ColoredString> orderByLength =
      new Ordering<ColoredString>() {
        @Override
        public int compare(ColoredString left, ColoredString right) {
          int leftLength = left.name.length();
          int rightLength = right.name.length();
          if (leftLength < rightLength) {
            return -1;
          } else if (rightLength < leftLength) {
            return 1;
          } else {
            return 0;
          }
        }
      };

  @Inject
  public TagFormatter(
      @ForApplication Context context,
      TagService tagService,
      ThemeCache themeCache,
      GoogleTaskListDao googleTaskListDao,
      CaldavDao caldavDao) {
    this.tagService = tagService;
    this.themeCache = themeCache;
    this.googleTaskListDao = googleTaskListDao;
    this.caldavDao = caldavDao;

    TypedValue typedValue = new TypedValue();
    context.getResources().getValue(R.dimen.tag_characters, typedValue, true);
    tagCharacters = typedValue.getFloat();

    for (TagData tagData : tagService.getTagList()) {
      tagMap.put(tagData.getRemoteId(), new ColoredString(tagData));
    }
    for (CaldavCalendar calendar : caldavDao.getCalendars()) {
      caldavCalendars.put(calendar.getUuid(), new ColoredString(calendar));
    }
    for (GoogleTaskList list : googleTaskListDao.getAllLists()) {
      googleTaskLists.put(list.getRemoteId(), new ColoredString(list));
    }
  }

  CharSequence getTagString(String caldav, String googleTask, List<String> tagUuids) {
    List<ColoredString> strings = new ArrayList<>();
    if (!Strings.isNullOrEmpty(googleTask)) {
      ColoredString googleTaskList = getGoogleTaskList(googleTask);
      if (googleTaskList != null) {
        strings.add(googleTaskList);
      }
    } else if (!Strings.isNullOrEmpty(caldav)) {
      ColoredString caldavCalendar = getCaldavCalendar(caldav);
      if (caldavCalendar != null) {
        strings.add(caldavCalendar);
      }
    }

    Iterable<ColoredString> tags = filter(transform(tagUuids, this::getTag), Predicates.notNull());
    strings.addAll(0, orderByName.leastOf(tags, MAX_TAGS - strings.size()));
    int numTags = strings.size();
    if (numTags == 0) {
      return null;
    }
    List<ColoredString> firstFourByNameLength = orderByLength.sortedCopy(strings);
    float maxLength = tagCharacters / numTags;
    for (int i = 0; i < numTags - 1; i++) {
      ColoredString tagData = firstFourByNameLength.get(i);
      String name = tagData.name;
      if (name.length() >= maxLength) {
        break;
      }
      float excess = maxLength - name.length();
      int beneficiaries = numTags - i - 1;
      float additional = excess / beneficiaries;
      maxLength += additional;
    }
    List<SpannableString> tagStrings = transform(strings, tagToString(maxLength));
    SpannableStringBuilder builder = new SpannableStringBuilder();
    for (SpannableString tagString : tagStrings) {
      if (builder.length() > 0) {
        builder.append(HAIR_SPACE);
      }
      builder.append(tagString);
    }
    return builder;
  }

  private Function<ColoredString, SpannableString> tagToString(final float maxLength) {
    return tagData -> {
      String tagName = tagData.name;
      tagName = tagName.substring(0, Math.min(tagName.length(), (int) maxLength));
      SpannableString string = new SpannableString(SPACE + tagName + SPACE);
      int themeIndex = tagData.color;
      ThemeColor color =
          themeIndex >= 0 ? themeCache.getThemeColor(themeIndex) : themeCache.getUntaggedColor();
      string.setSpan(
          new BackgroundColorSpan(color.getPrimaryColor()),
          0,
          string.length(),
          Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
      string.setSpan(
          new ForegroundColorSpan(color.getActionBarTint()),
          0,
          string.length(),
          Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
      return string;
    };
  }

  private ColoredString getGoogleTaskList(String remoteId) {
    ColoredString googleTaskList = googleTaskLists.get(remoteId);
    if (googleTaskList == null) {
      GoogleTaskList byRemoteId = googleTaskListDao.getByRemoteId(remoteId);
      if (byRemoteId != null) {
        googleTaskList = new ColoredString(byRemoteId);
      }
      googleTaskLists.put(remoteId, googleTaskList);
    }
    return googleTaskList;
  }

  private ColoredString getCaldavCalendar(String uuid) {
    ColoredString calendar = caldavCalendars.get(uuid);
    if (calendar == null) {
      CaldavCalendar byUuid = caldavDao.getCalendar(uuid);
      if (byUuid != null) {
        calendar = new ColoredString(byUuid);
      }
      caldavCalendars.put(uuid, calendar);
    }
    return calendar;
  }

  private ColoredString getTag(String uuid) {
    ColoredString tagData = tagMap.get(uuid);
    if (tagData == null) {
      TagData tagByUuid = tagService.getTagByUuid(uuid);
      if (tagByUuid != null) {
        tagData = new ColoredString(tagByUuid);
      }
      tagMap.put(uuid, tagData);
    }
    return tagData;
  }

  private class ColoredString {

    final String name;
    final int color;

    ColoredString(TagData tagData) {
      name = tagData.getName();
      color = tagData.getColor();
    }

    ColoredString(GoogleTaskList googleTaskList) {
      name = googleTaskList.getTitle();
      color = googleTaskList.getColor();
    }

    ColoredString(CaldavCalendar caldavCalendar) {
      name = caldavCalendar.getName();
      color = caldavCalendar.getColor();
    }
  }
}
