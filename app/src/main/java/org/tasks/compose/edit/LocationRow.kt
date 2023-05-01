package org.tasks.compose.edit

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.material.composethemeadapter.MdcTheme
import org.tasks.R
import org.tasks.compose.DisabledText
import org.tasks.compose.TaskEditRow
import org.tasks.data.Geofence
import org.tasks.data.Location
import org.tasks.data.Place

@Composable
fun LocationRow(
    location: Location?,
    hasPermissions: Boolean,
    onClick: () -> Unit,
    openGeofenceOptions: () -> Unit,
) {
    TaskEditRow(
        iconRes = R.drawable.ic_outline_place_24px,
        content = {
            if (location == null) {
                DisabledText(
                    text = stringResource(id = R.string.add_location),
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            } else {
                Location(
                    name = location.displayName,
                    address = location.displayAddress,
                    openGeofenceOptions = openGeofenceOptions,
                    geofenceOn = hasPermissions && (location.isArrival || location.isDeparture)
                )
            }
        },
        onClick = onClick
    )
}

@Composable
fun Location(
    name: String,
    address: String?,
    geofenceOn: Boolean,
    openGeofenceOptions: () -> Unit,
) {
    Row {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 20.dp)
        ) {
            Text(text = name)
            address?.takeIf { it.isNotBlank() && it != name }?.let {
                Text(text = address)
            }
        }
        IconButton(
            onClick = openGeofenceOptions,
            modifier = Modifier.padding(top = 8.dp /* + 12dp from icon */)
        ) {
            Icon(
                imageVector = if (geofenceOn) {
                    Icons.Outlined.Notifications
                } else {
                    Icons.Outlined.NotificationsOff
                },
                contentDescription = null,
                modifier = Modifier.alpha(ContentAlpha.medium),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun NoLocation() {
    MdcTheme {
        LocationRow(
            location = null,
            hasPermissions = true,
            onClick = {},
            openGeofenceOptions = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun SampleLocation() {
    MdcTheme {
        LocationRow(
            location = Location(
                Geofence(),
                Place(
                    name = "Googleplex",
                    address = "1600 Amphitheatre Pkwy, Mountain View, CA 94043"
                ),
            ),
            hasPermissions = true,
            onClick = {},
            openGeofenceOptions = {},
        )
    }
}
