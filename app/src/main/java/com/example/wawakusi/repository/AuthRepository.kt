package com.example.wawakusi.repository
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.wawakusi.data.api.WawakusiApiClient
import com.example.wawakusi.data.api.request.LoginRequest
import com.example.wawakusi.data.api.request.RegistroRequest
import com.example.wawakusi.data.api.response.LoginResponse
import com.example.wawakusi.data.api.response.RegistroResponse
import com.example.wawakusi.data.api.response.VistasResponse
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
class AuthRepository {
    var loginResponse= MutableLiveData<LoginResponse>()
    var registroResponse = MutableLiveData<RegistroResponse>()
    var vistasResponse = MutableLiveData<VistasResponse>()

    private val gson = Gson()

    fun login(loginRequest: LoginRequest): MutableLiveData<LoginResponse> {
        val call: Call<LoginResponse> = WawakusiApiClient.retrofitService.login(loginRequest)
        call.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    loginResponse.value = response.body()
                        ?: LoginResponse(rpta = false, mensaje = "No se pudo procesar la respuesta del servidor.")
                    return
                }

                val errorJson = response.errorBody()?.string()
                val parsed = try {
                    if (errorJson.isNullOrBlank()) null else gson.fromJson(errorJson, LoginResponse::class.java)
                } catch (_: Exception) {
                    null
                }

                loginResponse.value = parsed ?: LoginResponse(
                    rpta = false,
                    mensaje = "Error ${response.code()} al iniciar sesión."
                )
            }
            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Log.e("ErrorLogin", t.message.toString())
                loginResponse.value = LoginResponse(rpta = false, mensaje = "No se pudo conectar con el servidor.")
            }
        })
        return loginResponse
    }


    fun registro(registroRequest: RegistroRequest): MutableLiveData<RegistroResponse> {
        val call: Call<RegistroResponse> = WawakusiApiClient.retrofitService.registrar(registroRequest)
        call.enqueue(object : Callback<RegistroResponse> {
            override fun onResponse(call: Call<RegistroResponse>, response: Response<RegistroResponse>) {
                if (response.isSuccessful) {
                    registroResponse.value = response.body()
                        ?: RegistroResponse(rpta = false, mensaje = "No se pudo procesar la respuesta del servidor.")
                    return
                }

                val errorJson = response.errorBody()?.string()
                val parsed = try {
                    if (errorJson.isNullOrBlank()) null else gson.fromJson(errorJson, RegistroResponse::class.java)
                } catch (_: Exception) {
                    null
                }

                registroResponse.value = parsed ?: RegistroResponse(
                    rpta = false,
                    mensaje = "Error ${response.code()} al registrar."
                )
            }
            override fun onFailure(call: Call<RegistroResponse>, t: Throwable) {
                Log.e("ErrorLogin", t.message.toString())
                registroResponse.value = RegistroResponse(rpta = false, mensaje = "No se pudo conectar con el servidor.")
            }
        })
        return registroResponse
    }

    fun obtenerVistas(): MutableLiveData<VistasResponse> {
        val call: Call<VistasResponse> = WawakusiApiClient.retrofitService.vistas()
        call.enqueue(object : Callback<VistasResponse> {
            override fun onResponse(call: Call<VistasResponse>, response: Response<VistasResponse>) {
                if (response.isSuccessful) {
                    vistasResponse.value = response.body()
                        ?: VistasResponse(rpta = false, publico = true)
                    return
                }

                val errorJson = response.errorBody()?.string()
                val parsed = try {
                    if (errorJson.isNullOrBlank()) null else gson.fromJson(errorJson, VistasResponse::class.java)
                } catch (_: Exception) {
                    null
                }

                vistasResponse.value = parsed ?: VistasResponse(rpta = false, publico = true)
            }

            override fun onFailure(call: Call<VistasResponse>, t: Throwable) {
                Log.e("ErrorVistas", t.message.toString())
                vistasResponse.value = VistasResponse(rpta = false, publico = true)
            }
        })
        return vistasResponse
    }

}
