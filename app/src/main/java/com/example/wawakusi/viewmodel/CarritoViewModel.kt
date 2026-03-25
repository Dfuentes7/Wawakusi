package com.example.wawakusi.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.wawakusi.data.api.response.CarritoResponse
import com.example.wawakusi.data.api.response.MessageResponse
import com.example.wawakusi.repository.CarritoRepository

class CarritoViewModel : ViewModel() {
    var carritoResponse: LiveData<CarritoResponse>
    var agregarItemResponse: LiveData<MessageResponse>
    var actualizarItemResponse: LiveData<MessageResponse>
    var eliminarItemResponse: LiveData<MessageResponse>

    private var repository = CarritoRepository()

    init {
        carritoResponse = repository.carritoResponse
        agregarItemResponse = repository.agregarItemResponse
        actualizarItemResponse = repository.actualizarItemResponse
        eliminarItemResponse = repository.eliminarItemResponse
    }

    fun obtenerMiCarrito() {
        carritoResponse = repository.obtenerMiCarrito()
    }

    fun agregarItem(productoVarianteId: Int, cantidad: Int) {
        agregarItemResponse = repository.agregarItem(productoVarianteId, cantidad)
    }

    fun actualizarItem(idDetalle: Int, cantidad: Int) {
        actualizarItemResponse = repository.actualizarItem(idDetalle, cantidad)
    }

    fun eliminarItem(idDetalle: Int) {
        eliminarItemResponse = repository.eliminarItem(idDetalle)
    }
}

