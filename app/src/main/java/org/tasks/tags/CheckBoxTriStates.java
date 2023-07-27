package org.tasks.tags;

import static org.tasks.preferences.ResourceResolver.getData;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.widget.CompoundButton;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.core.content.res.ResourcesCompat;
import org.tasks.R;

public class CheckBoxTriStates extends AppCompatCheckBox {

  private int alpha;
  private State state;
  private OnCheckedChangeListener clientListener;

  private final OnCheckedChangeListener privateListener =
      new CompoundButton.OnCheckedChangeListener() {

        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
          switch (state) {
            case PARTIALLY_CHECKED:
            case UNCHECKED:
              setState(State.CHECKED, true);
              break;
            case CHECKED:
              setState(State.UNCHECKED, true);
              break;
          }
        }
      };

  public CheckBoxTriStates(Context context) {
    super(context);
    init();
  }

  public CheckBoxTriStates(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public CheckBoxTriStates(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  public void setState(State state, boolean notify) {
    if (this.state != state) {
      this.state = state;

      if (notify && this.clientListener != null) {
        this.clientListener.onCheckedChanged(this, this.isChecked());
      }
    }

    updateBtn();
  }

  @Override
  public void setOnCheckedChangeListener(@Nullable OnCheckedChangeListener listener) {
    if (this.privateListener != listener) {
      this.clientListener = listener;
    }

    super.setOnCheckedChangeListener(privateListener);
  }

  @Override
  public Parcelable onSaveInstanceState() {
    Parcelable superState = super.onSaveInstanceState();

    SavedState ss = new SavedState(superState);

    ss.state = state.ordinal();

    return ss;
  }

  @Override
  public void onRestoreInstanceState(Parcelable state) {
    SavedState ss = (SavedState) state;
    super.onRestoreInstanceState(ss.getSuperState());
    this.state = State.values()[ss.state];
    updateBtn();
    requestLayout();
  }

  private void init() {
    alpha = (int) (255 * ResourcesCompat.getFloat(getResources(), R.dimen.alpha_secondary));
    setState(State.UNCHECKED, false);
    setOnCheckedChangeListener(this.privateListener);
  }

  private void updateBtn() {
    int btnDrawable;
    int alpha = 255;
    switch (state) {
      case PARTIALLY_CHECKED:
        btnDrawable = R.drawable.ic_indeterminate_check_box_24px;
        break;
      case CHECKED:
        btnDrawable = R.drawable.ic_outline_check_box_24px;
        break;
      default:
        btnDrawable = R.drawable.ic_outline_check_box_outline_blank_24px;
        alpha = this.alpha;
        break;
    }
    Drawable original = getContext().getDrawable(btnDrawable);
    Drawable drawable;
    int color = state == State.UNCHECKED
        ? getContext().getColor(R.color.icon_tint)
        : getData(getContext(), androidx.appcompat.R.attr.colorAccent);
    drawable = original.mutate();
    drawable.setTint(color);
    drawable.setAlpha(alpha);
    setButtonDrawable(drawable);
  }

  public enum State {
    PARTIALLY_CHECKED,
    CHECKED,
    UNCHECKED
  }

  static class SavedState extends BaseSavedState {
    public static final Parcelable.Creator<SavedState> CREATOR =
        new Parcelable.Creator<>() {
          @Override
          public SavedState createFromParcel(Parcel in) {
            return new SavedState(in);
          }

          @Override
          public SavedState[] newArray(int size) {
            return new SavedState[size];
          }
        };

    int state;

    SavedState(Parcelable superState) {
      super(superState);
    }

    private SavedState(Parcel in) {
      super(in);
      state = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
      super.writeToParcel(out, flags);
      out.writeValue(state);
    }

    @Override
    public String toString() {
      return "CheckboxTriState.SavedState{"
          + Integer.toHexString(System.identityHashCode(this))
          + " state="
          + state
          + "}";
    }
  }
}
