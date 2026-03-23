package com.example.wawakusi.data.api.response

data class MeResponse(
    val rpta: Boolean,
    val usuario: MeUsuario? = null,
    val cliente: MeCliente? = null,
    val permisos: List<String> = emptyList(),
    val mensaje: String? = null
)

data class MeUsuario(
    val idUsuario: Int,
    val usuario: String,
    val rolId: Int? = null,
    val rolNombre: String? = null,
    val estado: Int? = null
)

data class MeCliente(
    val id: Int,
    val nombre: String? = null,
    val telefono: String? = null,
    val email: String? = null,
    val direccion: String? = null
)

data class UpdateMeResponse(
    val rpta: Boolean,
    val mensaje: String
)

