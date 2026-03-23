package com.example.wawakusi.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.wawakusi.data.api.WawakusiApiClient
import com.example.wawakusi.data.api.request.ActualizarUsuarioAdminRequest
import com.example.wawakusi.data.api.request.CrearUsuarioAdminRequest
import com.example.wawakusi.data.api.response.MessageResponse
import com.example.wawakusi.data.api.response.UsuarioRolResponse
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UsuarioAdminRepository {

    var usuariosRolesResponse = MutableLiveData<List<UsuarioRolResponse>>()
    var crearUsuarioResponse = MutableLiveData<MessageResponse>()
    var actualizarUsuarioResponse = MutableLiveData<MessageResponse>()

    private val gson = Gson()

    fun listarUsuariosAdmin(): MutableLiveData<List<UsuarioRolResponse>> {
        val call: Call<List<UsuarioRolResponse>> = WawakusiApiClient.retrofitService.listarUsuariosAdmin()
        call.enqueue(object : Callback<List<UsuarioRolResponse>> {
            override fun onResponse(
                call: Call<List<UsuarioRolResponse>>,
                response: Response<List<UsuarioRolResponse>>
            ) {
                if (response.isSuccessful) {
                    usuariosRolesResponse.value = response.body() ?: emptyList()
                    return
                }
                usuariosRolesResponse.value = emptyList()
            }

            override fun onFailure(call: Call<List<UsuarioRolResponse>>, t: Throwable) {
                Log.e("ErrorUsuariosAdmin", t.message.toString())
                usuariosRolesResponse.value = emptyList()
            }
        })
        return usuariosRolesResponse
    }

    fun crearUsuarioAdmin(request: CrearUsuarioAdminRequest): MutableLiveData<MessageResponse> {
        val call: Call<MessageResponse> = WawakusiApiClient.retrofitService.crearUsuarioAdmin(request)
        call.enqueue(object : Callback<MessageResponse> {
            override fun onResponse(call: Call<MessageResponse>, response: Response<MessageResponse>) {
                if (response.isSuccessful) {
                    crearUsuarioResponse.value = response.body() ?: MessageResponse(message = "Usuario creado.")
                    return
                }

                val errorJson = response.errorBody()?.string()
                val parsed = try {
                    if (errorJson.isNullOrBlank()) null else gson.fromJson(errorJson, MessageResponse::class.java)
                } catch (_: Exception) {
                    null
                }

                crearUsuarioResponse.value =
                    parsed ?: MessageResponse(message = "No se pudo crear usuario (${response.code()}).")
            }

            override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                Log.e("ErrorCrearUsuarioAdmin", t.message.toString())
                crearUsuarioResponse.value = MessageResponse(message = "No se pudo conectar con el servidor.")
            }
        })
        return crearUsuarioResponse
    }

    fun actualizarUsuarioAdmin(id: Int, request: ActualizarUsuarioAdminRequest): MutableLiveData<MessageResponse> {
        val call: Call<MessageResponse> = WawakusiApiClient.retrofitService.actualizarUsuarioAdmin(id, request)
        call.enqueue(object : Callback<MessageResponse> {
            override fun onResponse(call: Call<MessageResponse>, response: Response<MessageResponse>) {
                if (response.isSuccessful) {
                    actualizarUsuarioResponse.value =
                        response.body() ?: MessageResponse(message = "Usuario actualizado.")
                    return
                }

                val errorJson = response.errorBody()?.string()
                val parsed = try {
                    if (errorJson.isNullOrBlank()) null else gson.fromJson(errorJson, MessageResponse::class.java)
                } catch (_: Exception) {
                    null
                }

                actualizarUsuarioResponse.value =
                    parsed ?: MessageResponse(message = "No se pudo actualizar usuario (${response.code()}).")
            }

            override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                Log.e("ErrorActualizarUsuarioAdmin", t.message.toString())
                actualizarUsuarioResponse.value = MessageResponse(message = "No se pudo conectar con el servidor.")
            }
        })
        return actualizarUsuarioResponse
    }
}
