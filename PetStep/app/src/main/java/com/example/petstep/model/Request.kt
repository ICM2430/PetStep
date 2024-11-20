package com.example.petstep.adapters.com.example.petstep.model


data class Request(
    var id: String = "",  // Cambiado de val a var para permitir reasignación
    val ownerId: String = "",
    val ownerName: String = "",   // Nuevo campo
    val petName: String = "",     // Nuevo campo
    val petType: String = "",     // Nuevo campo
    val walkerId: String = "",
    val status: String = "",
    val timestamp: Long = 0
)

