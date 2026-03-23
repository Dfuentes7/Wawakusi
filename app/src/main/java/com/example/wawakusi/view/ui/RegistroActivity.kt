package com.example.wawakusi.view.ui

import android.os.Bundle
import android.view.View
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.example.wawakusi.databinding.ActivityRegistroBinding
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.wawakusi.data.api.response.RegistroResponse
import com.example.wawakusi.util.AppMensaje
import com.example.wawakusi.util.TipoMensaje
import com.example.wawakusi.viewmodel.AuthViewModel
import com.example.wawakusi.R
import android.util.Patterns


class RegistroActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityRegistroBinding
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)
        authViewModel = ViewModelProvider(this).get(AuthViewModel::class.java)
        binding.btnGuardar.setOnClickListener(this)
        authViewModel.registroResponse.observe(this, Observer {
                response -> obtenerDatosRegistro(response)
        })
    }
    private fun obtenerDatosRegistro(response: RegistroResponse) {
        binding.btnGuardar.isEnabled = true
        AppMensaje.enviarMensaje(
            binding.root,
            response.mensaje,
            if (response.rpta) TipoMensaje.CORRECTO else TipoMensaje.ERROR
        )

        if (response.rpta) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onClick(v: View) {
        when(v.id){
            R.id.btnGuardar -> registrarUsuario()
        }
    }

    private fun registrarUsuario() {

        binding.btnGuardar.isEnabled = false
        limpiarErrores()

        val sexo = when {
            binding.rbtMasculino.isChecked -> "Masculino"
            binding.rbtFemenino.isChecked -> "Femenino"
            else -> "Prefiero no decirlo"
        }

        val dni = binding.tietDni.text?.toString()?.trim().orEmpty()
        val apellidoPaterno = binding.tietApellidoPaterno.text?.toString()?.trim().orEmpty()
        val apellidoMaterno = binding.tietApellidoMaterno.text?.toString()?.trim().orEmpty()
        val nombres = binding.tietNombres.text?.toString()?.trim().orEmpty()
        val celular = binding.tietCelular.text?.toString()?.trim().orEmpty()
        val correoIngresado = binding.tietCorreo.text?.toString()?.trim().orEmpty()
        val clave = binding.tietClave.text?.toString().orEmpty()
        val terminos = binding.chkTyc.isChecked

        var valido = true

        if (dni.length != 8) {
            binding.tilDni.error = "Ingrese un DNI válido de 8 dígitos."
            valido = false
        }
        if (apellidoPaterno.isBlank()) {
            binding.tilApellidoPaterno.error = "Campo obligatorio."
            valido = false
        }
        if (apellidoMaterno.isBlank()) {
            binding.tilApellidoMaterno.error = "Campo obligatorio."
            valido = false
        }
        if (nombres.isBlank()) {
            binding.tilNombres.error = "Campo obligatorio."
            valido = false
        }
        if (celular.isNotBlank() && celular.length != 9) {
            binding.tilCelular.error = "Ingrese un celular válido de 9 dígitos."
            valido = false
        }

        val correo = if (correoIngresado.contains("@")) correoIngresado else "$correoIngresado@wawakusi.com"
        if (correoIngresado.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            binding.tilCorreo.error = "Ingrese un correo válido."
            valido = false
        }
        if (clave.length < 6) {
            binding.tilClave.error = "La contraseña debe tener al menos 6 caracteres."
            valido = false
        }
        if (!terminos) {
            AppMensaje.enviarMensaje(binding.root, "Debe aceptar los términos y condiciones.", TipoMensaje.ADVERTENCIA)
            valido = false
        }

        if (!valido) {
            binding.btnGuardar.isEnabled = true
            return
        }

        authViewModel.registro(
            dni,
            apellidoPaterno,
            apellidoMaterno,
            nombres,
            celular,
            sexo,
            correo,
            clave,
            terminos
        )
    }

    private fun limpiarErrores() {
        binding.tilDni.error = null
        binding.tilApellidoPaterno.error = null
        binding.tilApellidoMaterno.error = null
        binding.tilNombres.error = null
        binding.tilCelular.error = null
        binding.tilCorreo.error = null
        binding.tilClave.error = null
    }
}
