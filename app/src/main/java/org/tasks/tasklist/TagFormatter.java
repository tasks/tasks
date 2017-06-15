package org.tasks.tasklist;

import android.content.Context;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Ordering;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.tags.TagService;

import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.themes.ThemeCache;
import org.tasks.themes.ThemeColor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.transform;

public class TagFormatter {

    private static final char SPACE = '\u0020';
    private static final char HAIR_SPACE = '\u200a';

    private final Map<String, TagData> tagMap = new HashMap<>();
    private final TagService tagService;
    private final ThemeCache themeCache;
    private final float tagCharacters;

    @Inject
    public TagFormatter(@ForApplication Context context, TagService tagService, ThemeCache themeCache) {
        this.tagService = tagService;
        this.themeCache = themeCache;

        TypedValue typedValue = new TypedValue();
        context.getResources().getValue(R.dimen.tag_characters, typedValue, true);
        tagCharacters = typedValue.getFloat();

        for (TagData tagData : tagService.getTagList()) {
            tagMap.put(tagData.getUuid(), tagData);
        }
    }

    CharSequence getTagString(List<String> tagUuids) {
        Iterable<TagData> t = filter(transform(tagUuids, uuidToTag), Predicates.notNull());
        List<TagData> firstFourByName = orderByName.leastOf(t, 4);
        int numTags = firstFourByName.size();
        if (numTags == 0) {
            return null;
        }
        List<TagData> firstFourByNameLength = orderByLength.sortedCopy(firstFourByName);
        float maxLength = tagCharacters / numTags;
        for (int i = 0; i < numTags - 1; i++) {
            TagData tagData = firstFourByNameLength.get(i);
            String name = tagData.getName();
            if (name.length() >= maxLength) {
                break;
            }
            float excess = maxLength - name.length();
            int beneficiaries = numTags - i - 1;
            float additional = excess / beneficiaries;
            maxLength += additional;
        }
        List<SpannableString> tagStrings = transform(firstFourByName, tagToString(maxLength));
        SpannableStringBuilder builder = new SpannableStringBuilder();
        for (SpannableString tagString : tagStrings) {
            if (builder.length() > 0) {
                builder.append(HAIR_SPACE);
            }
            builder.append(tagString);
        }
        return builder;
    }

    private Function<TagData, SpannableString> tagToString(final float maxLength) {
        return tagData -> {
            String tagName = tagData.getName();
            tagName = tagName.substring(0, Math.min(tagName.length(), (int) maxLength));
            SpannableString string = new SpannableString(SPACE + tagName + SPACE);
            int themeIndex = tagData.getColor();
            ThemeColor color = themeIndex >= 0 ? themeCache.getThemeColor(themeIndex) : themeCache.getUntaggedColor();
            string.setSpan(new BackgroundColorSpan(color.getPrimaryColor()), 0, string.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            string.setSpan(new ForegroundColorSpan(color.getActionBarTint()), 0, string.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            return string;
        };
    }

    private TagData getTag(String uuid) {
        TagData tagData = tagMap.get(uuid);
        if (tagData == null) {
            tagData = tagService.getTagByUuid(uuid);
            tagMap.put(uuid, tagData);
        }
        return tagData;
    }

    private final Function<String, TagData> uuidToTag = this::getTag;

    private final Ordering<TagData> orderByName = new Ordering<TagData>() {
        @Override
        public int compare(TagData left, TagData right) {
            return left.getName().compareTo(right.getName());
        }
    };

    private final Ordering<TagData> orderByLength = new Ordering<TagData>() {
        @Override
        public int compare(TagData left, TagData right) {
            int leftLength = left.getName().length();
            int rightLength = right.getName().length();
            if (leftLength < rightLength) {
                return -1;
            } else if (rightLength < leftLength) {
                return 1;
            } else {
                return 0;
            }
        }
    };
}
