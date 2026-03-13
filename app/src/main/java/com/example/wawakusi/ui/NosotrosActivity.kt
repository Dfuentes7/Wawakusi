package com.example.wawakusi.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.wawakusi.databinding.ActivityNosotrosBinding

class NosotrosActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNosotrosBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNosotrosBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}