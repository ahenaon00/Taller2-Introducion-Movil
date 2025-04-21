package com.example.taller2

import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taller2.databinding.ActivityContactosBinding

class ContactosActivity : AppCompatActivity() {
    val getSimplePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(), ActivityResultCallback {
            if(it){//granted
                updateUI()
                Toast.makeText(this, "Permiso concedido", Toast.LENGTH_SHORT).show()
            }else {//denied
                Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
            }
        })

    val projection = arrayOf(ContactsContract.Profile._ID, ContactsContract.Profile.DISPLAY_NAME_PRIMARY)
    private val adapter = ContactosAdapter(this, null, 0)
    private lateinit var binding: ActivityContactosBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityContactosBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.listContacts.adapter=adapter
        getSimplePermission.launch(android.Manifest.permission.READ_CONTACTS)
    }

    fun updateUI() {
        val cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, projection, null, null, null)
        adapter.changeCursor(cursor)
    }
}