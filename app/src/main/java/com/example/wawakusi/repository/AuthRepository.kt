package com.example.wawakusi.repository
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.wawakusi.data.api.WawakusiApiClient
import com.example.wawakusi.data.api.request.LoginRequest
import com.example.wawakusi.data.api.request.RegistroRequest
import com.example.wawakusi.data.api.response.LoginResponse
import com.example.wawakusi.data.api.response.RegistroResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
class AuthRepository {
    var loginResponse= MutableLiveData<LoginResponse>()
    var registroResponse = MutableLiveData<RegistroResponse>()

    fun login(loginRequest: LoginRequest): MutableLiveData<LoginResponse> {
        val call: Call<LoginResponse> = WawakusiApiClient.retrofitService.login(loginRequest)
        call.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                loginResponse.value = response.body()
            }
            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Log.e("ErrorLogin", t.message.toString())
            }
        })
        return loginResponse
    }


    fun registro(registroRequest: RegistroRequest): MutableLiveData<RegistroResponse> {
        val call: Call<RegistroResponse> = WawakusiApiClient.retrofitService.registrar(registroRequest)
        call.enqueue(object : Callback<RegistroResponse> {
            override fun onResponse(call: Call<RegistroResponse>, response: Response<RegistroResponse>) {
                registroResponse.value = response.body()
            }
            override fun onFailure(call: Call<RegistroResponse>, t: Throwable) {
                Log.e("ErrorLogin", t.message.toString())
            }
        })
        return registroResponse
    }

}