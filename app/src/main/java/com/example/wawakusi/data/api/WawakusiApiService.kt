package com.example.wawakusi.data.api

import com.example.wawakusi.data.api.request.LoginRequest
import com.example.wawakusi.data.api.request.RegistroRequest
import com.example.wawakusi.data.api.response.LoginResponse
import com.example.wawakusi.data.api.response.RegistroResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Call

interface WawakusiApiService {

    //Aqui iran todos los recursos de la API realizado en Node
    @POST("login")
    fun login(@Body loginRequest: LoginRequest): Call<LoginResponse>

    @POST("usuario")
    fun registrar(@Body registroRequest: RegistroRequest): Call<RegistroResponse>
}