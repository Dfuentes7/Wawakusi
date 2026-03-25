package com.example.wawakusi.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.wawakusi.data.api.WawakusiApiClient
import com.example.wawakusi.data.api.response.ConsultarPedidoResponse
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PedidoRepository {

    var consultarPedidoResponse = MutableLiveData<ConsultarPedidoResponse>()
    private val gson = Gson()

    fun consultarPorCodigo(codigo: String): MutableLiveData<ConsultarPedidoResponse> {
        val call: Call<ConsultarPedidoResponse> = WawakusiApiClient.retrofitService.consultarPedido(codigo)
        call.enqueue(object : Callback<ConsultarPedidoResponse> {
            override fun onResponse(call: Call<ConsultarPedidoResponse>, response: Response<ConsultarPedidoResponse>) {
                if (response.isSuccessful) {
                    consultarPedidoResponse.value =
                        response.body() ?: ConsultarPedidoResponse(rpta = false, mensaje = "Respuesta inválida del servidor.")
                    return
                }

                val errorJson = response.errorBody()?.string()
                val parsed = try {
                    if (errorJson.isNullOrBlank()) null else gson.fromJson(errorJson, ConsultarPedidoResponse::class.java)
                } catch (_: Exception) {
                    null
                }
                consultarPedidoResponse.value =
                    parsed ?: ConsultarPedidoResponse(rpta = false, mensaje = "Error ${response.code()} al consultar pedido.")
            }

            override fun onFailure(call: Call<ConsultarPedidoResponse>, t: Throwable) {
                Log.e("ErrorConsultarPedido", t.message.toString())
                consultarPedidoResponse.value = ConsultarPedidoResponse(rpta = false, mensaje = "No se pudo conectar con el servidor.")
            }
        })
        return consultarPedidoResponse
    }
}

