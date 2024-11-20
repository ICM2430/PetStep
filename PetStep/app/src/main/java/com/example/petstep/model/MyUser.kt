package com.example.petstep.model

import com.google.android.material.color.ColorRoles

class MyUser {
    lateinit var nombre: String
    lateinit var apellido: String
    lateinit var telefono: String
    lateinit var correo: String
    lateinit var contrasena: String
    lateinit var rol: String

    constructor()

    constructor(
        nombre: String,
        apellido: String,
        telefono: String,
        correo: String,
        contrasena: String,
        rol: String
    ) {
        this.nombre = nombre
        this.apellido = apellido
        this.telefono = telefono
        this.correo = correo
        this.contrasena = contrasena
        this.rol = rol
    }

}