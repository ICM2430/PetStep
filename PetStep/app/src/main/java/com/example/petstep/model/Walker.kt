package com.example.petstep.com.example.petstep.model

data class Walker(
    var id: String = "",
    val nombre: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val available: Boolean = false,
    val precioPorHora: Double = 0.0,
    val distancia: Double = 0.0,
    val fotoPerfil: String = ""
)

