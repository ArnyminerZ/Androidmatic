package com.arnyminerz.androidmatic.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.arnyminerz.androidmatic.data.ui.NavigationBarItem

@Composable
fun NavigationBar(
    selectedItem: Int,
    items: Collection<NavigationBarItem>,
    modifier: Modifier = Modifier,
    alwaysShowLabel: Boolean = true,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    tonalElevation: Dp = Dp.Unspecified,
    windowInsets: WindowInsets = NavigationBarDefaults.windowInsets,
    onChange: (index: Int) -> Unit = {},
) {
    androidx.compose.material3.NavigationBar(
        modifier, containerColor, contentColor, tonalElevation, windowInsets,
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedItem == index,
                onClick = { onChange.invoke(index) },
                label = { item.label?.let { Text(stringResource(it)) } },
                icon = { Icon(item.icon, item.contentDescription?.let { stringResource(it) }) },
                alwaysShowLabel = alwaysShowLabel,
                enabled = item.enabled,
            )
        }
    }
}
