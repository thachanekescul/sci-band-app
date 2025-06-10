package com.example.appv1.cuidador

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.appv1.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*

class EditarInfoCuid : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var storageRef: StorageReference

    private lateinit var ivFotoPerfil: ImageView
    private lateinit var btnSeleccionarFoto: Button
    private lateinit var btnGuardar: Button

    private lateinit var etNombre: EditText
    private lateinit var etApellido: EditText
    private lateinit var etEmail: EditText
    private lateinit var etTelefono: EditText

    private lateinit var cuidadorId: String
    private lateinit var organizacionId: String

    private var imageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 71

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_info_cuid)

        db = FirebaseFirestore.getInstance()
        storageRef = FirebaseStorage.getInstance().reference

        ivFotoPerfil = findViewById(R.id.ivFotoPerfil)
        btnSeleccionarFoto = findViewById(R.id.btnSeleccionarFoto)
        btnGuardar = findViewById(R.id.btnGuardar)

        etNombre = findViewById(R.id.etNombre)
        etApellido = findViewById(R.id.etApellido)
        etEmail = findViewById(R.id.etEmail)
        etTelefono = findViewById(R.id.etCelular)

        val prefs = getSharedPreferences("usuario_sesion", MODE_PRIVATE)
        cuidadorId = prefs.getString("id_usuario", "")!!
        organizacionId = prefs.getString("id_organizacion", "")!!

        cargarDatosCuidador()

        btnSeleccionarFoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        btnGuardar.setOnClickListener {
            guardarCambios()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            imageUri = data.data
            ivFotoPerfil.setImageURI(imageUri)
        }
    }

    private fun cargarDatosCuidador() {
        val cuidadorRef = db.collection("organizacion")
            .document(organizacionId)
            .collection("cuidadores")
            .document(cuidadorId)

        cuidadorRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                etNombre.setText(document.getString("nombre"))
                etApellido.setText(document.getString("ape"))
                etEmail.setText(document.getString("email"))
                etTelefono.setText(document.getString("cel"))

                // Cargar foto con Glide si existe URL
                val fotoUrl = document.getString("profile_picture_url")
                if (!fotoUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(fotoUrl)
                        .placeholder(R.drawable.images)
                        .into(ivFotoPerfil)
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardarCambios() {
        val nuevoNombre = etNombre.text.toString().trim()
        val nuevoApellido = etApellido.text.toString().trim()
        val nuevoEmail = etEmail.text.toString().trim()
        val nuevoTelefono = etTelefono.text.toString().trim()

        if (nuevoNombre.isEmpty() || nuevoApellido.isEmpty() || nuevoEmail.isEmpty() || nuevoTelefono.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (imageUri != null) {
            subirImagenYGuardarDatos(nuevoNombre, nuevoApellido, nuevoEmail, nuevoTelefono)
        } else {
            // Guardar sin cambiar la foto
            guardarDatos(nuevoNombre, nuevoApellido, nuevoEmail, nuevoTelefono, null)
        }
    }

    private fun subirImagenYGuardarDatos(
        nombre: String,
        apellido: String,
        email: String,
        telefono: String
    ) {
        val fileRef = storageRef.child("profile_pictures/cuidador_${UUID.randomUUID()}.jpg")

        imageUri?.let { uri ->
            fileRef.putFile(uri)
                .addOnSuccessListener {
                    fileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        guardarDatos(nombre, apellido, email, telefono, downloadUri.toString())
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun guardarDatos(
        nombre: String,
        apellido: String,
        email: String,
        telefono: String,
        fotoUrl: String?
    ) {
        val cuidadorRef = db.collection("organizacion")
            .document(organizacionId)
            .collection("cuidadores")
            .document(cuidadorId)

        val datosActualizados = mutableMapOf<String, Any>(
            "nombre" to nombre,
            "ape" to apellido,
            "email" to email,
            "cel" to telefono
        )
        if (fotoUrl != null) {
            datosActualizados["profile_picture_url"] = fotoUrl
        }

        cuidadorRef.update(datosActualizados)
            .addOnSuccessListener {
                Toast.makeText(this, "Datos actualizados exitosamente", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar datos", Toast.LENGTH_SHORT).show()
            }
    }
}
