/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.taskrabbit;

import java.io.ByteArrayOutputStream;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.astrid.actfm.ActFmCameraModule;
import com.todoroo.astrid.actfm.ActFmCameraModule.CameraResultCallback;
import com.todoroo.astrid.actfm.ActFmCameraModule.ClearImageCallback;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.taskrabbit.TaskRabbitActivity.ActivityResultSetListener;
import com.todoroo.astrid.taskrabbit.TaskRabbitActivity.TaskRabbitSetListener;
import com.todoroo.astrid.ui.PopupControlSet;

public class TaskRabbitNameControlSet extends PopupControlSet implements TaskRabbitSetListener, ActivityResultSetListener{

    protected final EditText editText;
    protected final TextView notesPreview;

    private final ImageButton pictureButton;
    private Bitmap pendingCommentPicture = null;


    public TaskRabbitNameControlSet(Activity activity, int viewLayout,
            int displayViewLayout, int titleID) {
        super(activity, viewLayout, displayViewLayout, titleID);
        editText = (EditText) getView().findViewById(R.id.notes);
        notesPreview = (TextView) getDisplayView().findViewById(R.id.display_row_edit);
        displayText.setText(activity.getString(titleID));
        editText.setMaxLines(Integer.MAX_VALUE);

        pictureButton = (ImageButton) getDisplayView().findViewById(R.id.picture);
        if (pictureButton != null) {

        final ClearImageCallback clearImage = new ClearImageCallback() {
            @Override
            public void clearImage() {
                pendingCommentPicture = null;
                pictureButton.setImageResource(R.drawable.camera_button);
            }
        };
        pictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pendingCommentPicture != null)
                    ActFmCameraModule.showPictureLauncher(TaskRabbitNameControlSet.this.activity, clearImage);
                else
                    ActFmCameraModule.showPictureLauncher(TaskRabbitNameControlSet.this.activity, null);
            }
        });
        }

    }

    @Override
    protected void additionalDialogSetup() {
        dialog.getWindow()
        .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    @Override
    public void saveToDatabase(JSONObject json, String key) throws JSONException {
        json.put(key, editText.getText().toString());
    }


    @Override
    @SuppressWarnings("nls")
    public void postToTaskRabbit(JSONObject json, String key) throws JSONException {
        String nameKey = activity.getString(R.string.tr_set_key_description);
        if (key.equals(activity.getString(R.string.tr_set_key_name)) && json.has(nameKey)) {
            json.put(nameKey, json.optString(nameKey, "") + "\nRestaurant Name: " + editText.getText().toString());
        }
        else {
            json.put(nameKey, json.optString(nameKey, "") + "\n" + editText.getText().toString());
        }

        if (pendingCommentPicture != null) {
            String picture = buildPictureData(pendingCommentPicture);
            JSONObject pictureArray = new JSONObject();
            pictureArray.put("image", picture);
            json.put("uploaded_photos_attributes", new JSONObject().put("1", pictureArray));
        }
    }

    public static String buildPictureData(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if(bitmap.getWidth() > 512 || bitmap.getHeight() > 512) {
            float scale = Math.min(512f / bitmap.getWidth(), 512f / bitmap.getHeight());
            bitmap = Bitmap.createScaledBitmap(bitmap, (int)(scale * bitmap.getWidth()),
                    (int)(scale * bitmap.getHeight()), false);
        }
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        byte[] bytes = baos.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    @Override
    public void readFromModel(JSONObject json, String key, int mode) {
        if (json.optInt(key, -1) == 0) {
            editText.setHint(displayText.getText().toString());
            return;
        }
        String value = json.optString(key, "");  //$NON-NLS-1$
        if (!TextUtils.isEmpty(value)) {
        editText.setTextKeepState(value);
        notesPreview.setText(value);
        }

    }


    @Override
    public boolean activityResult (int requestCode, int resultCode, Intent data) {
    if (pictureButton != null) {
        CameraResultCallback callback = new CameraResultCallback() {
            @Override
            public void handleCameraResult(Bitmap bitmap) {
                pendingCommentPicture = bitmap;
                pictureButton.setImageBitmap(pendingCommentPicture);
            }
        };

        return (ActFmCameraModule.activityResult(activity,
                requestCode, resultCode, data, callback));
    }
    return false;
    }


    @Override
    protected void refreshDisplayView() {
        notesPreview.setText(editText.getText());
    }

    @Override
    public void readFromTask(Task task) {
        //
    }

    @Override
    protected void readFromTaskOnInitialize() {
        // Nothing, we don't lazy load this control set yet
    }

    @Override
    public String writeToModel(Task task) {
        return null;
    }

    @Override
    protected String writeToModelAfterInitialized(Task task) {
        // Nothing, we don't lazy load this control set yet
        return null;
    }

    @Override
    protected void afterInflate() {
        // Nothing, we don't lazy load this control set yet
    }

    @Override
    protected boolean onOkClick() {
        InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        return super.onOkClick();
    }

    @Override
    protected void onCancelClick() {
        super.onCancelClick();
        InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    public boolean hasNotes() {
        return !TextUtils.isEmpty(editText.getText());
    }

}
