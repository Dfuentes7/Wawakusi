package com.example.wawakusi.data.api.response

class LoginResponse (
    val id: Int,
    val dni: String,
    val apellidoPaterno: String,
    val apellidoMaterno: String,
    val nombres: String,
    val celular: String,
    val sexo: String,
    val correo: String,
    var rpta: Boolean,
    var mensaje: String
)