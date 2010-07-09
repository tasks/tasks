/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents an intent that can be called on a task being edited
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class EditOperation implements Parcelable {

    /**
     * Plugin Id
     */
    public final String plugin;

    /**
     * Label
     */
    public String text = null;

    /**
     * Intent to call when invoking this operation
     */
    public Intent intent = null;

    /**
     * Create an EditOperation object
     *
     * @param plugin
     *            {@link Addon} identifier that encompasses object
     * @param text
     *            label to display
     * @param intent
     *            intent to invoke. {@link EXTRAS_TASK_ID} will be passed
     */
    public EditOperation(String plugin, String text, Intent intent) {
        this.plugin = plugin;
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
        dest.writeString(plugin);
        dest.writeString(text);
        dest.writeParcelable(intent, 0);
    }

    /**
     * Parcelable creator
     */
    public static final Parcelable.Creator<EditOperation> CREATOR = new Parcelable.Creator<EditOperation>() {
        /**
         * {@inheritDoc}
         */
        public EditOperation createFromParcel(Parcel source) {
            return new EditOperation(source.readString(), source.readString(),
                    (Intent)source.readParcelable(Intent.class.getClassLoader()));
        }

        /**
         * {@inheritDoc}
         */
        public EditOperation[] newArray(int size) {
            return new EditOperation[size];
        };
    };

}
