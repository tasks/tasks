package org.tasks.ui;

import static com.todoroo.andlib.utility.AndroidUtilities.preLollipop;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.view.View;
import android.view.ViewAnimationUtils;

public class AnimationUtil {

  @SuppressLint("NewApi")
  public static void circularReveal(View v) {
    if (v.getVisibility() == View.VISIBLE) {
      return;
    }
    if (preLollipop()) {
      v.setVisibility(View.VISIBLE);
    } else {
      int cx = v.getMeasuredWidth() / 2;
      int cy = v.getMeasuredHeight() / 2;
      int finalRadius = Math.max(v.getWidth(), v.getHeight()) / 2;

      Animator anim = ViewAnimationUtils.createCircularReveal(v, cx, cy, 0, finalRadius);
      v.setVisibility(View.VISIBLE);
      anim.start();
    }
  }
}
