package com.example.wawakusi.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.wawakusi.data.api.request.UpdateMeRequest
import com.example.wawakusi.data.api.response.MeResponse
import com.example.wawakusi.data.api.response.UpdateMeResponse
import com.example.wawakusi.repository.UsuarioRepository

class UsuarioViewModel : ViewModel() {
    var meResponse: LiveData<MeResponse>
    var updateMeResponse: LiveData<UpdateMeResponse>

    private var repository = UsuarioRepository()

    init {
        meResponse = repository.meResponse
        updateMeResponse = repository.updateMeResponse
    }

    fun cargarPerfil() {
        meResponse = repository.me()
    }

    fun actualizarPerfil(request: UpdateMeRequest) {
        updateMeResponse = repository.updateMe(request)
    }
}

