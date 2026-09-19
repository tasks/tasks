package org.tasks.compose.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.tasks.compose.pickers.Icon
import org.tasks.compose.pickers.IconPickerDialog
import org.tasks.compose.settings.ColorPickerDialog
import org.tasks.compose.settings.PickerColor
import org.tasks.data.dao.LocationDao
import org.tasks.data.entity.Place
import org.tasks.kmp.org.tasks.themes.ColorProvider
import org.tasks.themes.TasksIcons
import org.tasks.compose.components.TasksIcon
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.back
import tasks.kmp.generated.resources.settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceSettingsScreen(
    placeId: String?,
    onBack: () -> Unit,
    onSave: (String, Double?, Double?) -> Unit,
) {
    val placeDao = koinInject<LocationDao>()
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(0) }
    var icon by remember { mutableStateOf(TasksIcons.PLACE) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var saveCompleted by remember { mutableStateOf(false) }
    var originalName by remember { mutableStateOf("") }
    var originalLatitude by remember { mutableStateOf("") }
    var originalLongitude by remember { mutableStateOf("") }
    var originalColor by remember { mutableStateOf(0) }
    var originalIcon by remember { mutableStateOf(TasksIcons.PLACE) }

    val pickerColors = remember {
        ColorProvider.PRESET_COLORS.map { colorValue ->
            PickerColor(
                originalColor = colorValue,
                primaryColor = colorValue,
                colorOnPrimary = if (colorValue == 0) 0xFF000000.toInt() else 0xFFFFFFFF.toInt(),
                isFree = true,
            )
        }
    }

    LaunchedEffect(placeId) {
        placeId?.toLongOrNull()?.let { id ->
            val place = placeDao.getPlace(id)
            name = place?.name ?: ""
            latitude = place?.latitude?.toString() ?: ""
            longitude = place?.longitude?.toString() ?: ""
            color = place?.color ?: 0
            icon = place?.icon ?: TasksIcons.PLACE
            originalName = name
            originalLatitude = latitude
            originalLongitude = longitude
            originalColor = color
            originalIcon = icon
        }
    }

    val hasChanges = name != originalName ||
            latitude != originalLatitude ||
            longitude != originalLongitude ||
            color != originalColor ||
            icon != originalIcon

    LaunchedEffect(saveCompleted) {
        if (saveCompleted) {
            saveCompleted = false
            onBack()
        }
    }

    val onSaveClick = createPlaceOnSaveClick(
        scope = scope,
        params = PlaceSaveParams(
            filterDao = placeDao,
            filterId = placeId,
            name = name,
            latitude = latitude,
            longitude = longitude,
            color = color,
            icon = icon,
            onSave = onSave,
            onSaveCompleted = { saveCompleted = true },
        ),
    )

    if (showDiscardDialog) {
        BasicAlertDialog(
            onDismissRequest = { showDiscardDialog = false },
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Discard changes?",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "You have unsaved changes. Are you sure you want to discard them?",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { showDiscardDialog = false }) {
                            Text("Cancel")
                        }
                        TextButton(onClick = {
                            showDiscardDialog = false
                            onBack()
                        }) {
                            Text("Discard")
                        }
                    }
                }
            }
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            hasPro = true,
            colors = pickerColors,
            onDismiss = { showColorPicker = false },
            onColorSelected = { pickerColor ->
                color = pickerColor.originalColor
                showColorPicker = false
            },
        )
    }

    if (showIconPicker) {
        IconPickerDialog(
            selectedIcon = null,
            onIconSelected = { selectedIcon ->
                icon = selectedIcon ?: ""
                showIconPicker = false
            },
            onDismissRequest = { showIconPicker = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        if (placeId != null && hasChanges) {
                            showDiscardDialog = true
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back)
                        )
                    }
                },
                title = { Text(if (placeId == null) "New Place" else stringResource(Res.string.settings)) },
                actions = {
                    TextButton(onClick = onSaveClick) { Text("Save") }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Place Name",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            BasicTextField(
                value = name,
                onValueChange = { name = it },
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))

            // Color Picker Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showColorPicker = true }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    color = if (color == 0) MaterialTheme.colorScheme.primary else Color(color),
                    shape = CircleShape,
                ) {}
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Icon Picker Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showIconPicker = true }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TasksIcon(
                    label = icon,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Icon",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Latitude",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            BasicTextField(
                value = latitude,
                onValueChange = { latitude = it },
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))

            Text(
                text = "Longitude",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            BasicTextField(
                value = longitude,
                onValueChange = { longitude = it },
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            if (placeId != null) {
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { /* TODO: Delete place */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete Place")
                }
            }
        }
    }
}

private fun createPlaceOnSaveClick(
    scope: CoroutineScope,
    params: PlaceSaveParams,
): () -> Unit = {
    {
        scope.launch {
            val lat = latitude.toDoubleOrNull()
            val lng = longitude.toDoubleOrNull()
            val updatedPlace = params.filterId?.toLongOrNull()?.let { id -> params.filterDao.getPlace(id) }?.let { place ->
                place.copy(
                    name = name,
                    latitude = lat ?: place.latitude,
                    longitude = lng ?: place.longitude,
                    color = color,
                    icon = icon,
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
            if (filterId == null || updatedPlace != null) {
                params.onSave(name, lat, lng)
                params.onSaveCompleted()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceSettingsDialogs(
    showDiscardDialog: Boolean,
    onDismissDiscard: () -> Unit,
    onDiscard: () -> Unit,
    showColorPicker: Boolean,
    onDismissColorPicker: () -> Unit,
    onColorSelected: (PickerColor) -> Unit,
    showIconPicker: Boolean,
    onDismissIconPicker: () -> Unit,
    onIconSelected: (String) -> Unit,
    pickerColors: List<PickerColor>,
) {
    PlaceDiscardDialog(
        show = showDiscardDialog,
        onDismiss = onDismissDiscard,
        onDiscard = onDiscard,
    )
    if (showColorPicker) {
        ColorPickerDialog(
            hasPro = true,
            colors = pickerColors,
            onDismiss = onDismissColorPicker,
            onColorSelected = onColorSelected,
        )
    }
    if (showIconPicker) {
        IconPickerDialog(
            selectedIcon = null,
            onIconSelected = onIconSelected,
            onDismissRequest = onDismissIconPicker,
        )
    }
}

data class PlaceSettingsContentState(
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
