package org.tasks.compose.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.tasks.compose.settings.PickerColor
import org.tasks.data.dao.LocationDao
import org.tasks.data.entity.Place
import org.tasks.kmp.org.tasks.themes.ColorProvider
import org.tasks.themes.TasksIcons

class PlaceSettingsState {
    var name: String = ""
    var latitude: String = ""
    var longitude: String = ""
    var color: Int = 0
    var icon: String = TasksIcons.PLACE
    var showDiscardDialog: Boolean = false
    var showColorPicker: Boolean = false
    var showIconPicker: Boolean = false
    var saveCompleted: Boolean = false
    var originalName: String = ""
    var originalLatitude: String = ""
    var originalLongitude: String = ""
    var originalColor: Int = 0
    var originalIcon: String = TasksIcons.PLACE

    val hasChanges: Boolean
        get() = name != originalName || latitude != originalLatitude || longitude != originalLongitude ||
                color != originalColor || icon != originalIcon
}

@Composable
private fun rememberPlaceSettingsState(
    placeId: String?,
    placeDao: LocationDao,
): PlaceSettingsState {
    val state = remember { PlaceSettingsState() }

    LaunchedEffect(placeId) {
        placeId?.toLongOrNull()?.let { id ->
            val place = placeDao.getPlace(id)
            state.name = place?.name ?: ""
            state.latitude = place?.latitude?.toString() ?: ""
            state.longitude = place?.longitude?.toString() ?: ""
            state.color = place?.color ?: 0
            state.icon = place?.icon ?: TasksIcons.PLACE
            state.originalName = state.name
            state.originalLatitude = state.latitude
            state.originalLongitude = state.longitude
            state.originalColor = state.color
            state.originalIcon = state.icon
        }
    }

    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) {
            state.saveCompleted = false
        }
    }

    return state
}

@Composable
private fun rememberPlacePickerColors(): List<PickerColor> {
    return remember {
        ColorProvider.PRESET_COLORS.map { colorValue ->
            PickerColor(
                originalColor = colorValue,
                primaryColor = colorValue,
                colorOnPrimary = if (colorValue == 0) 0xFF000000.toInt() else 0xFFFFFFFF.toInt(),
                isFree = true,
            )
        }
    }
}

@Composable
private fun renderPlaceDialogs(
    state: PlaceSettingsState,
    onBack: () -> Unit,
    pickerColors: List<PickerColor>,
) {
    PlaceDialogsContent(
        params = PlaceDialogParams(
            showDiscardDialog = state.showDiscardDialog,
            onDismissDiscard = { state.showDiscardDialog = false },
            onDiscard = {
                state.showDiscardDialog = false
                onBack()
            },
            showColorPicker = state.showColorPicker,
            onDismissColorPicker = { state.showColorPicker = false },
            onColorSelected = { pickerColor ->
                state.color = pickerColor.originalColor
                state.showColorPicker = false
            },
            showIconPicker = state.showIconPicker,
            onDismissIconPicker = { state.showIconPicker = false },
            onIconSelected = { selectedIcon ->
                state.icon = selectedIcon ?: ""
                state.showIconPicker = false
            },
            pickerColors = pickerColors,
        )
    )
}

@Composable
private fun renderPlaceScaffold(
    placeId: String?,
    state: PlaceSettingsState,
    onBack: () -> Unit,
    onSaveClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            PlaceSettingsTopAppBar(
                placeId = placeId,
                hasChanges = state.hasChanges,
                onBack = {
                    if (placeId != null && state.hasChanges) {
                        state.showDiscardDialog = true
                    } else {
                        onBack()
                    }
                },
                onSaveClick = onSaveClick,
            )
        }
    ) { innerPadding ->
        PlaceSettingsContent(
            params = PlaceContentParams(
                innerPadding = innerPadding,
                name = state.name,
                onNameChange = { state.name = it },
                latitude = state.latitude,
                onLatitudeChange = { state.latitude = it },
                longitude = state.longitude,
                onLongitudeChange = { state.longitude = it },
                color = state.color,
                onColorClick = { state.showColorPicker = true },
                icon = state.icon,
                onIconClick = { state.showIconPicker = true },
                placeId = placeId,
                onDeleteClick = { /* TODO: Delete place */ },
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceSettingsScreen(
    placeId: String?,
    onBack: () -> Unit,
    onSave: (String, Double?, Double?) -> Unit,
) {
    val placeDao = koinInject<LocationDao>()
    val scope = rememberCoroutineScope()
    val state = rememberPlaceSettingsState(placeId, placeDao)
    val pickerColors = rememberPlacePickerColors()

    val onSaveClick = createPlaceOnSaveClick(
        scope = scope,
        params = PlaceSaveParams(
            filterDao = placeDao,
            filterId = placeId,
            name = state.name,
            latitude = state.latitude,
            longitude = state.longitude,
            color = state.color,
            icon = state.icon,
            onSave = onSave,
            onSaveCompleted = { state.saveCompleted = true },
        ),
    )

    renderPlaceDialogs(state, onBack, pickerColors)
    renderPlaceScaffold(placeId, state, onBack, onSaveClick)
}

private fun createPlaceOnSaveClick(
    scope: CoroutineScope,
    params: PlaceSaveParams,
): () -> Unit = {
    {
        scope.launch {
            val lat = params.latitude.toDoubleOrNull()
            val lng = params.longitude.toDoubleOrNull()
            val updatedPlace = params.filterId?.toLongOrNull()?.let { id -> params.filterDao.getPlace(id) }?.let { place ->
                place.copy(
                    name = params.name,
                    latitude = lat ?: place.latitude,
                    longitude = lng ?: place.longitude,
                    color = params.color,
                    icon = params.icon,
                )
            }
            if (params.filterId == null) {
                params.filterDao.insert(
                    Place(
                        name = params.name,
                        latitude = lat ?: 0.0,
                        longitude = lng ?: 0.0,
                        color = params.color,
                        icon = params.icon,
                    )
                )
            } else if (updatedPlace != null) {
                params.filterDao.update(updatedPlace)
            }
            if (params.filterId == null || updatedPlace != null) {
                params.onSave(params.name, lat, lng)
                params.onSaveCompleted()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
data class PlaceDialogParams(
    val showDiscardDialog: Boolean,
    val onDismissDiscard: () -> Unit,
    val onDiscard: () -> Unit,
    val showColorPicker: Boolean,
    val onDismissColorPicker: () -> Unit,
    val onColorSelected: (PickerColor) -> Unit,
    val showIconPicker: Boolean,
    val onDismissIconPicker: () -> Unit,
    val onIconSelected: (String?) -> Unit,
    val pickerColors: List<PickerColor>,
)

data class PlaceContentParams(
    val innerPadding: PaddingValues,
    val name: String,
    val onNameChange: (String) -> Unit,
    val latitude: String,
    val onLatitudeChange: (String) -> Unit,
    val longitude: String,
    val onLongitudeChange: (String) -> Unit,
    val color: Int,
    val onColorClick: () -> Unit,
    val icon: String,
    val onIconClick: () -> Unit,
    val placeId: String?,
    val onDeleteClick: () -> Unit,
)

data class PlaceSaveParams(
    val filterDao: LocationDao,
    val filterId: String?,
    val name: String,
    val latitude: String,
    val longitude: String,
    val color: Int,
    val icon: String,
    val onSave: (String, Double?, Double?) -> Unit,
    val onSaveCompleted: () -> Unit,
)
