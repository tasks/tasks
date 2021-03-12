package org.tasks.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import org.tasks.R
import org.tasks.activities.attribution.AttributionViewModel.LibraryAttribution
import org.tasks.compose.Constants.KEYLINE_FIRST

object AttributionList {
    @Composable
    fun AttributionList(licenses: Map<String, Map<String, List<LibraryAttribution>>>) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            licenses.forEach { (license, libraries) ->
                Text(
                    license,
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier.padding(KEYLINE_FIRST)
                )
                libraries.forEach { (copyrightHolder, libraries) ->
                    LibraryCard(copyrightHolder, libraries)
                }
            }
        }
    }

    @Composable
    fun LibraryCard(copyrightHolder: String, libraries: List<LibraryAttribution>) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constants.HALF_KEYLINE),
            backgroundColor = colorResource(R.color.content_background),
        ) {
            Column(
                modifier = Modifier.padding(KEYLINE_FIRST)
            ) {
                Text(
                    copyrightHolder,
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.secondary
                )
                Spacer(Modifier.height(Constants.HALF_KEYLINE))
                libraries.forEach {
                    Text(
                        "\u2022 ${it.libraryName!!}",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onBackground
                    )
                }
            }
        }
    }
}