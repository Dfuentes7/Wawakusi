package com.example.wawakusi.data.api

import okhttp3.Interceptor
import okhttp3.Response
import com.example.wawakusi.util.SharedPreferencesManager

class ApiInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = SharedPreferencesManager.obtenerTokenValido()
        val requestBuilder = chain.request().newBuilder()

        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val response = chain.proceed(requestBuilder.build())

        // CORREGIDO: Usar el método code() en lugar de acceder directamente
        if (response.code() == 401) {
            SharedPreferencesManager.limpiarSesionYNotificar()
        }

        return response
    }
}