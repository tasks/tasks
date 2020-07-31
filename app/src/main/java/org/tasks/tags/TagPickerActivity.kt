package org.tasks.tags

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnTextChanged
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.billing.Inventory
import org.tasks.data.TagData
import org.tasks.injection.ThemedInjectingAppCompatActivity
import org.tasks.themes.ColorProvider
import org.tasks.themes.Theme
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class TagPickerActivity : ThemedInjectingAppCompatActivity() {
    @BindView(R.id.toolbar)
    lateinit var toolbar: Toolbar
    @BindView(R.id.recycler_view)
    lateinit var recyclerView: RecyclerView
    @BindView(R.id.search_input)
    lateinit var editText: EditText

    @Inject lateinit var theme: Theme
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var colorProvider: ColorProvider

    private val viewModel: TagPickerViewModel by viewModels()
    private var taskIds: ArrayList<Long>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        taskIds = intent.getSerializableExtra(EXTRA_TASKS) as ArrayList<Long>?
        if (savedInstanceState == null) {
            viewModel.setSelected(
                    intent.getParcelableArrayListExtra(EXTRA_SELECTED),
                    intent.getParcelableArrayListExtra(EXTRA_PARTIALLY_SELECTED))
        }
        setContentView(R.layout.activity_tag_picker)
        ButterKnife.bind(this)
        toolbar.setNavigationIcon(R.drawable.ic_outline_arrow_back_24px)
        toolbar.setNavigationOnClickListener { onBackPressed() }
        val themeColor = theme.themeColor
        themeColor.applyToStatusBarIcons(this)
        themeColor.applyToNavigationBar(this)
        themeColor.apply(toolbar)
        val recyclerAdapter = TagRecyclerAdapter(this, viewModel, inventory, colorProvider) { tagData, vh ->
            onToggle(tagData, vh)
        }
        recyclerView.adapter = recyclerAdapter
        (recyclerView.itemAnimator as DefaultItemAnimator?)!!.supportsChangeAnimations = false
        recyclerView.layoutManager = LinearLayoutManager(this)
        viewModel.observe(this) { recyclerAdapter.submitList(it) }
        editText.setText(viewModel.text)
    }

    private fun onToggle(tagData: TagData, vh: TagPickerViewHolder) = lifecycleScope.launch {
        val newTag = tagData.id == null
        val newState = viewModel.toggle(tagData, vh.isChecked || newTag)
        vh.updateCheckbox(newState)
        if (newTag) {
            clear()
        }
    }

    @OnTextChanged(R.id.search_input)
    fun onSearch(text: CharSequence) {
        viewModel.search(text.toString())
    }

    override fun onBackPressed() {
        if (isNullOrEmpty(viewModel.text)) {
            val data = Intent()
            data.putExtra(EXTRA_TASKS, taskIds)
            data.putParcelableArrayListExtra(EXTRA_PARTIALLY_SELECTED, viewModel.getPartiallySelected())
            data.putParcelableArrayListExtra(EXTRA_SELECTED, viewModel.getSelected())
            setResult(Activity.RESULT_OK, data)
            finish()
        } else {
            clear()
        }
    }

    private fun clear() {
        editText.setText("")
    }

    companion object {
        const val EXTRA_SELECTED = "extra_tags"
        const val EXTRA_PARTIALLY_SELECTED = "extra_partial"
        const val EXTRA_TASKS = "extra_tasks"
    }
}