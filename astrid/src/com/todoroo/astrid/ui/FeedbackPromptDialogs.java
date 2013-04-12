package com.todoroo.astrid.ui;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.astrid.activity.AstridActivity;

public class FeedbackPromptDialogs {

    @SuppressWarnings("nls")
    public static void showFeedbackDialog(final AstridActivity activity, boolean positive) {
        final AstridDialog d = new AstridDialog(activity, false);

        int titleRes = positive ? R.string.feedback_positive_title : R.string.feedback_negative_title;
        int bodyRes = positive ? R.string.feedback_positive_body : R.string.feedback_negative_body;
        int buttonRes = positive ? R.string.feedback_positive_button : R.string.feedback_negative_button;

        final String url = positive ? "https://play.google.com/store/apps/details?id=com.timsu.astrid&write_review=true" : "http://weloveastrid.com/problem_astrid_android.html";

        d.setAstridTitle(titleRes)
        .setAstridText(bodyRes)
        .setButtonText(buttonRes, 0)
        .setButtonText(R.string.feedback_not_now, 1)
        .setButtonListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(url));
                try {
                    activity.startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(activity, R.string.feedback_activity_error, Toast.LENGTH_LONG).show();
                }
                d.dismiss();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });

        d.show();
    }

}
