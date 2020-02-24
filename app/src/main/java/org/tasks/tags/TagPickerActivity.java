package org.tasks.tags;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnTextChanged;
import com.google.common.base.Strings;
import java.util.ArrayList;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.billing.Inventory;
import org.tasks.data.TagData;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.ThemedInjectingAppCompatActivity;
import org.tasks.tags.CheckBoxTriStates.State;
import org.tasks.themes.Theme;
import org.tasks.themes.ThemeCache;
import org.tasks.themes.ThemeColor;

public class TagPickerActivity extends ThemedInjectingAppCompatActivity {

  public static final String EXTRA_SELECTED = "extra_tags";
  public static final String EXTRA_PARTIALLY_SELECTED = "extra_partial";
  public static final String EXTRA_TASKS = "extra_tasks";

  @BindView(R.id.toolbar)
  Toolbar toolbar;

  @BindView(R.id.recycler_view)
  RecyclerView recyclerView;

  @BindView(R.id.search_input)
  EditText editText;

  @Inject Theme theme;
  @Inject ThemeCache themeCache;
  @Inject Inventory inventory;

  private TagPickerViewModel viewModel;
  private ArrayList<Long> taskIds;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent intent = getIntent();
    taskIds = (ArrayList<Long>) intent.getSerializableExtra(EXTRA_TASKS);
    if (savedInstanceState == null) {
      viewModel.setSelected(
          intent.getParcelableArrayListExtra(EXTRA_SELECTED),
          intent.getParcelableArrayListExtra(EXTRA_PARTIALLY_SELECTED));
    }

    setContentView(R.layout.activity_tag_picker);

    ButterKnife.bind(this);

    toolbar.setNavigationIcon(R.drawable.ic_outline_arrow_back_24px);
    toolbar.setNavigationOnClickListener(v -> onBackPressed());

    ThemeColor themeColor = theme.getThemeColor();
    themeColor.applyToStatusBarIcons(this);
    themeColor.applyToNavigationBar(this);
    themeColor.apply(toolbar);

    TagRecyclerAdapter recyclerAdapter =
        new TagRecyclerAdapter(this, viewModel, themeCache, inventory, this::onToggle);

    recyclerView.setAdapter(recyclerAdapter);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));

    viewModel.observe(this, recyclerAdapter::submitList);
    editText.setText(viewModel.getText());
  }

  private State onToggle(TagData tagData, Boolean checked) {
    boolean newTag = tagData.getId() == null;

    State state = viewModel.toggle(tagData, checked);

    if (newTag) {
      clear();
    }

    return state;
  }

  @OnTextChanged(R.id.search_input)
  void onSearch(CharSequence text) {
    viewModel.search(text.toString());
  }

  @Override
  public void onBackPressed() {
    if (Strings.isNullOrEmpty(viewModel.getText())) {
      Intent data = new Intent();
      data.putExtra(EXTRA_TASKS, taskIds);
      data.putParcelableArrayListExtra(EXTRA_PARTIALLY_SELECTED, viewModel.getPartiallySelected());
      data.putParcelableArrayListExtra(EXTRA_SELECTED, viewModel.getSelected());
      setResult(RESULT_OK, data);
      finish();
    } else {
      clear();
    }
  }

  private void clear() {
    editText.setText("");
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
    viewModel = ViewModelProviders.of(this).get(TagPickerViewModel.class);
    component.inject(viewModel);
  }
}
