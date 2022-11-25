package org.tasks.reminders;

import static org.tasks.reminders.NotificationActivity.EXTRA_READ_ONLY;
import static java.util.Arrays.asList;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class NotificationDialog extends DialogFragment {

  @Inject DialogBuilder dialogBuilder;
  private String title;
  private NotificationHandler handler;

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    List<String> items =
        asList(
            getString(R.string.TAd_actionEditTask),
            getString(R.string.rmd_NoA_snooze),
            getString(R.string.rmd_NoA_done));

    handler = (NotificationHandler) getActivity();
    boolean readOnly = getArguments().getBoolean(EXTRA_READ_ONLY);
    return dialogBuilder
        .newDialog(title)
        .setItems(
            readOnly ? items.subList(0, 2) : items,
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
        .setNegativeButton(R.string.cancel, null)
        .show();
  }

  @Override
  public void onCancel(@NonNull DialogInterface dialog) {
    super.onCancel(dialog);

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
