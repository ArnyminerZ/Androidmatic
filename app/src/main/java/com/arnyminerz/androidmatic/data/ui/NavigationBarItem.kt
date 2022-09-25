package com.arnyminerz.androidmatic.data.ui

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

data class NavigationBarItem(
    val icon: ImageVector,
    @StringRes val label: Int?,
    @StringRes val contentDescription: Int? = label,
    val enabled: Boolean = true,
)
