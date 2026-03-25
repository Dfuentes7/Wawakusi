package com.example.wawakusi.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.wawakusi.data.api.WawakusiApiClient
import com.example.wawakusi.data.api.request.AgregarCarritoItemRequest
import com.example.wawakusi.data.api.request.ActualizarCarritoItemRequest
import com.example.wawakusi.data.api.response.CarritoResponse
import com.example.wawakusi.data.api.response.MessageResponse
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CarritoRepository {

    var carritoResponse = MutableLiveData<CarritoResponse>()
    var agregarItemResponse = MutableLiveData<MessageResponse>()
    var actualizarItemResponse = MutableLiveData<MessageResponse>()
    var eliminarItemResponse = MutableLiveData<MessageResponse>()

    private val gson = Gson()

    fun obtenerMiCarrito(): MutableLiveData<CarritoResponse> {
        val call: Call<CarritoResponse> = WawakusiApiClient.retrofitService.miCarrito()
        call.enqueue(object : Callback<CarritoResponse> {
            override fun onResponse(call: Call<CarritoResponse>, response: Response<CarritoResponse>) {
                if (response.isSuccessful) {
                    carritoResponse.value =
                        response.body() ?: CarritoResponse(rpta = false, mensaje = "No se pudo procesar la respuesta del servidor.")
                    return
                }

                val errorJson = response.errorBody()?.string()
                val parsed = try {
                    if (errorJson.isNullOrBlank()) null else gson.fromJson(errorJson, CarritoResponse::class.java)
                } catch (_: Exception) {
                    null
                }

                carritoResponse.value =
                    parsed ?: CarritoResponse(rpta = false, mensaje = "Error ${response.code()} al obtener carrito.")
            }

            override fun onFailure(call: Call<CarritoResponse>, t: Throwable) {
                Log.e("ErrorMiCarrito", t.message.toString())
                carritoResponse.value = CarritoResponse(rpta = false, mensaje = "No se pudo conectar con el servidor.")
            }
        })
        return carritoResponse
    }

    fun agregarItem(productoVarianteId: Int, cantidad: Int): MutableLiveData<MessageResponse> {
        val call: Call<MessageResponse> = WawakusiApiClient.retrofitService.agregarCarritoItem(
            AgregarCarritoItemRequest(productoVarianteId = productoVarianteId, cantidad = cantidad)
        )
        call.enqueue(object : Callback<MessageResponse> {
            override fun onResponse(call: Call<MessageResponse>, response: Response<MessageResponse>) {
                if (response.isSuccessful) {
                    agregarItemResponse.value = response.body() ?: MessageResponse(message = "Agregado al carrito.")
                    return
                }

                val errorJson = response.errorBody()?.string()
                val parsed = try {
                    if (errorJson.isNullOrBlank()) null else gson.fromJson(errorJson, MessageResponse::class.java)
                } catch (_: Exception) {
                    null
                }

                agregarItemResponse.value =
                    parsed ?: MessageResponse(message = "No se pudo agregar al carrito (${response.code()}).")
            }

            override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                Log.e("ErrorAgregarItemCarrito", t.message.toString())
                agregarItemResponse.value = MessageResponse(message = "No se pudo conectar con el servidor.")
            }
        })
        return agregarItemResponse
    }

    fun actualizarItem(idDetalle: Int, cantidad: Int): MutableLiveData<MessageResponse> {
        val call: Call<MessageResponse> = WawakusiApiClient.retrofitService.actualizarCarritoItem(
            idDetalle,
            ActualizarCarritoItemRequest(cantidad = cantidad)
        )
        call.enqueue(object : Callback<MessageResponse> {
            override fun onResponse(call: Call<MessageResponse>, response: Response<MessageResponse>) {
                if (response.isSuccessful) {
                    actualizarItemResponse.value = response.body() ?: MessageResponse(message = "Carrito actualizado.")
                    return
                }

                val errorJson = response.errorBody()?.string()
                val parsed = try {
                    if (errorJson.isNullOrBlank()) null else gson.fromJson(errorJson, MessageResponse::class.java)
                } catch (_: Exception) {
                    null
                }

                actualizarItemResponse.value =
                    parsed ?: MessageResponse(message = "No se pudo actualizar carrito (${response.code()}).")
            }

            override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                Log.e("ErrorActualizarItemCarrito", t.message.toString())
                actualizarItemResponse.value = MessageResponse(message = "No se pudo conectar con el servidor.")
            }
        })
        return actualizarItemResponse
    }

    fun eliminarItem(idDetalle: Int): MutableLiveData<MessageResponse> {
        val call: Call<MessageResponse> = WawakusiApiClient.retrofitService.eliminarCarritoItem(idDetalle)
        call.enqueue(object : Callback<MessageResponse> {
            override fun onResponse(call: Call<MessageResponse>, response: Response<MessageResponse>) {
                if (response.isSuccessful) {
                    eliminarItemResponse.value = response.body() ?: MessageResponse(message = "Producto eliminado del carrito.")
                    return
                }

                val errorJson = response.errorBody()?.string()
                val parsed = try {
                    if (errorJson.isNullOrBlank()) null else gson.fromJson(errorJson, MessageResponse::class.java)
                } catch (_: Exception) {
                    null
                }

                eliminarItemResponse.value =
                    parsed ?: MessageResponse(message = "No se pudo eliminar del carrito (${response.code()}).")
            }

            override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                Log.e("ErrorEliminarItemCarrito", t.message.toString())
                eliminarItemResponse.value = MessageResponse(message = "No se pudo conectar con el servidor.")
            }
        })
        return eliminarItemResponse
    }
}

