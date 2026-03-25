package com.example.wawakusi.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.wawakusi.data.api.response.ConsultarPedidoResponse
import com.example.wawakusi.repository.PedidoRepository

class PedidoViewModel : ViewModel() {

    var consultarPedidoResponse: LiveData<ConsultarPedidoResponse>
    private var repository = PedidoRepository()

    init {
        consultarPedidoResponse = repository.consultarPedidoResponse
    }

    fun consultarPorCodigo(codigo: String) {
        consultarPedidoResponse = repository.consultarPorCodigo(codigo)
    }
}

