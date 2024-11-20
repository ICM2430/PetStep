package com.example.petstep.com.example.petstep.model

data class Walker(
    var id: String = "",
    var available: Boolean = false,
    var precioPorHora: Int = 0,
    var workZone: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var nombre: String = "",
    var apellido: String = "",
    var profilePhotoUrl: String = "",
    var distancia: Double = 0.0
)

