/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.data;

import android.graphics.Bitmap;

/**
 * An add-on installable by Astrid
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class AddOn {

    private final boolean free;
    private final boolean internal;
    private final String title;
    private final String author;
    private final String description;
    private final String packageName;
    private final Bitmap icon;

    public AddOn(boolean free, boolean internal, String title, String author, String description,
            String packageName, Bitmap icon) {
        this.free = free;
        this.internal = internal;
        this.title = title;
        this.author = author;
        this.description = description;
        this.packageName = packageName;
        this.icon = icon;
    }

    /**
     * @return whether this add-on is available for free
     */
    public boolean isFree() {
        return free;
    }

    /**
     * @return whether this add-on is signed with the same key as Astrid
     */
    public boolean isInternal() {
        return internal;
    }

    /**
     * @return add-on title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return add-on author
     */
    public String getAuthor() {
        return author;
    }

    /**
     * @return add-on description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return add-on java package name
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * @return add-on icon
     */
    public Bitmap getIcon() {
        return icon;
    }

}
