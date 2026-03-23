package com.example.wawakusi.data.api.request

data class CrearUsuarioAdminRequest(
    val usuario: String,
    val password: String,
    val rolId: Int,
    val estado: Int = 1
)

data class ActualizarUsuarioAdminRequest(
    val usuario: String? = null,
    val password: String? = null,
    val rolId: Int? = null,
    val estado: Int? = null
)

