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

public class NotificationDialog extends InjectingDialogFragment {

  @Inject DialogBuilder dialogBuilder;
  private String title;
  private NotificationHandler handler;

  @Override
  protected void inject(DialogFragmentComponent component) {
    component.inject(this);
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    List<String> items =
        asList(
            getString(R.string.TAd_actionEditTask),
            getString(R.string.rmd_NoA_snooze),
            getString(R.string.rmd_NoA_done));

    handler = (NotificationHandler) getActivity();

    return dialogBuilder
        .newDialog()
        .setTitle(title)
        .setItems(
            items,
            (dialog, which) -> {
              switch (which) {
                case 0:
                  handler.edit();
                  break;
                case 1:
                  handler.snooze();
                  break;
                case 2:
                  handler.complete();
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

  public interface NotificationHandler {

    void edit();

    void snooze();

    void complete();

    void dismiss();
  }
}
