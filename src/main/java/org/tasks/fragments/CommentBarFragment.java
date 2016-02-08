package org.tasks.fragments;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.common.base.Strings;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.UserActivity;

import org.json.JSONObject;
import org.tasks.R;
import org.tasks.activities.CameraActivity;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.InjectingFragment;
import org.tasks.preferences.Device;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnTextChanged;

import static org.tasks.files.FileHelper.getPathFromUri;
import static org.tasks.files.ImageHelper.sampleBitmap;

public class CommentBarFragment extends InjectingFragment {

    public interface CommentBarFragmentCallback {
        void addComment(String message, String actionCode, String picture);
    }

    public interface ClearImageCallback {
        void clearImage();
    }

    private static final int REQUEST_CODE_CAMERA = 60;
    private static final String TOKEN_PICTURE_IN_PROGRESS = "picture_in_progress"; //$NON-NLS-1$

    private final int cameraButton = R.drawable.ic_camera_alt_white_24dp;

    @Inject Activity activity;
    @Inject DialogBuilder dialogBuilder;
    @Inject Device device;

    @Bind(R.id.commentButton) View commentButton;
    @Bind(R.id.commentField) EditText commentField;
    @Bind(R.id.picture) ImageView pictureButton;

    private CommentBarFragmentCallback callback;
    private Uri pendingCommentPicture = null;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        callback = (CommentBarFragmentCallback) activity;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_comment_bar, container, false);
        ButterKnife.bind(this, view);

        if (savedInstanceState != null) {
            String uri = savedInstanceState.getString(TOKEN_PICTURE_IN_PROGRESS);
            if (uri != null) {
                pendingCommentPicture = Uri.parse(uri);
                setPictureButtonToPendingPicture();
            }
        }

        commentField.setHorizontallyScrolling(false);
        commentField.setMaxLines(Integer.MAX_VALUE);
        return view;
    }

    @OnTextChanged(R.id.commentField)
    void onTextChanged(CharSequence s) {
        commentButton.setVisibility(pendingCommentPicture == null && Strings.isNullOrEmpty(s.toString())
                ? View.GONE
                : View.VISIBLE);
    }

    @OnEditorAction(R.id.commentField)
    boolean onEditorAction(KeyEvent key) {
        int actionId = key.getAction();
        if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
            if (commentField.getText().length() > 0 || pendingCommentPicture != null) {
                addComment();
                return true;
            }
        }
        return false;
    }

    @OnClick(R.id.commentButton)
    void addClicked() {
        addComment();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (pendingCommentPicture != null) {
            outState.putString(TOKEN_PICTURE_IN_PROGRESS, pendingCommentPicture.toString());
        }
    }

    @OnClick(R.id.picture)
    void onClickPicture() {
        if (pendingCommentPicture == null) {
            showPictureLauncher(null);
        } else {
            showPictureLauncher(new ClearImageCallback() {
                @Override
                public void clearImage() {
                    pendingCommentPicture = null;
                    pictureButton.setImageResource(cameraButton);
                }
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CAMERA) {
            if (resultCode == Activity.RESULT_OK) {
                pendingCommentPicture = data.getParcelableExtra(CameraActivity.EXTRA_URI);
                setPictureButtonToPendingPicture();
                commentField.requestFocus();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void addComment() {
        addComment(commentField.getText().toString(), UserActivity.ACTION_TASK_COMMENT);
        AndroidUtilities.hideSoftInputForViews(activity, commentField);
    }

    private void setPictureButtonToPendingPicture() {
        String path = getPathFromUri(activity, pendingCommentPicture);
        Bitmap bitmap = sampleBitmap(path, pictureButton.getLayoutParams().width, pictureButton.getLayoutParams().height);
        pictureButton.setImageBitmap(bitmap);
        commentButton.setVisibility(View.VISIBLE);
    }

    private void addComment(String message, String actionCode) {
        // Allow for users to just add picture
        if (TextUtils.isEmpty(message)) {
            message = " ";
        }
        String picture = null;
        if (pendingCommentPicture != null) {
            JSONObject pictureJson = RemoteModel.PictureHelper.savePictureJson(pendingCommentPicture);
            if (pictureJson != null) {
                picture = pictureJson.toString();
            }
        }

        if (commentField != null) {
            commentField.setText(""); //$NON-NLS-1$
        }

        pendingCommentPicture = null;
        pictureButton.setImageResource(cameraButton);
        callback.addComment(message, actionCode, picture);
    }

    private void showPictureLauncher(final ClearImageCallback clearImageOption) {
        final List<Runnable> runnables = new ArrayList<>();
        List<String> options = new ArrayList<>();

        final boolean cameraAvailable = device.hasCamera();
        if (cameraAvailable) {
            runnables.add(new Runnable() {
                @Override
                public void run() {
                    startActivityForResult(new Intent(activity, CameraActivity.class), REQUEST_CODE_CAMERA);
                }
            });
            options.add(getString(R.string.take_a_picture));
        }

        if (clearImageOption != null) {
            runnables.add(new Runnable() {
                @Override
                public void run() {
                    clearImageOption.clearImage();
                }
            });
            options.add(getString(R.string.actfm_picture_clear));
        }

        if (runnables.size() == 1) {
            runnables.get(0).run();
        } else {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity,
                    android.R.layout.simple_spinner_dropdown_item, options.toArray(new String[options.size()]));

            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int which) {
                    runnables.get(which).run();
                    d.dismiss();
                }
            };

            // show a menu of available options
            dialogBuilder.newDialog()
                    .setAdapter(adapter, listener)
                    .show().setOwnerActivity(activity);
        }
    }
}
