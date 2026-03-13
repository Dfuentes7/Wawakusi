package com.example.wawakusi.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.example.wawakusi.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Iniciar la actividad de Login después de 5 segundos
        Handler().postDelayed({
            // Después de 5 segundos, se inicia la actividad de login
            val intent = Intent(this@SplashActivity, LoginActivity::class.java)
            startActivity(intent)
            finish() // Finaliza SplashActivity para no dejarla en la pila de actividades
        }, 5000) // 5000 milisegundos = 5 segundos
    }
}