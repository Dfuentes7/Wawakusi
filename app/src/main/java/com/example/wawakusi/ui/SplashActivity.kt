package com.example.wawakusi.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.wawakusi.R

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Para usar el modo edge-to-edge (si lo deseas)
        setContentView(R.layout.activity_splash)

        // Configuración de la vista para tomar en cuenta las barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Iniciar la actividad de Login después de 5 segundos
        Handler().postDelayed({
            // Después de 5 segundos, se inicia la actividad de login
            val intent = Intent(this@SplashActivity, LoginActivity::class.java)
            startActivity(intent)
            finish() // Finaliza SplashActivity para no dejarla en la pila de actividades
        }, 5000) // 5000 milisegundos = 5 segundos
    }
}