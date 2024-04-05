package org.tasks.tags

/*
 * TagPickerActivityCompose is a replacement for TagPickerActivity reimplemented
 * using JetPack Compose framework.
 *
 * ViewModel is a bit modified but stays backward compatible with TagPickerActivity, so to
 * switch back to it just find lines like this
 *       //val intent = Intent(context, TagPickerActivity::class.java)
 *       val intent = Intent(context, TagPickerActivityCompose::class.java)
 * in TaskListFragment.kt and TagsControlSet.kt and move comment mark to another line.
 */

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.google.android.material.composethemeadapter.MdcTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.Strings
import org.tasks.billing.Inventory
import org.tasks.data.TagData
import org.tasks.injection.ThemedInjectingAppCompatActivity
import org.tasks.themes.ColorProvider
import org.tasks.themes.CustomIcons
import org.tasks.themes.Theme
import javax.inject.Inject

@AndroidEntryPoint
class TagPickerActivityCompose : ThemedInjectingAppCompatActivity() {
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
            val selected = intent.getParcelableArrayListExtra<TagData>(EXTRA_SELECTED)
            if ( selected != null ) {
                viewModel.setSelected(
                    selected, intent.getParcelableArrayListExtra<TagData>(EXTRA_PARTIALLY_SELECTED)
                )
            }
        }

        viewModel.search("")

        setContent {
            MdcTheme {
                TagPicker(
                    viewModel,
                    onBackClicked = { onBackPressed() },
                    getTagIcon = { tagData ->  getIcon(tagData) },
                    getTagColor = { tagData ->  getColor(tagData) }
                )
            } /* setContent */
        }
    } /* onCreate */

    override fun onBackPressed() {
        if (Strings.isNullOrEmpty(viewModel.pattern.value)) {
            val data = Intent()
            data.putExtra(EXTRA_TASKS, taskIds)
            data.putParcelableArrayListExtra(EXTRA_PARTIALLY_SELECTED, viewModel.getPartiallySelected())
            data.putParcelableArrayListExtra(EXTRA_SELECTED, viewModel.getSelected())
            setResult(Activity.RESULT_OK, data)
            finish()
        } else {
            viewModel.search("")
        }
    } /* onBackPressed */

    private fun getColor(tagData: TagData): Color {
        if (tagData.getColor() != 0) {
            val themeColor = colorProvider.getThemeColor(tagData.getColor()!!, true)
            if (inventory.purchasedThemes() || themeColor.isFree) {
                return Color(themeColor.primaryColor)
            }
        }
        return Color(getColor(R.color.icon_tint_with_alpha))
    }

    private fun getIcon(tagData: TagData): Int
    {
        val iconIndex = tagData.getIcon()
        var iconResource = R.drawable.ic_outline_label_24px
        if ( (iconIndex != null) && (iconIndex < 1000 || inventory.hasPro) ) {
            iconResource = CustomIcons.getIconResId(iconIndex) ?: R.drawable.ic_outline_label_24px
        }
        return iconResource
    }

    /* Copy of the TagPickerActivity's companion object */
    companion object {
        const val EXTRA_SELECTED = "extra_tags"
        const val EXTRA_PARTIALLY_SELECTED = "extra_partial"
        const val EXTRA_TASKS = "extra_tasks"
    }
}

@Composable
internal fun TagPicker(
    viewModel: TagPickerViewModel,
    onBackClicked: () -> Unit,
    getTagIcon: (TagData) -> Int,
    getTagColor: (TagData) -> Color
) {
    Box ( modifier = Modifier.fillMaxSize() )
    {
        Column (modifier = Modifier.padding(horizontal = 12.dp)) {
            Box( modifier = Modifier.fillMaxWidth() ) {
                SearchBar(viewModel, onBackClicked)
            }
            Box (
                modifier = Modifier.weight(1f)
            ) {
                PickerBox(viewModel, viewModel.tagsList.observeAsState(initial = emptyList()), getTagIcon, getTagColor)
            }
        }
    }
}

@Composable
internal fun SearchBar(
    viewModel: TagPickerViewModel,
    onBack: () -> Unit
) {
    val searchPattern = remember { viewModel.pattern }
    val invitation = LocalContext.current.getString(R.string.enter_tag_name)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            ImageVector.vectorResource(id = R.drawable.ic_outline_arrow_back_24px),
            "Done",
            modifier = Modifier
                .padding(6.dp)
                .clickable { onBack() }
        )

        TextField(
            value = searchPattern.value,
            onValueChange = { viewModel.search(it) },
            placeholder = { Text(invitation) },
            colors = TextFieldDefaults.textFieldColors(
                textColor = MaterialTheme.colors.onBackground,
                backgroundColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier.padding(start = 6.dp)
        )
    }
} /* SearchBar */

@Composable
internal fun PickerBox(
    viewModel: TagPickerViewModel,
    tags: State<List<TagData>>,
    getTagIcon: (TagData) -> Int = { R.drawable.ic_outline_label_24px },
    getTagColor: (TagData) -> Color = { Color.Gray }
) {
    val onClick: (TagData) -> Unit = {
        viewModel.viewModelScope.launch {
            viewModel.toggle(it, viewModel.getState(it) != ToggleableState.On) }
    }
    val newItem: (TagData) -> Unit = {
        viewModel.viewModelScope.launch { viewModel.createNew(it.name!!); viewModel.search("") }
    }

    LazyColumn {
        items( tags.value, key = { if (it.id == null) -1 else it.id!! } )
        {
            val checked = remember { mutableStateOf ( viewModel.getState(it) ) }
            Row(modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (it.id == null) newItem(it)
                    else {
                        onClick(it); checked.value = viewModel.getState(it)
                    }
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(getTagIcon(it)),
                    contentDescription = "",
                    modifier = Modifier.padding(6.dp),
                    tint = getTagColor(it)
                )
                if ( it.id == null ) {
                    val text = LocalContext.current.getString(R.string.new_tag) + " \"${it.name!!}\""
                    Text( text,
                          modifier = Modifier
                              .padding(horizontal = 24.dp)
                              .clickable { newItem(it) } )
                } else {
                    Text(it.name!!,
                         modifier = Modifier
                             .weight(1f)
                             .padding(horizontal = 24.dp)
                        )
                    TriStateCheckbox(
                        modifier = Modifier.padding(6.dp),
                        state = checked.value,
                        onClick = { onClick(it); checked.value = viewModel.getState(it) }
                    )
                }
            }
        }
    }
} /* PickerBox */

/*
internal fun genTestTags(): List<TagData>
{
    var idcc: Long = 1
    return "alpha beta gamma delta kappa theta alfa1 beta1 gamma1 delta1 kappa1 theta1"
        .split(" ")
        .map { name -> TagData(name).also{ it.id = idcc++ } }
}

@Composable
@Preview(showBackground = true, backgroundColor = 0xffffff)
internal fun PickerBoxPreview() {
    val list = remember { mutableStateOf( genTestTags() ) }
    PickerBox(list, getTagColor = { Color.Green })
}
*/
