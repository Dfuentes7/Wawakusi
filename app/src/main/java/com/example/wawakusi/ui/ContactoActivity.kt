package com.example.wawakusi.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.wawakusi.databinding.ActivityContactoBinding

class ContactoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityContactoBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactoBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}