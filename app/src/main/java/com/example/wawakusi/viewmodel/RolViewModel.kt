package com.example.wawakusi.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.wawakusi.data.api.response.RolResponse
import com.example.wawakusi.repository.RolRepository

class RolViewModel : ViewModel() {
    var rolesResponse: LiveData<List<RolResponse>>

    private var repository = RolRepository()

    init {
        rolesResponse = repository.rolesResponse
    }

    fun listarRolesAdmin() {
        rolesResponse = repository.listarRolesAdmin()
    }
}

