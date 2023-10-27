package org.tasks.tags

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.activity.viewModels
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.billing.Inventory
import org.tasks.data.TagData
import org.tasks.databinding.ActivityTagPickerBinding
import org.tasks.extensions.addBackPressedCallback
import org.tasks.injection.ThemedInjectingAppCompatActivity
import org.tasks.themes.ColorProvider
import org.tasks.themes.Theme
import javax.inject.Inject

@AndroidEntryPoint
class TagPickerActivity : ThemedInjectingAppCompatActivity() {
    @Inject lateinit var theme: Theme
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var colorProvider: ColorProvider

    private val viewModel: TagPickerViewModel by viewModels()
    private var taskIds: ArrayList<Long>? = null
    private lateinit var editText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        taskIds = intent.getSerializableExtra(EXTRA_TASKS) as ArrayList<Long>?
        if (savedInstanceState == null) {
            intent.getParcelableArrayListExtra<TagData>(EXTRA_SELECTED)?.let {
                viewModel.setSelected(
                        it,
                        intent.getParcelableArrayListExtra(EXTRA_PARTIALLY_SELECTED)
                )
            }
        }
        val binding = ActivityTagPickerBinding.inflate(layoutInflater)
        editText = binding.searchInput.apply {
            addTextChangedListener(
                onTextChanged = { text, _, _, _ -> onSearch(text) }
            )
        }
        setContentView(binding.root)
        val toolbar = binding.toolbar
        toolbar.setNavigationIcon(R.drawable.ic_outline_arrow_back_24px)
        toolbar.setNavigationOnClickListener { onBackPressed() }
        val themeColor = theme.themeColor
        themeColor.applyToNavigationBar(this)
        val recyclerAdapter = TagRecyclerAdapter(this, viewModel, inventory, colorProvider) { tagData, vh ->
            onToggle(tagData, vh)
        }
        val recyclerView = binding.recyclerView
        recyclerView.adapter = recyclerAdapter
        (recyclerView.itemAnimator as DefaultItemAnimator?)!!.supportsChangeAnimations = false
        recyclerView.layoutManager = LinearLayoutManager(this)
        viewModel.observe(this) { recyclerAdapter.submitList(it) }
        editText.setText(viewModel.text)

        addBackPressedCallback {
            if (isNullOrEmpty(viewModel.text)) {
                val data = Intent()
                data.putExtra(EXTRA_TASKS, taskIds)
                data.putParcelableArrayListExtra(
                    EXTRA_PARTIALLY_SELECTED,
                    viewModel.getPartiallySelected()
                )
                data.putParcelableArrayListExtra(EXTRA_SELECTED, viewModel.getSelected())
                setResult(Activity.RESULT_OK, data)
                finish()
            } else {
                clear()
            }
        }
    }

    private fun onToggle(tagData: TagData, vh: TagPickerViewHolder) = lifecycleScope.launch {
        val newTag = tagData.id == null
        val newState = viewModel.toggle(tagData, vh.isChecked || newTag)
        vh.updateCheckbox(newState)
        if (newTag) {
            clear()
        }
    }

    private fun onSearch(text: CharSequence?) {
        viewModel.search(text?.toString() ?: "")
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