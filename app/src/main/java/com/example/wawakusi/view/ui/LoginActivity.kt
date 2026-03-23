package com.example.wawakusi.view.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.wawakusi.databinding.ActivityLoginBinding
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.wawakusi.data.api.response.LoginResponse
import com.example.wawakusi.util.AppMensaje
import com.example.wawakusi.util.SharedPreferencesManager
import com.example.wawakusi.util.TipoMensaje
import com.example.wawakusi.viewmodel.AuthViewModel
import com.example.wawakusi.R


class LoginActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        authViewModel = ViewModelProvider(this)
            .get(AuthViewModel::class.java)
        binding.btnLogin.setOnClickListener(this)
        binding.btnRegister.setOnClickListener(this)
        authViewModel.loginResponse.observe(this, Observer {
                response -> obtenerDatosLogin(response)
        })

    }
    private fun obtenerDatosLogin(response: LoginResponse) {
        if (response.rpta) {
            val token = response.token
            if (token.isNullOrBlank()) {
                AppMensaje.enviarMensaje(binding.root, "No se recibió el token de sesión.", TipoMensaje.ERROR)
            } else {
                SharedPreferencesManager.guardarSesion(
                    token = token,
                    usuario = response.auth?.usuario,
                    rol = response.auth?.rolNombre
                )
                val destino = if (response.auth?.rolNombre == "ADMIN") {
                    AdminPanelActivity::class.java
                } else {
                    MainActivity::class.java
                }
                val intent = Intent(applicationContext, destino)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        } else {
            AppMensaje.enviarMensaje(binding.root, response.mensaje, TipoMensaje.ERROR)
        }
        binding.btnLogin.isEnabled = true
        binding.btnRegister.isEnabled = true
    }

    override fun onClick(v: View) {
        when(v.id){
            R.id.btnLogin -> autenticarUsuario()
            R.id.btnRegister -> startActivity(Intent(applicationContext, RegistroActivity::class.java))
        }
    }

    private fun autenticarUsuario() {
        binding.btnLogin.isEnabled = false
        binding.btnRegister.isEnabled = false

        authViewModel.login(binding.etEmail.text.toString(),
            binding.etPassword.text.toString())
    }
}
