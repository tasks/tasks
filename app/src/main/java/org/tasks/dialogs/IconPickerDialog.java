package org.tasks.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import org.tasks.R;
import org.tasks.billing.Inventory;
import org.tasks.billing.PurchaseActivity;
import org.tasks.databinding.DialogIconPickerBinding;
import org.tasks.themes.CustomIcons;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class IconPickerDialog extends DialogFragment {

  private static final String EXTRA_CURRENT = "extra_current";

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
    DialogIconPickerBinding binding = DialogIconPickerBinding.inflate(LayoutInflater.from(context));
    Bundle arguments = getArguments();
    int current = arguments.getInt(EXTRA_CURRENT);

    IconPickerAdapter iconPickerAdapter =
        new IconPickerAdapter(context, inventory, current, this::onSelected);
    RecyclerView recyclerView = binding.icons;
    recyclerView.setLayoutManager(new IconLayoutManager(context));
    recyclerView.setAdapter(iconPickerAdapter);

    iconPickerAdapter.submitList(CustomIcons.getIconList());

    AlertDialogBuilder builder =
        dialogBuilder
                .newDialog()
                .setNegativeButton(R.string.cancel, null)
                .setView(binding.getRoot());
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
