package com.arnyminerz.androidmatic.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arnyminerz.androidmatic.R

/**
 * A Chip used for indicating the sorting parameter of a list.
 * @author Arnau Mora
 * @since 20220923
 */
@Composable
@ExperimentalMaterial3Api
fun SortingChip(selected: Boolean, @StringRes sortByLabel: Int, enabled: Boolean = true, onSelected: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onSelected,
        enabled = enabled,
        label = {
            Text(stringResource(R.string.sort_by, stringResource(sortByLabel)))
        },
        leadingIcon = {
            if (selected)
                Icon(
                    Icons.Rounded.Check,
                    stringResource(R.string.image_desc_sorting_parameter, stringResource(sortByLabel))
                )
        },
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}
