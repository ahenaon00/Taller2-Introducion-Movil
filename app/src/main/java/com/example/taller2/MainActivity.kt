package com.example.taller2

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taller2.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.contactsButton.setOnClickListener {
            startActivity(Intent(this, ContactosActivity::class.java))
        }
        binding.cameraButton.setOnClickListener {
            startActivity(Intent(this, CamaraActivity::class.java))
        }
        binding.osmButton.setOnClickListener {
            startActivity(Intent(this, OsmActivity::class.java))
        }
    }
}