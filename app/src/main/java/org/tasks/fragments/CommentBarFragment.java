package org.tasks.fragments;

import static org.tasks.files.FileHelper.getPathFromUri;
import static org.tasks.files.ImageHelper.sampleBitmap;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnTextChanged;
import com.google.common.base.Strings;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.data.Task;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.json.JSONException;
import org.json.JSONObject;
import org.tasks.R;
import org.tasks.activities.CameraActivity;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.FragmentComponent;
import org.tasks.preferences.Device;
import org.tasks.preferences.Preferences;
import org.tasks.ui.TaskEditControlFragment;
import timber.log.Timber;

public class CommentBarFragment extends TaskEditControlFragment {

  public static final int TAG = R.string.TEA_ctrl_comments;
  private static final int REQUEST_CODE_CAMERA = 60;
  private static final String EXTRA_TEXT = "extra_text";
  private static final String EXTRA_PICTURE = "extra_picture";
  @Inject Activity activity;
  @Inject DialogBuilder dialogBuilder;
  @Inject Device device;
  @Inject Preferences preferences;

  @BindView(R.id.commentButton)
  View commentButton;

  @BindView(R.id.commentField)
  EditText commentField;

  @BindView(R.id.picture)
  ImageView pictureButton;

  @BindView(R.id.updatesFooter)
  LinearLayout commentBar;

  private CommentBarFragmentCallback callback;
  private Uri pendingCommentPicture = null;

  private static JSONObject savePictureJson(final Uri uri) {
    try {
      JSONObject json = new JSONObject();
      json.put("uri", uri.toString());
      return json;
    } catch (JSONException e) {
      Timber.e(e);
    }
    return null;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    callback = (CommentBarFragmentCallback) activity;
  }

  @Override
  protected void inject(FragmentComponent component) {
    component.inject(this);
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(getLayout(), container, false);
    ButterKnife.bind(this, view);

    if (savedInstanceState != null) {
      String uri = savedInstanceState.getString(EXTRA_PICTURE);
      if (uri != null) {
        pendingCommentPicture = Uri.parse(uri);
        setPictureButtonToPendingPicture();
      }
      commentField.setText(savedInstanceState.getString(EXTRA_TEXT));
    }

    commentField.setHorizontallyScrolling(false);
    commentField.setMaxLines(Integer.MAX_VALUE);

    if (!preferences.getBoolean(R.string.p_show_task_edit_comments, true)) {
      commentBar.setVisibility(View.GONE);
    }

    resetPictureButton();
    return view;
  }

  @Override
  protected int getLayout() {
    return R.layout.fragment_comment_bar;
  }

  @Override
  protected int getIcon() {
    return 0;
  }

  @Override
  public int controlId() {
    return TAG;
  }

  @Override
  public void apply(Task task) {}

  @OnTextChanged(R.id.commentField)
  void onTextChanged(CharSequence s) {
    commentButton.setVisibility(
        pendingCommentPicture == null && Strings.isNullOrEmpty(s.toString())
            ? View.GONE
            : View.VISIBLE);
  }

  @OnEditorAction(R.id.commentField)
  boolean onEditorAction(KeyEvent key) {
    int actionId = key != null ? key.getAction() : 0;
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

    outState.putString(EXTRA_TEXT, commentField.getText().toString());
    if (pendingCommentPicture != null) {
      outState.putString(EXTRA_PICTURE, pendingCommentPicture.toString());
    }
  }

  @OnClick(R.id.picture)
  void onClickPicture() {
    if (pendingCommentPicture == null) {
      showPictureLauncher(null);
    } else {
      showPictureLauncher(
          () -> {
            pendingCommentPicture = null;
            resetPictureButton();
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
    addComment(commentField.getText().toString());
    AndroidUtilities.hideSoftInputForViews(activity, commentField);
  }

  private void setPictureButtonToPendingPicture() {
    String path = getPathFromUri(activity, pendingCommentPicture);
    Bitmap bitmap =
        sampleBitmap(
            path, pictureButton.getLayoutParams().width, pictureButton.getLayoutParams().height);
    pictureButton.setImageBitmap(bitmap);
    commentButton.setVisibility(View.VISIBLE);
  }

  private void addComment(String message) {
    // Allow for users to just add picture
    if (TextUtils.isEmpty(message)) {
      message = " ";
    }
    String picture = null;
    if (pendingCommentPicture != null) {
      JSONObject pictureJson = savePictureJson(pendingCommentPicture);
      if (pictureJson != null) {
        picture = pictureJson.toString();
      }
    }

    if (commentField != null) {
      commentField.setText(""); // $NON-NLS-1$
    }

    pendingCommentPicture = null;
    resetPictureButton();
    callback.addComment(message, picture);
  }

  private void resetPictureButton() {
    TypedValue typedValue = new TypedValue();
    getActivity().getTheme().resolveAttribute(R.attr.actionBarPrimaryText, typedValue, true);
    Drawable drawable =
        DrawableCompat.wrap(
            ContextCompat.getDrawable(getContext(), R.drawable.ic_camera_alt_black_24dp));
    drawable.mutate();
    DrawableCompat.setTint(drawable, typedValue.data);
    pictureButton.setImageDrawable(drawable);
  }

  private void showPictureLauncher(final ClearImageCallback clearImageOption) {
    final List<Runnable> runnables = new ArrayList<>();
    List<String> options = new ArrayList<>();

    final boolean cameraAvailable = device.hasCamera();
    if (cameraAvailable) {
      runnables.add(
          () ->
              startActivityForResult(
                  new Intent(activity, CameraActivity.class), REQUEST_CODE_CAMERA));
      options.add(getString(R.string.take_a_picture));
    }

    if (clearImageOption != null) {
      runnables.add(clearImageOption::clearImage);
      options.add(getString(R.string.actfm_picture_clear));
    }

    if (runnables.size() == 1) {
      runnables.get(0).run();
    } else {
      DialogInterface.OnClickListener listener =
          (d, which) -> {
            runnables.get(which).run();
            d.dismiss();
          };

      // show a menu of available options
      dialogBuilder.newDialog().setItems(options, listener).show().setOwnerActivity(activity);
    }
  }

  public interface CommentBarFragmentCallback {

    void addComment(String message, String picture);
  }

  interface ClearImageCallback {

    void clearImage();
  }
}
