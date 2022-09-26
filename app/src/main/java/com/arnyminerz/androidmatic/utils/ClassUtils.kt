package com.arnyminerz.androidmatic.utils

fun findClassForName(name: String) =
    Class.forName(
        name.replace("kotlin.String", "java.lang.String")
    )
