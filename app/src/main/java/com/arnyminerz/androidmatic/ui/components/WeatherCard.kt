package com.arnyminerz.androidmatic.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.arnyminerz.androidmatic.R
import com.arnyminerz.androidmatic.annotation.WeatherLiteral
import com.arnyminerz.androidmatic.data.MaxValue
import com.arnyminerz.androidmatic.data.MinMaxValue
import com.arnyminerz.androidmatic.data.Station
import com.arnyminerz.androidmatic.data.StationSample
import com.arnyminerz.androidmatic.data.WeatherState
import com.arnyminerz.androidmatic.utils.shortTime
import java.util.Calendar
import java.util.Date

@Composable
fun WeatherCard(station: Station, weather: WeatherState?) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = station.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )

            if (weather != null) {
                val now = Calendar.getInstance()
                val diff = now.timeInMillis - weather.timestamp.time
                Text(
                    text = diff.shortTime(context),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        if (weather != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            ) {
                Image(
                    painter = painterResource(
                        when(weather.literalState) {
                            "sun" -> R.drawable.weather_sunny
                            "moon" -> R.drawable.weather_moon
                            "hazesun" -> R.drawable.weather_haze
                            "suncloud" -> R.drawable.weather_cloudy
                            "rain" -> R.drawable.weather_rain
                            "fog" -> R.drawable.weather_fog
                            "hazemoon" -> R.drawable.weather_haze_moon
                            else -> R.drawable.weather_unknown
                        }
                    ),
                    contentDescription = stringResource(R.string.weather_sunny),
                    modifier = Modifier
                        .size(96.dp)
                        .padding(horizontal = 8.dp),
                )
                Column(
                    modifier = Modifier
                        .weight(1f),
                ) {
                    val tempValues = weather.temperatureValues
                    val humValues = weather.humidityValues
                    val barValues = weather.barometerValues
                    val windValues = weather.windSpeedValues
                    IconText(
                        "${tempValues.value}ºC (${tempValues.min}ºC - ${tempValues.max}ºC)",
                        icon = Icons.Rounded.Thermostat,
                        contentDescription = stringResource(R.string.image_desc_temperature),
                        modifier = Modifier
                            .fillMaxWidth(),
                    )
                    IconText(
                        "${humValues.value}% (${humValues.min}% - ${humValues.max}%)",
                        icon = Icons.Rounded.WaterDrop,
                        contentDescription = stringResource(R.string.image_desc_humidity),
                        modifier = Modifier
                            .fillMaxWidth(),
                    )
                    IconText(
                        "${barValues.value}hPa (${humValues.min}hPa - ${humValues.max}hPa)",
                        icon = Icons.Rounded.Speed,
                        contentDescription = stringResource(R.string.image_desc_pressure),
                        modifier = Modifier
                            .fillMaxWidth(),
                    )
                    IconText(
                        "${windValues.value} kmph (${windValues.max} kmph)",
                        icon = Icons.Rounded.Air,
                        contentDescription = stringResource(R.string.image_desc_wind),
                        modifier = Modifier
                            .fillMaxWidth(),
                    )
                }
            }
        }
    }
}

class WeatherCardLiteralStateProvider: PreviewParameterProvider<@WeatherLiteral String> {
    override val values: Sequence<@WeatherLiteral String> =
        sequenceOf("sun", "hazesun", "suncloud", "rain", "fog", "moon", "hazemoon", "unknown")
}

@Preview
@Composable
fun WeatherCardPreview(
    @PreviewParameter(WeatherCardLiteralStateProvider::class) literalState: String,
) {
    WeatherCard(
        StationSample,
        WeatherState(
            Date(),
            MinMaxValue(
                24.1,
                30.0,
                20.0,
            ),
            MinMaxValue(
                60.0,
                58.9,
                75.0,
            ),
            MinMaxValue(
                1017.6,
                1010.5,
                1020.0,
            ),
            MaxValue(17.4, 50.0),
            30,
            0.0,
            literalState,
        )
    )
}
