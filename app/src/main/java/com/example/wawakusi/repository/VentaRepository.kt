package com.example.wawakusi.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.wawakusi.data.api.WawakusiApiClient
import com.example.wawakusi.data.api.request.ActualizarEstadoVentaRequest
import com.example.wawakusi.data.api.response.MessageResponse
import com.example.wawakusi.data.api.response.VentaListResponse
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VentaRepository {

    var misVentasResponse = MutableLiveData<VentaListResponse>()
    var ventasAdminResponse = MutableLiveData<VentaListResponse>()
    var actualizarEstadoResponse = MutableLiveData<MessageResponse>()

    private val gson = Gson()

    fun listarMisVentas(): MutableLiveData<VentaListResponse> {
        val call: Call<VentaListResponse> = WawakusiApiClient.retrofitService.misVentas()
        call.enqueue(object : Callback<VentaListResponse> {
            override fun onResponse(call: Call<VentaListResponse>, response: Response<VentaListResponse>) {
                if (response.isSuccessful) {
                    misVentasResponse.value =
                        response.body() ?: VentaListResponse(rpta = false, mensaje = "Respuesta inválida del servidor.")
                    return
                }

                val errorJson = response.errorBody()?.string()
                val parsed = try {
                    if (errorJson.isNullOrBlank()) null else gson.fromJson(errorJson, VentaListResponse::class.java)
                } catch (_: Exception) {
                    null
                }
                misVentasResponse.value = parsed ?: VentaListResponse(rpta = false, mensaje = "Error ${response.code()} al listar pedidos.")
            }

            override fun onFailure(call: Call<VentaListResponse>, t: Throwable) {
                Log.e("ErrorMisVentas", t.message.toString())
                misVentasResponse.value = VentaListResponse(rpta = false, mensaje = "No se pudo conectar con el servidor.")
            }
        })
        return misVentasResponse
    }

    fun listarVentasAdmin(): MutableLiveData<VentaListResponse> {
        val call: Call<VentaListResponse> = WawakusiApiClient.retrofitService.ventasAdmin()
        call.enqueue(object : Callback<VentaListResponse> {
            override fun onResponse(call: Call<VentaListResponse>, response: Response<VentaListResponse>) {
                if (response.isSuccessful) {
                    ventasAdminResponse.value =
                        response.body() ?: VentaListResponse(rpta = false, mensaje = "Respuesta inválida del servidor.")
                    return
                }

                val errorJson = response.errorBody()?.string()
                val parsed = try {
                    if (errorJson.isNullOrBlank()) null else gson.fromJson(errorJson, VentaListResponse::class.java)
                } catch (_: Exception) {
                    null
                }
                ventasAdminResponse.value =
                    parsed ?: VentaListResponse(rpta = false, mensaje = "Error ${response.code()} al listar ventas.")
            }

            override fun onFailure(call: Call<VentaListResponse>, t: Throwable) {
                Log.e("ErrorVentasAdmin", t.message.toString())
                ventasAdminResponse.value = VentaListResponse(rpta = false, mensaje = "No se pudo conectar con el servidor.")
            }
        })
        return ventasAdminResponse
    }

    fun actualizarEstadoVenta(idVenta: Int, estado: Int): MutableLiveData<MessageResponse> {
        val call: Call<MessageResponse> = WawakusiApiClient.retrofitService.actualizarEstadoVenta(
            idVenta,
            ActualizarEstadoVentaRequest(estado = estado)
        )
        call.enqueue(object : Callback<MessageResponse> {
            override fun onResponse(call: Call<MessageResponse>, response: Response<MessageResponse>) {
                if (response.isSuccessful) {
                    actualizarEstadoResponse.value =
                        response.body() ?: MessageResponse(rpta = false, mensaje = "Respuesta inválida del servidor.")
                    return
                }

                val errorJson = response.errorBody()?.string()
                val parsed = try {
                    if (errorJson.isNullOrBlank()) null else gson.fromJson(errorJson, MessageResponse::class.java)
                } catch (_: Exception) {
                    null
                }
                actualizarEstadoResponse.value =
                    parsed ?: MessageResponse(rpta = false, mensaje = "Error ${response.code()} al actualizar estado.")
            }

            override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                Log.e("ErrorActualizarEstado", t.message.toString())
                actualizarEstadoResponse.value = MessageResponse(rpta = false, mensaje = "No se pudo conectar con el servidor.")
            }
        })
        return actualizarEstadoResponse
    }
}

