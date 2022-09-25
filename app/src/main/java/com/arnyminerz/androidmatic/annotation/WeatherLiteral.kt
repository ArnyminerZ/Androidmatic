package com.arnyminerz.androidmatic.annotation

import androidx.annotation.StringDef

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
@StringDef(
    "sun", "moon", "hazesun", "suncloud", "rain", "fog", "hazemoon", "mist", "unknown"
)
annotation class WeatherLiteral
