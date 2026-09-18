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

    val onSaveClick: () -> Unit = {
        scope.launch {
            val lat = latitude.toDoubleOrNull()
            val lng = longitude.toDoubleOrNull()
            val updatedPlace = placeId?.toLongOrNull()?.let { id -> placeDao.getPlace(id) }?.let { place ->
                place.copy(
                    name = name,
                    latitude = lat ?: place.latitude,
                    longitude = lng ?: place.longitude,
                    color = color,
                    icon = icon,
                )
            }
            if (placeId == null) {
                placeDao.insert(
                    Place(
                        name = name,
                        latitude = lat ?: 0.0,
                        longitude = lng ?: 0.0,
                        color = color,
                        icon = icon,
                    )
                )
            } else if (updatedPlace != null) {
                placeDao.update(updatedPlace)
            }
            if (placeId == null || updatedPlace != null) {
                onSave(name, lat, lng)
                saveCompleted = true
            }
        }
    }

    PlaceDiscardDialog(
        show = showDiscardDialog,
        onDismiss = { showDiscardDialog = false },
        onDiscard = { showDiscardDialog = false; onBack() },
    )
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
    Scaffold(topBar = {
        PlaceSettingsTopBar(
            placeId = placeId,
            hasChanges = hasChanges,
            onBack = onBack,
            onShowDiscard = { showDiscardDialog = true },
            onSaveClick = onSaveClick,
        )
    }) { innerPadding ->
        PlaceSettingsContent(
            name = name,
            onNameChange = { name = it },
            latitude = latitude,
            onLatitudeChange = { latitude = it },
            longitude = longitude,
            onLongitudeChange = { longitude = it },
            color = color,
            onColorClick = { showColorPicker = true },
            icon = icon,
            onIconClick = { showIconPicker = true },
            placeId = placeId,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceDiscardDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onDiscard: () -> Unit,
) {
    if (!show) return
    BasicAlertDialog(
        onDismissRequest = onDismiss,
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
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
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = onDiscard) { Text("Discard") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceSettingsTopBar(
    placeId: String?,
    hasChanges: Boolean,
    onBack: () -> Unit,
    onShowDiscard: () -> Unit,
    onSaveClick: () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = {
                if (placeId != null && hasChanges) {
                    onShowDiscard()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceSettingsContent(
    name: String,
    onNameChange: (String) -> Unit,
    latitude: String,
    onLatitudeChange: (String) -> Unit,
    longitude: String,
    onLongitudeChange: (String) -> Unit,
    color: Int,
    onColorClick: () -> Unit,
    icon: String,
    onIconClick: () -> Unit,
    placeId: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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
            onValueChange = onNameChange,
            textStyle = TextStyle(fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onColorClick() }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                color = if (color == 0) MaterialTheme.colorScheme.primary else Color(color),
                shape = CircleShape,
            ) {}
            Spacer(modifier = Modifier.width(16.dp))
            Text("Color", style = MaterialTheme.typography.bodyLarge)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onIconClick() }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TasksIcon(label = icon, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text("Icon", style = MaterialTheme.typography.bodyLarge)
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
            onValueChange = onLatitudeChange,
            textStyle = TextStyle(fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
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
            onValueChange = onLongitudeChange,
            textStyle = TextStyle(fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )
        if (placeId != null) {
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { /* TODO: Delete place */ },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Delete Place") }
        }
    }
}
