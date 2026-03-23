package com.example.wawakusi.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.wawakusi.data.api.request.CrearDescuentoRequest
import com.example.wawakusi.data.api.response.MessageResponse
import com.example.wawakusi.data.api.response.PromocionAdminResponse
import com.example.wawakusi.repository.DescuentoRepository

class DescuentoViewModel : ViewModel() {
    var promocionesAdminResponse: LiveData<List<PromocionAdminResponse>>
    var crearPromocionAdminResponse: LiveData<MessageResponse>
    var eliminarPromocionAdminResponse: LiveData<MessageResponse>

    private var repository = DescuentoRepository()

    init {
        promocionesAdminResponse = repository.promocionesAdminResponse
        crearPromocionAdminResponse = repository.crearPromocionAdminResponse
        eliminarPromocionAdminResponse = repository.eliminarPromocionAdminResponse
    }

    fun listarPromocionesAdmin() {
        promocionesAdminResponse = repository.listarPromocionesAdmin()
    }

    fun crearPromocionAdmin(request: CrearDescuentoRequest) {
        crearPromocionAdminResponse = repository.crearPromocionAdmin(request)
    }

    fun eliminarPromocionAdmin(id: Int) {
        eliminarPromocionAdminResponse = repository.eliminarPromocionAdmin(id)
    }
}

