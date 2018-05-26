package org.tasks.reminders;

import static java.util.Arrays.asList;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import java.util.List;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.InjectingDialogFragment;

public class MissedCallDialog extends InjectingDialogFragment {

  @Inject DialogBuilder dialogBuilder;
  private String title;
  private MissedCallHandler handler;

  @Override
  protected void inject(DialogFragmentComponent component) {
    component.inject(this);
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    List<String> actions =
        asList(
            getString(R.string.MCA_return_call),
            getString(R.string.MCA_add_task),
            getString(R.string.MCA_ignore));

    handler = (MissedCallHandler) getActivity();

    return dialogBuilder
        .newDialog()
        .setTitle(title)
        .setItems(
            actions,
            (dialog, which) -> {
              switch (which) {
                case 0:
                  handler.callNow();
                  break;
                case 1:
                  handler.callLater();
                  break;
                default:
                  handler.ignore();
                  break;
              }
            })
        .show();
  }

  @Override
  public void onDismiss(DialogInterface dialog) {
    handler.dismiss();
  }

  public void setTitle(String title) {
    this.title = title;
    Dialog dialog = getDialog();
    if (dialog != null) {
      dialog.setTitle(title);
    }
  }

  public interface MissedCallHandler {

    void callNow();

    void callLater();

    void ignore();

    void dismiss();
  }
}
