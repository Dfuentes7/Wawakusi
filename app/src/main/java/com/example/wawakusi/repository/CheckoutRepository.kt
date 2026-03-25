package com.example.wawakusi.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.wawakusi.data.api.WawakusiApiClient
import com.example.wawakusi.data.api.request.CheckoutPaypalCreateRequest
import com.example.wawakusi.data.api.request.CheckoutPaypalCaptureRequest
import com.example.wawakusi.data.api.response.CheckoutPaypalCreateResponse
import com.example.wawakusi.data.api.response.CheckoutPaypalCaptureResponse
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CheckoutRepository {

    var checkoutCreateResponse = MutableLiveData<CheckoutPaypalCreateResponse>()
    var checkoutCaptureResponse = MutableLiveData<CheckoutPaypalCaptureResponse>()

    private val gson = Gson()

    fun paypalCreate(direccionEnvio: String): MutableLiveData<CheckoutPaypalCreateResponse> {
        val call: Call<CheckoutPaypalCreateResponse> = WawakusiApiClient.retrofitService.checkoutPaypalCreate(
            CheckoutPaypalCreateRequest(direccionEnvio = direccionEnvio)
        )
        call.enqueue(object : Callback<CheckoutPaypalCreateResponse> {
            override fun onResponse(
                call: Call<CheckoutPaypalCreateResponse>,
                response: Response<CheckoutPaypalCreateResponse>
            ) {
                if (response.isSuccessful) {
                    checkoutCreateResponse.value =
                        response.body() ?: CheckoutPaypalCreateResponse(rpta = false, mensaje = "Respuesta inválida del servidor.")
                    return
                }

                val errorJson = response.errorBody()?.string()
                val parsed = try {
                    if (errorJson.isNullOrBlank()) null else gson.fromJson(errorJson, CheckoutPaypalCreateResponse::class.java)
                } catch (_: Exception) {
                    null
                }

                checkoutCreateResponse.value =
                    parsed ?: CheckoutPaypalCreateResponse(rpta = false, mensaje = "Error ${response.code()} al iniciar checkout.")
            }

            override fun onFailure(call: Call<CheckoutPaypalCreateResponse>, t: Throwable) {
                Log.e("ErrorCheckoutCreate", t.message.toString())
                checkoutCreateResponse.value = CheckoutPaypalCreateResponse(rpta = false, mensaje = "No se pudo conectar con el servidor.")
            }
        })
        return checkoutCreateResponse
    }

    fun paypalCapture(paypalOrderId: String, checkoutContext: String?): MutableLiveData<CheckoutPaypalCaptureResponse> {
        val call: Call<CheckoutPaypalCaptureResponse> = WawakusiApiClient.retrofitService.checkoutPaypalCapture(
            CheckoutPaypalCaptureRequest(paypalOrderId = paypalOrderId, checkoutContext = checkoutContext)
        )
        call.enqueue(object : Callback<CheckoutPaypalCaptureResponse> {
            override fun onResponse(
                call: Call<CheckoutPaypalCaptureResponse>,
                response: Response<CheckoutPaypalCaptureResponse>
            ) {
                if (response.isSuccessful) {
                    checkoutCaptureResponse.value =
                        response.body() ?: CheckoutPaypalCaptureResponse(rpta = false, mensaje = "Respuesta inválida del servidor.")
                    return
                }

                val errorJson = response.errorBody()?.string()
                val parsed = try {
                    if (errorJson.isNullOrBlank()) null else gson.fromJson(errorJson, CheckoutPaypalCaptureResponse::class.java)
                } catch (_: Exception) {
                    null
                }

                checkoutCaptureResponse.value =
                    parsed ?: CheckoutPaypalCaptureResponse(rpta = false, mensaje = "Error ${response.code()} al confirmar pago.")
            }

            override fun onFailure(call: Call<CheckoutPaypalCaptureResponse>, t: Throwable) {
                Log.e("ErrorCheckoutCapture", t.message.toString())
                checkoutCaptureResponse.value = CheckoutPaypalCaptureResponse(rpta = false, mensaje = "No se pudo conectar con el servidor.")
            }
        })
        return checkoutCaptureResponse
    }
}
