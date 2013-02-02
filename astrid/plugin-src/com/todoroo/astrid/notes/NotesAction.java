/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.notes;

import android.app.PendingIntent;
import android.graphics.drawable.BitmapDrawable;

import com.todoroo.astrid.api.TaskAction;

public class NotesAction extends TaskAction {

    public NotesAction(String text, PendingIntent intent, BitmapDrawable icon) {
        super(text, intent, icon);
    }
//
//    /**
//     * Parcelable creator
//     */
//    @SuppressWarnings("hiding")
//    public static final Parcelable.Creator<NotesAction> CREATOR = new Parcelable.Creator<NotesAction>() {
//        /**
//         * {@inheritDoc}
//         */
//        public NotesAction createFromParcel(Parcel source) {
//            NotesAction action = new NotesAction(source.readString(),
//                    (PendingIntent)source.readParcelable(PendingIntent.class.getClassLoader()),
//                    (Bitmap)source.readParcelable(Bitmap.class.getClassLoader()));
//            action.drawable = source.readInt();
//            return action;
//        }
//
//        /**
//         * {@inheritDoc}
//         */
//        public NotesAction[] newArray(int size) {
//            return new NotesAction[size];
//        };
//    };

}
