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
    fun login(correo:String, contrasena:String){
        loginResponse = repository.login(LoginRequest(correo, contrasena))
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
        registroResponse = repository.registro(RegistroRequest(dni, apellidoPaterno, apellidoMaterno, nombres, celular, sexo, correo, contrasena, terminos))
    }

}