package com.arnyminerz.androidmatic.data

import java.util.Date

data class StationFeedResult(
    val timestamp: Date,
    val stations: List<Station>,
)
