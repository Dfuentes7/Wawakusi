package com.example.wawakusi.view.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.example.wawakusi.databinding.ActivitySplashBinding
import com.example.wawakusi.util.SharedPreferencesManager

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Iniciar la actividad de Login después de 5 segundos
        Handler().postDelayed({
            val siguiente = if (SharedPreferencesManager.estaAutenticado()) {
                if (SharedPreferencesManager.obtenerRol() == "ADMIN") {
                    AdminPanelActivity::class.java
                } else {
                    MainActivity::class.java
                }
            } else {
                LoginActivity::class.java
            }
            val intent = Intent(this@SplashActivity, siguiente)
            startActivity(intent)
            finish() // Finaliza SplashActivity para no dejarla en la pila de actividades
        }, 5000) // 5000 milisegundos = 5 segundos
    }
}
