package org.tasks.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.todoroo.astrid.utility.Flags;

public class DraggableSwipeRefreshLayout extends SwipeRefreshLayout {

  public DraggableSwipeRefreshLayout(Context context) {
    super(context);
  }

  public DraggableSwipeRefreshLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    if (Flags.check(Flags.TLFP_NO_INTERCEPT_TOUCH)) {
      return false;
    }
    return super.onInterceptTouchEvent(ev);
  }
}
