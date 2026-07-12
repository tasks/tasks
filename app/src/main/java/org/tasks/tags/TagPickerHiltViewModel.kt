package org.tasks.tags

import androidx.compose.ui.graphics.Color
import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.billing.Inventory
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.TagData
import org.tasks.filters.TagFilter
import org.tasks.filters.getIcon
import org.tasks.sync.SyncAdapters
import org.tasks.themes.ColorProvider
import org.tasks.themes.TasksIcons
import javax.inject.Inject

@HiltViewModel
class TagPickerHiltViewModel @Inject constructor(
    tagDataDao: TagDataDao,
    syncAdapters: SyncAdapters,
    private val inventory: Inventory,
    private val colorProvider: ColorProvider,
) : TagPickerViewModel(tagDataDao, syncAdapters) {

    fun getColor(tagData: TagData): Color {
        if ((tagData.color ?: 0) != 0) {
            val themeColor = colorProvider.getThemeColor(tagData.color ?: 0, true)
            if (inventory.purchasedThemes() || themeColor.isFree) {
                return Color(themeColor.primaryColor)
            }
        }
        return Color.Unspecified
    }

    fun getIcon(tagData: TagData): String =
        TagFilter(tagData).getIcon(inventory) ?: TasksIcons.LABEL
}
