/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;

import android.app.PendingIntent;
import android.graphics.drawable.BitmapDrawable;

/**
 * Represents an intent that can be called on a task
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskAction {

    /**
     * Label
     */
    public String text = null;

    /**
     * Intent to call when invoking this operation
     */
    public PendingIntent intent = null;

    /**
     * Quick action icon
     */
    public BitmapDrawable icon = null;

    /**
     * Quick action drawable resource
     */
    public int drawable = 0;

    /**
     * Create an EditOperation object
     *
     * @param text
     *            label to display
     * @param intent
     *            intent to invoke. {@link #EXTRAS_TASK_ID} will be passed
     */
    public TaskAction(String text, PendingIntent intent, BitmapDrawable icon) {
        super();
        this.text = text;
        this.intent = intent;
        this.icon = icon;
    }

    // --- parcelable helpers

    /**
     * {@inheritDoc}
     */
    public int describeContents() {
        return 0;
    }
//
//    /**
//     * {@inheritDoc}
//     */
//    public void writeToParcel(Parcel dest, int flags) {
//        dest.writeString(text);
//        dest.writeParcelable(intent, 0);
//        dest.writeParcelable(icon, 0);
//        dest.writeInt(drawable);
//    }
//
//    /**
//     * Parcelable creator
//     */
//    public static final Parcelable.Creator<TaskAction> CREATOR = new Parcelable.Creator<TaskAction>() {
//        /**
//         * {@inheritDoc}
//         */
//        public TaskAction createFromParcel(Parcel source) {
//            TaskAction action = new TaskAction(source.readString(),
//                    (PendingIntent)source.readParcelable(PendingIntent.class.getClassLoader()),
//                    (Bitmap)source.readParcelable(Bitmap.class.getClassLoader()));
//            action.drawable = source.readInt();
//            return action;
//        }
//
//        /**
//         * {@inheritDoc}
//         */
//        public TaskAction[] newArray(int size) {
//            return new TaskAction[size];
//        };
//    };

}
