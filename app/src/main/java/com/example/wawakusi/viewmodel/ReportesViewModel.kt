package com.example.wawakusi.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.wawakusi.data.api.response.DashboardResponse
import com.example.wawakusi.data.api.response.ReporteVentasResponse
import com.example.wawakusi.repository.ReportesRepository

class ReportesViewModel : ViewModel() {

    var reporteVentasResponse: LiveData<ReporteVentasResponse>
    var dashboardResponse: LiveData<DashboardResponse>
    private var repository = ReportesRepository()

    init {
        reporteVentasResponse = repository.reporteVentasResponse
        dashboardResponse = repository.dashboardResponse
    }

    fun obtenerReporteVentas(desde: String?, hasta: String?) {
        reporteVentasResponse = repository.obtenerReporteVentas(desde, hasta)
    }

    fun obtenerDashboard(dias: Int?) {
        dashboardResponse = repository.obtenerDashboard(dias)
    }
}
