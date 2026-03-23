package com.example.wawakusi.data.api.request

import com.google.gson.annotations.SerializedName

data class UpdateMeRequest(
    @SerializedName("nombre")
    val nombre: String? = null,
    @SerializedName("telefono")
    val telefono: String? = null,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("direccion")
    val direccion: String? = null,
    @SerializedName("usuario")
    val usuario: String? = null,
    @SerializedName("password")
    val password: String? = null
)

