package com.example.wawakusi.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.wawakusi.data.api.WawakusiApiClient
import com.example.wawakusi.data.api.request.UpdateMeRequest
import com.example.wawakusi.data.api.response.MeResponse
import com.example.wawakusi.data.api.response.UpdateMeResponse
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UsuarioRepository {
    var meResponse = MutableLiveData<MeResponse>()
    var updateMeResponse = MutableLiveData<UpdateMeResponse>()

    private val gson = Gson()

    fun me(): MutableLiveData<MeResponse> {
        val call: Call<MeResponse> = WawakusiApiClient.retrofitService.me()
        call.enqueue(object : Callback<MeResponse> {
            override fun onResponse(call: Call<MeResponse>, response: Response<MeResponse>) {
                if (response.isSuccessful) {
                    meResponse.value = response.body() ?: MeResponse(rpta = false, mensaje = "No se pudo procesar la respuesta del servidor.")
                    return
                }

                val errorJson = response.errorBody()?.string()
                val parsed = try {
                    if (errorJson.isNullOrBlank()) null else gson.fromJson(errorJson, MeResponse::class.java)
                } catch (_: Exception) {
                    null
                }

                meResponse.value = parsed ?: MeResponse(rpta = false, mensaje = "Error ${response.code()} al obtener perfil.")
            }

            override fun onFailure(call: Call<MeResponse>, t: Throwable) {
                Log.e("ErrorMe", t.message.toString())
                meResponse.value = MeResponse(rpta = false, mensaje = "No se pudo conectar con el servidor.")
            }
        })
        return meResponse
    }

    fun updateMe(request: UpdateMeRequest): MutableLiveData<UpdateMeResponse> {
        val call: Call<UpdateMeResponse> = WawakusiApiClient.retrofitService.updateMe(request)
        call.enqueue(object : Callback<UpdateMeResponse> {
            override fun onResponse(call: Call<UpdateMeResponse>, response: Response<UpdateMeResponse>) {
                if (response.isSuccessful) {
                    updateMeResponse.value =
                        response.body() ?: UpdateMeResponse(rpta = false, mensaje = "No se pudo procesar la respuesta del servidor.")
                    return
                }

                val errorJson = response.errorBody()?.string()
                val parsed = try {
                    if (errorJson.isNullOrBlank()) null else gson.fromJson(errorJson, UpdateMeResponse::class.java)
                } catch (_: Exception) {
                    null
                }

                updateMeResponse.value = parsed ?: UpdateMeResponse(rpta = false, mensaje = "Error ${response.code()} al actualizar perfil.")
            }

            override fun onFailure(call: Call<UpdateMeResponse>, t: Throwable) {
                Log.e("ErrorUpdateMe", t.message.toString())
                updateMeResponse.value = UpdateMeResponse(rpta = false, mensaje = "No se pudo conectar con el servidor.")
            }
        })
        return updateMeResponse
    }
}

