/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;

import android.app.PendingIntent;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents an intent that can be called on a task
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskAction implements Parcelable {

    /**
     * Label
     */
    public String text = null;

    /**
     * Intent to call when invoking this operation
     */
    public PendingIntent intent = null;

    /**
     * Create an EditOperation object
     *
     * @param text
     *            label to display
     * @param intent
     *            intent to invoke. {@link EXTRAS_TASK_ID} will be passed
     */
    public TaskAction(String text, PendingIntent intent) {
        super();
        this.text = text;
        this.intent = intent;
    }

    // --- parcelable helpers

    /**
     * {@inheritDoc}
     */
    public int describeContents() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(text);
        dest.writeParcelable(intent, 0);
    }

    /**
     * Parcelable creator
     */
    public static final Parcelable.Creator<TaskAction> CREATOR = new Parcelable.Creator<TaskAction>() {
        /**
         * {@inheritDoc}
         */
        public TaskAction createFromParcel(Parcel source) {
            return new TaskAction(source.readString(), (PendingIntent)source.readParcelable(
                    PendingIntent.class.getClassLoader()));
        }

        /**
         * {@inheritDoc}
         */
        public TaskAction[] newArray(int size) {
            return new TaskAction[size];
        };
    };

}
