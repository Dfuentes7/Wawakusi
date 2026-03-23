package com.example.wawakusi.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.wawakusi.data.api.WawakusiApiClient
import com.example.wawakusi.data.api.request.CrearDescuentoRequest
import com.example.wawakusi.data.api.response.MessageResponse
import com.example.wawakusi.data.api.response.PromocionAdminResponse
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DescuentoRepository {

    var promocionesAdminResponse = MutableLiveData<List<PromocionAdminResponse>>()
    var crearPromocionAdminResponse = MutableLiveData<MessageResponse>()
    var eliminarPromocionAdminResponse = MutableLiveData<MessageResponse>()

    private val gson = Gson()

    fun listarPromocionesAdmin(): MutableLiveData<List<PromocionAdminResponse>> {
        val call: Call<List<PromocionAdminResponse>> = WawakusiApiClient.retrofitService.listarPromocionesAdmin()
        call.enqueue(object : Callback<List<PromocionAdminResponse>> {
            override fun onResponse(call: Call<List<PromocionAdminResponse>>, response: Response<List<PromocionAdminResponse>>) {
                if (response.isSuccessful) {
                    promocionesAdminResponse.value = response.body() ?: emptyList()
                    return
                }
                promocionesAdminResponse.value = emptyList()
            }

            override fun onFailure(call: Call<List<PromocionAdminResponse>>, t: Throwable) {
                Log.e("ErrorPromocionesAdmin", t.message.toString())
                promocionesAdminResponse.value = emptyList()
            }
        })
        return promocionesAdminResponse
    }

    fun crearPromocionAdmin(request: CrearDescuentoRequest): MutableLiveData<MessageResponse> {
        val call: Call<MessageResponse> = WawakusiApiClient.retrofitService.crearPromocionAdmin(request)
        call.enqueue(object : Callback<MessageResponse> {
            override fun onResponse(call: Call<MessageResponse>, response: Response<MessageResponse>) {
                if (response.isSuccessful) {
                    crearPromocionAdminResponse.value =
                        response.body() ?: MessageResponse(message = "Promoción registrada.")
                    return
                }

                val errorJson = response.errorBody()?.string()
                val parsed = try {
                    if (errorJson.isNullOrBlank()) null else gson.fromJson(errorJson, MessageResponse::class.java)
                } catch (_: Exception) {
                    null
                }

                crearPromocionAdminResponse.value = parsed
                    ?: MessageResponse(message = "No se pudo registrar promoción (${response.code()}).")
            }

            override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                Log.e("ErrorCrearPromocionAdmin", t.message.toString())
                crearPromocionAdminResponse.value = MessageResponse(message = "No se pudo conectar con el servidor.")
            }
        })
        return crearPromocionAdminResponse
    }

    fun eliminarPromocionAdmin(id: Int): MutableLiveData<MessageResponse> {
        val call: Call<MessageResponse> = WawakusiApiClient.retrofitService.eliminarPromocionAdmin(id)
        call.enqueue(object : Callback<MessageResponse> {
            override fun onResponse(call: Call<MessageResponse>, response: Response<MessageResponse>) {
                if (response.isSuccessful) {
                    eliminarPromocionAdminResponse.value =
                        response.body() ?: MessageResponse(message = "Promoción eliminada.")
                    return
                }

                val errorJson = response.errorBody()?.string()
                val parsed = try {
                    if (errorJson.isNullOrBlank()) null else gson.fromJson(errorJson, MessageResponse::class.java)
                } catch (_: Exception) {
                    null
                }

                eliminarPromocionAdminResponse.value = parsed
                    ?: MessageResponse(message = "No se pudo eliminar promoción (${response.code()}).")
            }

            override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                Log.e("ErrorEliminarPromocionAdmin", t.message.toString())
                eliminarPromocionAdminResponse.value = MessageResponse(message = "No se pudo conectar con el servidor.")
            }
        })
        return eliminarPromocionAdminResponse
    }
}

