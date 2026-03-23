package com.example.wawakusi.data.api.response

data class LoginResponse(
    val rpta: Boolean,
    val mensaje: String,
    val token: String? = null,
    val auth: AuthResponse? = null
)

data class AuthResponse(
    val idUsuario: Int,
    val usuario: String,
    val rolId: Int? = null,
    val rolNombre: String? = null,
    val permisos: List<String>? = emptyList()
)

data class VistasResponse(
    val rpta: Boolean,
    val publico: Boolean,
    val vistas: List<VistaItem> = emptyList(),
    val rol: String? = null,
    val permisos: List<String> = emptyList()
)

data class VistaItem(
    val codigo: String,
    val nombre: String
)
