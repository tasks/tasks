package org.tasks.dialogs;

import static java.util.Arrays.asList;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.TextView;
import com.google.common.base.Strings;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import javax.inject.Inject;
import me.saket.bettermovementmethod.BetterLinkMovementMethod;
import org.tasks.R;
import org.tasks.injection.ForActivity;

public class Linkify {

  private final Context context;
  private final DialogBuilder dialogBuilder;

  @Inject
  public Linkify(@ForActivity Context context, DialogBuilder dialogBuilder) {
    this.context = context;
    this.dialogBuilder = dialogBuilder;
  }

  public void linkify(TextView textView) {
    linkify(textView, () -> {}, () -> {});
  }

  public void linkify(TextView textView, Runnable onClick, Runnable onLongClick) {
    if (textView.length() == 0) {
      return;
    }

    BetterLinkMovementMethod.linkify(android.text.util.Linkify.ALL, textView)
        .setOnLinkClickListener((tv, url) -> handleLink(url, onClick))
        .setOnLinkLongClickListener(
            (tv, url) -> {
              onLongClick.run();
              return true;
            });
  }

  private boolean handleLink(String url, Runnable onEdit) {
    String title;
    String edit = context.getString(R.string.TAd_actionEditTask);
    String action;
    Uri uri = Uri.parse(url);
    String scheme = uri.getScheme();
    if (Strings.isNullOrEmpty(scheme)) {
      scheme = "";
    }
    switch (scheme) {
      case "tel":
        title = uri.getEncodedSchemeSpecificPart();
        action = context.getString(R.string.action_call);
        break;
      case "mailto":
        title = uri.getEncodedSchemeSpecificPart();
        action = context.getString(R.string.action_open);
        break;
      case "geo":
        title = uri.getEncodedQuery().replaceFirst("q=", "");
        try {
          title = URLDecoder.decode(title, "utf-8");
        } catch (UnsupportedEncodingException ignored) {
        }
        action = context.getString(R.string.action_open);
        break;
      default:
        title = url;
        action = context.getString(R.string.action_open);
        break;
    }
    dialogBuilder
        .newDialog()
        .setTitle(title)
        .setItems(
            asList(action, edit),
            (dialogInterface, selected) -> {
              if (selected == 0) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(uri);
                context.startActivity(intent);
              } else {
                onEdit.run();
              }
            })
        .show();
    return true;
  }
}
