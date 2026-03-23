package com.example.wawakusi.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.wawakusi.data.api.request.ActualizarUsuarioAdminRequest
import com.example.wawakusi.data.api.request.CrearUsuarioAdminRequest
import com.example.wawakusi.data.api.response.MessageResponse
import com.example.wawakusi.data.api.response.UsuarioRolResponse
import com.example.wawakusi.repository.UsuarioAdminRepository

class UsuarioAdminViewModel : ViewModel() {
    var usuariosRolesResponse: LiveData<List<UsuarioRolResponse>>
    var crearUsuarioResponse: LiveData<MessageResponse>
    var actualizarUsuarioResponse: LiveData<MessageResponse>

    private var repository = UsuarioAdminRepository()

    init {
        usuariosRolesResponse = repository.usuariosRolesResponse
        crearUsuarioResponse = repository.crearUsuarioResponse
        actualizarUsuarioResponse = repository.actualizarUsuarioResponse
    }

    fun listarUsuariosAdmin() {
        usuariosRolesResponse = repository.listarUsuariosAdmin()
    }

    fun crearUsuarioAdmin(request: CrearUsuarioAdminRequest) {
        crearUsuarioResponse = repository.crearUsuarioAdmin(request)
    }

    fun actualizarUsuarioAdmin(id: Int, request: ActualizarUsuarioAdminRequest) {
        actualizarUsuarioResponse = repository.actualizarUsuarioAdmin(id, request)
    }
}
