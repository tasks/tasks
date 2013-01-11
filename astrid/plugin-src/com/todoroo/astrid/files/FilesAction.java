/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.files;

import android.app.PendingIntent;
import android.graphics.drawable.BitmapDrawable;

import com.todoroo.astrid.api.TaskAction;

public class FilesAction extends TaskAction {

    public FilesAction(String text, PendingIntent intent, BitmapDrawable icon) {
        super(text, intent, icon);
    }
//
//    /**
//     * Parcelable creator
//     */
//    @SuppressWarnings("hiding")
//    public static final Parcelable.Creator<FilesAction> CREATOR = new Parcelable.Creator<FilesAction>() {
//        /**
//         * {@inheritDoc}
//         */
//        public FilesAction createFromParcel(Parcel source) {
//            FilesAction action = new FilesAction(source.readString(),
//                    (PendingIntent)source.readParcelable(PendingIntent.class.getClassLoader()),
//                    (Bitmap)source.readParcelable(Bitmap.class.getClassLoader()));
//            action.drawable = source.readInt();
//            return action;
//        }
//
//        /**
//         * {@inheritDoc}
//         */
//        public FilesAction[] newArray(int size) {
//            return new FilesAction[size];
//        };
//    };

}
