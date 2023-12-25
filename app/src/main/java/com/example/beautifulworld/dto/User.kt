package com.example.beautifulworld.dto

import com.yandex.mapkit.geometry.Point

class User(
    var name:String? = null,
    var phoneNumber:String? = null,
    var location: Point? = null,
    var token: String? = null,
    var linkImage: String? = null,
    )