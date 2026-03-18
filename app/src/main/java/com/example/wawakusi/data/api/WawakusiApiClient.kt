package com.example.wawakusi.data.api
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object WawakusiApiClient {
    private var retrofitCliente = OkHttpClient.Builder()
        .connectTimeout(1, TimeUnit.MINUTES)
        .readTimeout(15, TimeUnit.MINUTES)
        .writeTimeout(15, TimeUnit.MINUTES)

        .build()

    private fun buildRetrofit() = Retrofit.Builder()
        .baseUrl("http://192.168.1.50:4000/api/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(retrofitCliente)
        .build()
    val retrofitService: WawakusiApiService by lazy {
        buildRetrofit().create(WawakusiApiService::class.java)
    }
}