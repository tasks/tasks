package org.tasks.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import org.tasks.R;
import org.tasks.billing.Inventory;
import org.tasks.billing.PurchaseActivity;
import org.tasks.themes.CustomIcons;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class IconPickerDialog extends DialogFragment {

  private static final String EXTRA_CURRENT = "extra_current";

  @BindView(R.id.icons)
  RecyclerView recyclerView;

  @Inject DialogBuilder dialogBuilder;
  @Inject Activity context;
  @Inject Inventory inventory;
  private IconPickerCallback callback;

  public static IconPickerDialog newIconPicker(int currentIcon) {
    IconPickerDialog dialog = new IconPickerDialog();
    Bundle args = new Bundle();
    args.putInt(EXTRA_CURRENT, currentIcon);
    dialog.setArguments(args);
    return dialog;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    LayoutInflater inflater = LayoutInflater.from(context);
    View view = inflater.inflate(R.layout.dialog_icon_picker, null);

    ButterKnife.bind(this, view);

    Bundle arguments = getArguments();
    int current = arguments.getInt(EXTRA_CURRENT);

    IconPickerAdapter iconPickerAdapter =
        new IconPickerAdapter((Activity) context, inventory, current, this::onSelected);
    recyclerView.setLayoutManager(new IconLayoutManager(context));
    recyclerView.setAdapter(iconPickerAdapter);

    iconPickerAdapter.submitList(CustomIcons.getIconList());

    AlertDialogBuilder builder =
        dialogBuilder.newDialog().setNegativeButton(R.string.cancel, null).setView(view);
    if (!inventory.getHasPro()) {
      builder.setPositiveButton(
          R.string.upgrade_to_pro,
          (dialog, which) -> startActivity(new Intent(getContext(), PurchaseActivity.class))
      );
    }
    return builder.show();
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    callback = (IconPickerCallback) activity;
  }

  private void onSelected(int index) {
    callback.onSelected(getDialog(), index);
  }

  public interface IconPickerCallback {
    void onSelected(DialogInterface d, int icon);
  }
}
