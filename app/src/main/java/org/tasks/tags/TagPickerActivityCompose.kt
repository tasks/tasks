package org.tasks.tags

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TriStateCheckbox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.material.search.SearchBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.Strings
import org.tasks.data.TagData
import org.tasks.data.TagDataDao
import org.tasks.data.TagDataDao_Impl
import org.tasks.injection.ThemedInjectingAppCompatActivity
import java.util.ArrayList
import javax.inject.Inject

@AndroidEntryPoint
class TagPickerActivityCompose : ThemedInjectingAppCompatActivity() {

    private val viewModel: TagPickerViewModel by viewModels()
    private var taskIds: ArrayList<Long>? = null
    private val searchPattern = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = intent
        taskIds = intent.getSerializableExtra(TagPickerActivity.EXTRA_TASKS) as ArrayList<Long>?
        if (savedInstanceState == null) {
            intent.getParcelableArrayListExtra<TagData>(TagPickerActivity.EXTRA_SELECTED)?.let {
                viewModel.setSelected(
                    it,
                    intent.getParcelableArrayListExtra(TagPickerActivity.EXTRA_PARTIALLY_SELECTED)
                )
            }
        }

        viewModel.search(searchPattern.value)

        setContent {
            TagPicker(
                searchPattern,
                viewModel.tagsList.observeAsState(initial = emptyList()),
                onTextChange = { newText -> viewModel.search(newText); searchPattern.value = newText },
                onBackClicked = { onBackPressed() },
                checkedState = {
                    when(viewModel.getState(it)) {
                        CheckBoxTriStates.State.CHECKED -> ToggleableState.On
                        CheckBoxTriStates.State.PARTIALLY_CHECKED -> ToggleableState.Indeterminate
                        else -> ToggleableState.Off
                    }
                },
                onTagClicked = { onToggle(it, viewModel.getState(it) != CheckBoxTriStates.State.CHECKED) },
                createTag = {  onNewTag(it.name!!); searchPattern.value = "" }
            )
        }
    } /* onCreate */

    private fun onToggle(tag: TagData, checked: Boolean) =
        viewModel.viewModelScope.launch { viewModel.toggle(tag, checked) }

    private fun onNewTag(name: String) =
        viewModel.viewModelScope.launch { viewModel.createNew(name) }

    override fun onBackPressed() {
        if (Strings.isNullOrEmpty(viewModel.text)) {
            val data = Intent()
            data.putExtra(TagPickerActivity.EXTRA_TASKS, taskIds)
            data.putParcelableArrayListExtra(TagPickerActivity.EXTRA_PARTIALLY_SELECTED, viewModel.getPartiallySelected())
            data.putParcelableArrayListExtra(TagPickerActivity.EXTRA_SELECTED, viewModel.getSelected())
            setResult(Activity.RESULT_OK, data)
            finish()
        } else {
            clear()
        }
    } /* onBackPressed */

    private fun clear() {
        searchPattern.value = ""
        viewModel.search(searchPattern.value)
    }

    companion object {
        const val EXTRA_SELECTED = "extra_tags"
        const val EXTRA_PARTIALLY_SELECTED = "extra_partial"
        const val EXTRA_TASKS = "extra_tasks"
    }

}

@Composable
internal fun TagPicker(
    searchPattern: MutableState<String>,
    tagsList: State<List<TagData>>,         /* tags selected in accordance to searchText */
    onTextChange: (String) -> Unit = {},
    onBackClicked: () -> Unit,
    checkedState: (TagData) -> ToggleableState = { ToggleableState.Off },
    onTagClicked: (TagData) -> Unit,
    createTag: (TagData) -> Unit
) {
    Box ( modifier = Modifier.fillMaxSize() )
    {
        Column {
            Box( modifier = Modifier.fillMaxWidth() ) {
                SearchBar(searchPattern, onTextChange, onBackClicked)
            }
            Box ( modifier = Modifier.weight(1f)) {
                PickerBox(tagsList, checkedState, onTagClicked, createTag)
            }
        }
    }
}

@Composable
internal fun SearchBar(
    text: MutableState<String>,
    onTextChange: (String) -> Unit,
    onBack: () -> Unit
) {
    Row () {
        IconButton(onClick = onBack) {
           Icon(Icons.Default.ArrowBack, "Done")
        }
        TextField(value = text.value, onValueChange = { onTextChange(it); text.value = it })
    }
}

@Composable
internal fun PickerBox(
    tags: State<List<TagData>>,
    getState: (TagData) -> ToggleableState = { ToggleableState.Off },
    onClick: (TagData) -> Unit = {},
    newItem: (TagData) -> Unit = {}
) {
    LazyColumn (
        modifier = Modifier.padding(horizontal = 10.dp)
    ) {
        items(
            tags.value,
            key = { if (it.id == null) -1 else it.id!! }
        ) {
            val checked = remember { mutableStateOf ( getState(it) ) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if ( it.id == null ) {
                    Text( it.name!!, modifier = Modifier.clickable { newItem(it) } )
                    Text("Create new", modifier = Modifier.clickable { newItem(it) } )
                } else {
                    Text(it.name!!)
                    TriStateCheckbox(
                        state = checked.value,
                        onClick = {
                            onClick(it)
                            checked.value = getState(it)
                        }
                    )
                }
            }
        }
    }
} /* PickerBox */

internal fun genTestTags(): List<TagData>
{
    var idcc: Long = 1;
    val tagnames = "alfa beta gamma delta kappa theta alfa1 beta1 gamma1 delta1 kappa1 theta1"
    val list = tagnames.split(" ")
    val res = list.map { name -> TagData(name).also{ it.id = idcc++  } }
    return res
}

@Composable
@Preview(showBackground = true, backgroundColor = 0xffffff)
internal fun PickerBoxPreview() {
    val list = remember { mutableStateOf( genTestTags() ) }
    PickerBox(list)
}