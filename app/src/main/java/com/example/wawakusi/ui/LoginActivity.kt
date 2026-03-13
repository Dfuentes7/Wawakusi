package com.example.wawakusi.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.wawakusi.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Establecer el OnClickListener para el botón de "Regístrate"
        binding.btnRegister.setOnClickListener {
            // Crear un Intent para ir a la RegistroActivity
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }
    }
}