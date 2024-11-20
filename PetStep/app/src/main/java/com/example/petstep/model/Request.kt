package com.example.petstep.adapters.com.example.petstep.model


data class Request(
    var id: String = "",
    val distance: Double = 0.0,  // Asegurarnos que es Double
    val duration: Int = 0,
    val ownerLat: Double = 0.0,
    val ownerLng: Double = 0.0,
    val petId: String = "",
    val status: String = "",
    val timestamp: Long = 0,
    val userId: String = "",
    val walkerId: String = "",
    val price: Double = 0.0  // Añadido price
)
