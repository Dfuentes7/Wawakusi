package com.example.wawakusi.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.wawakusi.databinding.ActivityTiendaBinding

class TiendaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTiendaBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTiendaBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}