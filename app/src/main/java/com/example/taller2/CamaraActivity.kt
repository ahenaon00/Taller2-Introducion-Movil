package com.example.taller2

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.ActivityResultCallback
import androidx.core.content.FileProvider
import com.example.taller2.databinding.ActivityCamaraBinding
import java.io.File

class CamaraActivity : AppCompatActivity() {

    private lateinit var uriCamera : Uri

    val getContentGallery = registerForActivityResult(ActivityResultContracts.GetContent(),
        ActivityResultCallback {
        loadImage(it!!)
    })
    val getContentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture(),ActivityResultCallback {
            if(it){
                loadImage(uriCamera)
            }
        })

    private lateinit var binding : ActivityCamaraBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityCamaraBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.galleryButton.setOnClickListener {
            getContentGallery.launch("image/*")
        }
        binding.cameraButton.setOnClickListener {
            val file = File(getFilesDir(), "picFromCamera")
            uriCamera = FileProvider.getUriForFile(baseContext, baseContext.packageName + ".fileprovider", file)
            getContentCamera.launch(uriCamera);
        }
    }

    fun loadImage(uri : Uri){
        val imageStream = getContentResolver().openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(imageStream)
        binding.imgView.setImageBitmap(bitmap)
    }


}