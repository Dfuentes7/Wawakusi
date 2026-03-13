package com.example.wawakusi.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.wawakusi.databinding.ActivityPromocionesBinding

class PromocionesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPromocionesBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       binding = ActivityPromocionesBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}