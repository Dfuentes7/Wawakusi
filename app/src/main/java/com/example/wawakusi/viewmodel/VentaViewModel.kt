package com.example.wawakusi.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.wawakusi.data.api.response.MessageResponse
import com.example.wawakusi.data.api.response.VentaListResponse
import com.example.wawakusi.repository.VentaRepository

class VentaViewModel : ViewModel() {
    var misVentasResponse: LiveData<VentaListResponse>
    var ventasAdminResponse: LiveData<VentaListResponse>
    var actualizarEstadoResponse: LiveData<MessageResponse>

    private var repository = VentaRepository()

    init {
        misVentasResponse = repository.misVentasResponse
        ventasAdminResponse = repository.ventasAdminResponse
        actualizarEstadoResponse = repository.actualizarEstadoResponse
    }

    fun listarMisVentas() {
        misVentasResponse = repository.listarMisVentas()
    }

    fun listarVentasAdmin() {
        ventasAdminResponse = repository.listarVentasAdmin()
    }

    fun actualizarEstadoVenta(idVenta: Int, estado: Int) {
        actualizarEstadoResponse = repository.actualizarEstadoVenta(idVenta, estado)
    }
}

