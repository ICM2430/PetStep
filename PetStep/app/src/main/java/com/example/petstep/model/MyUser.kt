package com.example.petstep.model

import kotlin.properties.Delegates

class MyUser {
    lateinit var id: String
    lateinit var nombre: String
    lateinit var apellido: String
    lateinit var telefono: String
    lateinit var correo: String
    lateinit var contrasena: String
    lateinit var rol: String
    lateinit var image: String
    var calificacion by Delegates.notNull<Int>()

    constructor()

    constructor(
        id: String,
        nombre: String,
        apellido: String,
        telefono: String,
        correo: String,
        contrasena: String,
        rol: String,
    ) {
        this.id = id
        this.nombre = nombre
        this.apellido = apellido
        this.telefono = telefono
        this.correo = correo
        this.contrasena = contrasena
        this.rol = rol
        this.image = ""
        this.calificacion = 0 // Initialize calificacion
    }

    // Modificar la calificacion
    fun setCalificacion(calificacion: Int) {
        this.calificacion = calificacion
    }

    fun getCalificacion(): Int {
        return this.calificacion
    }
}