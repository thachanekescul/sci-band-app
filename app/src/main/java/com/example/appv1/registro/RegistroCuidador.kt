package com.example.appv1.registro

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.appv1.R
import com.example.appv1.cuidador.MainActivityCuidador
import com.example.appv1.cuidador.RegistroDePaciente
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*
import kotlin.collections.HashMap

class RegistroCuidador : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var storageRef: StorageReference

    private lateinit var ivFotoPerfil: ImageView
    private lateinit var btnSeleccionarFoto: Button
    private lateinit var btnRegistro: Button
    private lateinit var etEmail: EditText
    private lateinit var etPass: EditText
    private lateinit var etConfirmar: EditText
    private lateinit var etOrganizacion: EditText
    private lateinit var etNombre: EditText
    private lateinit var etApellido: EditText
    private lateinit var etCelular: EditText

    private var imageUri: Uri? = null

    private val PICK_IMAGE_REQUEST = 71

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_cuidador)

        db = FirebaseFirestore.getInstance()
        storageRef = FirebaseStorage.getInstance().reference

        ivFotoPerfil = findViewById(R.id.ivFotoPerfil)
        btnSeleccionarFoto = findViewById(R.id.btnSeleccionarFoto)
        btnRegistro = findViewById(R.id.btnRegistro)

        etEmail = findViewById(R.id.etEmail)
        etPass = findViewById(R.id.etPass)
        etConfirmar = findViewById(R.id.etConfirmar)
        etOrganizacion = findViewById(R.id.etOrganizacion)
        etNombre = findViewById(R.id.etNombre)
        etApellido = findViewById(R.id.etApellido)
        etCelular = findViewById(R.id.etCelular)

        btnSeleccionarFoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        btnRegistro.setOnClickListener {
            registrarCuidador()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            imageUri = data.data
            ivFotoPerfil.setImageURI(imageUri)
        }
    }

    private fun registrarCuidador() {
        val email = etEmail.text.toString().trim()
        val pass = etPass.text.toString().trim()
        val confirmar = etConfirmar.text.toString().trim()
        val orgCodigo = etOrganizacion.text.toString().trim()
        val nombre = etNombre.text.toString().trim()
        val apellido = etApellido.text.toString().trim()
        val celular = etCelular.text.toString().trim()

        if (email.isEmpty() || pass.isEmpty() || confirmar.isEmpty() ||
            orgCodigo.isEmpty() || nombre.isEmpty() || apellido.isEmpty() || celular.isEmpty()
        ) {
            Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Correo inválido", Toast.LENGTH_SHORT).show()
            return
        }

        if (pass != confirmar) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        val orgRef = db.collection("organizacion").document(orgCodigo)

        orgRef.get().addOnSuccessListener { orgDoc ->
            if (!orgDoc.exists()) {
                Toast.makeText(this, "La organización no existe", Toast.LENGTH_SHORT).show()
            } else {
                val cuidadoresRef = orgRef.collection("cuidadores")

                cuidadoresRef.whereEqualTo("email", email).get()
                    .addOnSuccessListener { snapshot ->
                        if (!snapshot.isEmpty) {
                            Toast.makeText(this, "El correo ya está registrado", Toast.LENGTH_SHORT).show()
                        } else {
                            // Generar ID cuidador
                            val idCuidador = generarIdCuidador()

                            if (imageUri != null) {
                                // Subir foto primero
                                subirImagenYRegistrarCuidador(idCuidador, email, pass, nombre, apellido, celular, orgCodigo)
                            } else {
                                // Registrar sin foto
                                registrarCuidadorSinFoto(idCuidador, email, pass, nombre, apellido, celular, orgCodigo)
                            }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al verificar el correo", Toast.LENGTH_SHORT).show()
                    }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al verificar la organización", Toast.LENGTH_SHORT).show()
        }
    }

    private fun subirImagenYRegistrarCuidador(
        idCuidador: String,
        email: String,
        pass: String,
        nombre: String,
        apellido: String,
        celular: String,
        orgCodigo: String
    ) {
        val fileReference = storageRef.child("profile_pictures/cuidador_${UUID.randomUUID()}.jpg")

        imageUri?.let { uri ->
            fileReference.putFile(uri)
                .addOnSuccessListener {
                    fileReference.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()
                        val datos = hashMapOf(
                            "email" to email,
                            "password" to pass,
                            "nombre" to nombre,
                            "ape" to apellido,
                            "cel" to celular,
                            "profile_picture_url" to imageUrl
                        )
                        guardarCuidador(idCuidador, datos, orgCodigo)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun registrarCuidadorSinFoto(
        idCuidador: String,
        email: String,
        pass: String,
        nombre: String,
        apellido: String,
        celular: String,
        orgCodigo: String
    ) {
        val datos = hashMapOf(
            "email" to email,
            "password" to pass,
            "nombre" to nombre,
            "ape" to apellido,
            "cel" to celular
        )
        guardarCuidador(idCuidador, datos, orgCodigo)
    }

    private fun guardarCuidador(idCuidador: String, datos: HashMap<String, String>, orgCodigo: String) {
        val cuidadoresRef = db.collection("organizacion").document(orgCodigo).collection("cuidadores")

        cuidadoresRef.document(idCuidador).set(datos)
            .addOnSuccessListener {
                cuidadoresRef.document("placeholder").get().addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        cuidadoresRef.document("placeholder").delete()
                    }
                }

                val prefs = getSharedPreferences("usuario_sesion", MODE_PRIVATE)
                prefs.edit()
                    .putString("tipo_usuario", "cuidador")
                    .putString("id_usuario", idCuidador)
                    .putString("id_organizacion", orgCodigo)
                    .apply()

                AlertDialog.Builder(this)
                    .setTitle("Registro exitoso")
                    .setMessage("¿Desea registrar su primer paciente ahora o continuar en la app?")
                    .setPositiveButton("Registrar paciente") { _, _ ->
                        startActivity(Intent(this, RegistroDePaciente::class.java))
                        finish()
                    }
                    .setNegativeButton("Continuar en la app") { _, _ ->
                        startActivity(Intent(this, MainActivityCuidador::class.java))
                        finish()
                    }
                    .setCancelable(false)
                    .show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al registrar", Toast.LENGTH_SHORT).show()
            }
    }

    private fun generarIdCuidador(): String {
        val caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..5).map { caracteres.random() }.joinToString("")
    }
}
