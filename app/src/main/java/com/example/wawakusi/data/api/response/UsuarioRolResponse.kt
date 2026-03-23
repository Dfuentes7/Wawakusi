package com.example.wawakusi.data.api.response

data class UsuarioRolResponse(
    val idUsuario: Int,
    val usuario: String,
    val rolId: Int? = null,
    val rolNombre: String? = null,
    val estado: Int,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

