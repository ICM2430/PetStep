package com.example.petstep.adapters.com.example.petstep.model


data class Request(
    val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",   // Nuevo campo
    val petName: String = "",     // Nuevo campo
    val petType: String = "",     // Nuevo campo
    val walkerId: String = "",
    val status: String = ""
)

