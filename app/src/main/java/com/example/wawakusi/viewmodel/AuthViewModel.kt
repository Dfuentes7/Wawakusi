package com.example.wawakusi.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.wawakusi.data.api.request.LoginRequest
import com.example.wawakusi.data.api.request.RegistroRequest
import com.example.wawakusi.data.api.response.LoginResponse
import com.example.wawakusi.data.api.response.RegistroResponse
import com.example.wawakusi.repository.AuthRepository

class AuthViewModel : ViewModel() {
    var loginResponse: LiveData<LoginResponse>
    var registroResponse: LiveData<RegistroResponse>
    private var repository = AuthRepository()
    init {
        loginResponse = repository.loginResponse
        registroResponse = repository.registroResponse

    }
    fun login(correo: String, contrasena: String) {
        loginResponse = repository.login(
            LoginRequest(
                email = correo.trim(),
                password = contrasena
            )
        )
    }

    fun registro(dni: String,
                 apellidoPaterno: String,
                 apellidoMaterno: String,
                 nombres: String,
                 celular: String,
                 sexo: String,
                 correo: String,
                 contrasena: String,
                 terminos: Boolean){
        val nombreCompleto = listOf(nombres, apellidoPaterno, apellidoMaterno)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(" ")

        registroResponse = repository.registro(
            RegistroRequest(
                nombre = nombreCompleto,
                telefono = celular.trim().ifBlank { null },
                email = correo.trim(),
                direccion = null,
                usuario = correo.trim(),
                password = contrasena
            )
        )
    }

}
