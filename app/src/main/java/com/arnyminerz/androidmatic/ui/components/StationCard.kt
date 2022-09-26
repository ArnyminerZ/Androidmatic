package com.arnyminerz.androidmatic.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arnyminerz.androidmatic.data.Station
import com.arnyminerz.androidmatic.data.providers.model.Descriptor

@Composable
@ExperimentalMaterial3Api
fun StationCard(
    station: Station,
    checkboxEnabled: Boolean,
    checked: Boolean,
    modifier: Modifier = Modifier,
    showCheckbox: Boolean = true,
    onCheckedChange: (checked: Boolean) -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .padding(4.dp)
                    .weight(1f),
            ) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (station.descriptorSupports(Descriptor.Capability.LOCATION))
                    Text(
                        text = station.getValue<String>("location"),
                        style = MaterialTheme.typography.labelMedium,
                    )
            }
            if (showCheckbox)
                Checkbox(
                    checked = checked,
                    enabled = checkboxEnabled,
                    onCheckedChange = onCheckedChange,
                )
        }
    }
}
