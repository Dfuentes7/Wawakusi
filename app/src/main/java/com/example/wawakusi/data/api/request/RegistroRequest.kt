package com.example.wawakusi.data.api.request

data class RegistroRequest(
    val dni: String,
    val apellidoPaterno: String,
    val apellidoMaterno: String,
    val nombres: String,
    val celular: String,
    val sexo: String,
    val correo: String,
    val contrasena: String,
    val terminos: Boolean
)