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
        AppMensaje.enviarMensaje(binding.root, response.mensaje, TipoMensaje.ADVERTENCIA)

        // Si el registro fue exitoso (asumiendo que response.rpta es un valor booleano que indica éxito)
        if (response.rpta) {
            // Redirigir a la actividad LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)

            // Finalizar la actividad actual para que el usuario no regrese a la pantalla de registro
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
        // Deshabilitar el botón para evitar múltiples clics
        binding.btnGuardar.isEnabled = false

        // Obtener el valor seleccionado del RadioGroup (Sexo)
        val sexo = when {
            binding.rbtMasculino.isChecked -> "Masculino"
            binding.rbtFemenino.isChecked -> "Femenino"
            else -> "Prefiero no decirlo"  // Valor predeterminado si ninguno está seleccionado
        }

        // Obtener el estado del CheckBox (Términos y condiciones)
        val terminos = binding.chkTyc.isChecked  // Devuelve un Boolean: true si está marcado, false si no

        authViewModel.registro(
            binding.tietDni.text.toString(),
            binding.tietApellidoPaterno.text.toString(),
            binding.tietApellidoMaterno.text.toString(),
            binding.tietNombres.text.toString(),
            binding.tietCelular.text.toString(),
            sexo,
            binding.tietCorreo.text.toString(),
            binding.tietClave.text.toString(),
            terminos

        )

    }
}