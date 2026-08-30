/**
 * EmptyCard.kt — Placeholder card used for visual padding/spacing.
 *
 * Renders an empty, non-interactive card. Useful at the bottom of a
 * [ScalingLazyColumn] to ensure the last real item can scroll fully
 * into view on round Wear screens.
 */

package org.tasks.presentation.components

import androidx.compose.runtime.Composable

@Composable
fun EmptyCard() = Card(onClick = {}) { }
