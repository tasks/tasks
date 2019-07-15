package org.tasks.dialogs;

import static org.tasks.billing.PurchaseDialog.newPurchaseDialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.billing.Inventory;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.ForActivity;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.themes.CustomIcons;

public class IconPickerDialog extends InjectingDialogFragment {

  private static final String FRAG_TAG_PURCHASE = "frag_tag_purchase";
  private static final String EXTRA_CURRENT = "extra_current";

  @BindView(R.id.icons)
  RecyclerView recyclerView;

  @Inject DialogBuilder dialogBuilder;
  @Inject @ForActivity Context context;
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
        dialogBuilder.newDialog().setNegativeButton(android.R.string.cancel, null).setView(view);
    if (!inventory.hasPro()) {
      builder.setPositiveButton(
          R.string.button_subscribe,
          (dialog, which) -> newPurchaseDialog().show(getFragmentManager(), FRAG_TAG_PURCHASE));
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

  @Override
  protected void inject(DialogFragmentComponent component) {
    component.inject(this);
  }

  public interface IconPickerCallback {
    void onSelected(DialogInterface d, int icon);
  }
}
