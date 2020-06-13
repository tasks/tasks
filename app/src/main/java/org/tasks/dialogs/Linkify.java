package org.tasks.dialogs;

import static java.util.Arrays.asList;
import static org.tasks.Strings.isNullOrEmpty;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.TextView;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.injection.ActivityContext;

public class Linkify {

  private final Context context;
  private final DialogBuilder dialogBuilder;

  @Inject
  public Linkify(@ActivityContext Context context, DialogBuilder dialogBuilder) {
    this.context = context;
    this.dialogBuilder = dialogBuilder;
  }

  public void linkify(TextView textView) {
    linkify(textView, () -> {});
  }

  public void linkify(TextView textView, Runnable onClick) {
    if (textView.length() == 0) {
      return;
    }

    android.text.util.Linkify.addLinks(textView, android.text.util.Linkify.ALL);

    textView.setOnClickListener(
        v -> {
          if (textView.getSelectionStart() == -1 && textView.getSelectionEnd() == -1) {
            onClick.run();
          }
        });

    URLSpan[] spans;
    Spannable spannable;
    CharSequence text = textView.getText();
    if (text instanceof SpannableStringBuilder || text instanceof SpannableString) {
      spannable = (Spannable) text;
    } else {
      return;
    }
    spans = spannable.getSpans(0, text.length(), URLSpan.class);
    for (URLSpan span : spans) {
      int start = spannable.getSpanStart(span);
      int end = spannable.getSpanEnd(span);

      spannable.removeSpan(span);
      spannable.setSpan(new ClickHandlingURLSpan(span.getURL(), onClick), start, end, 0);
    }
  }

  private class ClickHandlingURLSpan extends URLSpan {

    private final Runnable onEdit;

    ClickHandlingURLSpan(String url, Runnable onEdit) {
      super(url);
      this.onEdit = onEdit;
    }

    @Override
    public void onClick(View widget) {
      String title;
      String edit = context.getString(R.string.TAd_actionEditTask);
      String action;
      String url = getURL();
      Uri uri = Uri.parse(url);
      String scheme = uri.getScheme();
      if (isNullOrEmpty(scheme)) {
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
          .newDialog(title)
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
    }
  }
}
