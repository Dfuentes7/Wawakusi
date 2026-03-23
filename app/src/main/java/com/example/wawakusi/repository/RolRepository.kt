package com.example.wawakusi.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.wawakusi.data.api.WawakusiApiClient
import com.example.wawakusi.data.api.response.RolResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RolRepository {

    var rolesResponse = MutableLiveData<List<RolResponse>>()

    fun listarRolesAdmin(): MutableLiveData<List<RolResponse>> {
        val call: Call<List<RolResponse>> = WawakusiApiClient.retrofitService.listarRolesAdmin()
        call.enqueue(object : Callback<List<RolResponse>> {
            override fun onResponse(call: Call<List<RolResponse>>, response: Response<List<RolResponse>>) {
                if (response.isSuccessful) {
                    rolesResponse.value = response.body() ?: emptyList()
                    return
                }
                rolesResponse.value = emptyList()
            }

            override fun onFailure(call: Call<List<RolResponse>>, t: Throwable) {
                Log.e("ErrorRolesAdmin", t.message.toString())
                rolesResponse.value = emptyList()
            }
        })
        return rolesResponse
    }
}

