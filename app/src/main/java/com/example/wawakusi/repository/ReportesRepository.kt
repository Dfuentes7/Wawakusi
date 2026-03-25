package com.example.wawakusi.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.wawakusi.data.api.WawakusiApiClient
import com.example.wawakusi.data.api.response.DashboardResponse
import com.example.wawakusi.data.api.response.ReporteVentasResponse
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReportesRepository {

    var reporteVentasResponse = MutableLiveData<ReporteVentasResponse>()
    var dashboardResponse = MutableLiveData<DashboardResponse>()
    private val gson = Gson()

    fun obtenerReporteVentas(desde: String?, hasta: String?): MutableLiveData<ReporteVentasResponse> {
        val call: Call<ReporteVentasResponse> = WawakusiApiClient.retrofitService.reporteVentas(desde, hasta)
        call.enqueue(object : Callback<ReporteVentasResponse> {
            override fun onResponse(call: Call<ReporteVentasResponse>, response: Response<ReporteVentasResponse>) {
                if (response.isSuccessful) {
                    reporteVentasResponse.value =
                        response.body() ?: ReporteVentasResponse(rpta = false, mensaje = "Respuesta inválida del servidor.")
                    return
                }

                val errorJson = response.errorBody()?.string()
                val parsed = try {
                    if (errorJson.isNullOrBlank()) null else gson.fromJson(errorJson, ReporteVentasResponse::class.java)
                } catch (_: Exception) {
                    null
                }
                reporteVentasResponse.value =
                    parsed ?: ReporteVentasResponse(rpta = false, mensaje = "Error ${response.code()} al obtener reportes.")
            }

            override fun onFailure(call: Call<ReporteVentasResponse>, t: Throwable) {
                Log.e("ErrorReporteVentas", t.message.toString())
                reporteVentasResponse.value = ReporteVentasResponse(rpta = false, mensaje = "No se pudo conectar con el servidor.")
            }
        })
        return reporteVentasResponse
    }

    fun obtenerDashboard(dias: Int?): MutableLiveData<DashboardResponse> {
        val call: Call<DashboardResponse> = WawakusiApiClient.retrofitService.dashboard(dias)
        call.enqueue(object : Callback<DashboardResponse> {
            override fun onResponse(call: Call<DashboardResponse>, response: Response<DashboardResponse>) {
                if (response.isSuccessful) {
                    dashboardResponse.value =
                        response.body() ?: DashboardResponse(rpta = false, mensaje = "Respuesta inválida del servidor.")
                    return
                }

                val errorJson = response.errorBody()?.string()
                val parsed = try {
                    if (errorJson.isNullOrBlank()) null else gson.fromJson(errorJson, DashboardResponse::class.java)
                } catch (_: Exception) {
                    null
                }
                dashboardResponse.value =
                    parsed ?: DashboardResponse(rpta = false, mensaje = "Error ${response.code()} al obtener dashboard.")
            }

            override fun onFailure(call: Call<DashboardResponse>, t: Throwable) {
                Log.e("ErrorDashboard", t.message.toString())
                dashboardResponse.value = DashboardResponse(rpta = false, mensaje = "No se pudo conectar con el servidor.")
            }
        })
        return dashboardResponse
    }
}
