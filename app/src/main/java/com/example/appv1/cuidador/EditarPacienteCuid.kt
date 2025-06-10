package com.example.appv1.cuidador

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.appv1.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.bumptech.glide.Glide
import java.util.*

class EditarPacienteCuid : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var etNombre: EditText
    private lateinit var etApellido: EditText
    private lateinit var etCelular: EditText
    private lateinit var etFechaNacimiento: EditText
    private lateinit var spCondicion: Spinner
    private lateinit var btnGuardar: Button

    // Nuevos campos
    private lateinit var etPeso: EditText
    private lateinit var etEstatura: EditText
    private lateinit var ivFotoPerfil: ImageView
    private lateinit var btnSubirFoto: Button

    private lateinit var pacienteId: String
    private lateinit var cuidadorId: String
    private lateinit var organizacionId: String

    private val condiciones = arrayOf("Ninguna", "Diabetes", "Hipertensión", "Cáncer", "Obesidad", "Otro")
    private var imageUri: Uri? = null
    private val storageRef: StorageReference = FirebaseStorage.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_paciente_cuid)

        db = FirebaseFirestore.getInstance()

        etNombre = findViewById(R.id.etNombre)
        etApellido = findViewById(R.id.etApellido)
        etCelular = findViewById(R.id.etCelular)
        etFechaNacimiento = findViewById(R.id.etFechaNacimiento)
        spCondicion = findViewById(R.id.spCondicion)
        btnGuardar = findViewById(R.id.btnGuardar)

        // Nuevos EditText
        etPeso = findViewById(R.id.etPeso)
        etEstatura = findViewById(R.id.etEstatura)
        ivFotoPerfil = findViewById(R.id.ivFotoPerfil)
        btnSubirFoto = findViewById(R.id.btnSeleccionarFoto)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, condiciones)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCondicion.adapter = adapter

        val prefs = getSharedPreferences("usuario_sesion", Context.MODE_PRIVATE)
        cuidadorId = prefs.getString("id_usuario", "")!!
        organizacionId = prefs.getString("id_organizacion", "")!!

        pacienteId = intent.getStringExtra("idPaciente") ?: ""

        cargarDatosPaciente()

        etFechaNacimiento.setOnClickListener {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, y, m, d ->
                etFechaNacimiento.setText(String.format("%02d/%02d/%04d", d, m + 1, y))
            }, year, month, day).show()
        }

        btnSubirFoto.setOnClickListener {
            // Abre la galería para seleccionar una imagen
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            startActivityForResult(intent, 71)  // 71 es el código para seleccionar imagen
        }

        btnGuardar.setOnClickListener {
            guardarCambios()
        }
    }

    private fun cargarDatosPaciente() {
        val pacienteRef = db.collection("organizacion")
            .document(organizacionId)
            .collection("cuidadores")
            .document(cuidadorId)
            .collection("pacientes")
            .document(pacienteId)

        pacienteRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                etNombre.setText(document.getString("nombre") ?: "")
                etApellido.setText(document.getString("apellido") ?: "")
                etCelular.setText(document.getString("celular") ?: "")
                etFechaNacimiento.setText(document.getString("fecha_nacimiento") ?: "")

                val condicion = document.getString("condicion_cronica") ?: "Ninguna"
                val index = condiciones.indexOf(condicion)
                if (index >= 0) {
                    spCondicion.setSelection(index)
                }

                // Cargar peso y estatura si existen
                etPeso.setText(document.getString("peso") ?: "")
                etEstatura.setText(document.getString("estatura") ?: "")

                // Cargar foto de perfil si existe
                val profileUrl = document.getString("profile_picture_url")
                if (profileUrl != null) {
                    Glide.with(this).load(profileUrl).into(ivFotoPerfil)
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 71 && resultCode == RESULT_OK && data != null) {
            imageUri = data.data
            ivFotoPerfil.setImageURI(imageUri)  // Mostrar la imagen seleccionada en el ImageView
        }
    }

    private fun guardarCambios() {
        val nuevoNombre = etNombre.text.toString().trim()
        val nuevoApellido = etApellido.text.toString().trim()
        val nuevoCelular = etCelular.text.toString().trim()
        val nuevaFechaNacimiento = etFechaNacimiento.text.toString().trim()
        val nuevaCondicion = spCondicion.selectedItem.toString()

        val pesoStr = etPeso.text.toString().trim()
        val estaturaStr = etEstatura.text.toString().trim()

        if (nuevoNombre.isEmpty() || nuevoApellido.isEmpty() || nuevoCelular.isEmpty() || nuevaFechaNacimiento.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Si se seleccionó una foto, subimos primero
        imageUri?.let {
            subirImagenAFirebase(it)  // Subimos la imagen y guardamos todos los datos
        } ?: run {
            // Si no se seleccionó foto, guardamos solo los datos sin la imagen
            guardarPacienteSinFoto()
        }
    }

    private fun subirImagenAFirebase(imageUri: Uri) {
        val fileReference = storageRef.child("profile_pictures/${UUID.randomUUID()}.jpg")

        fileReference.putFile(imageUri)
            .addOnSuccessListener {
                fileReference.downloadUrl.addOnSuccessListener { uri ->
                    val imageUrl = uri.toString()
                    guardarPacienteConFoto(imageUrl)  // Guardar paciente con la URL de la imagen
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al subir imagen", Toast.LENGTH_SHORT).show()
            }
    }

    private fun guardarPacienteSinFoto() {
        val nuevoNombre = etNombre.text.toString().trim()
        val nuevoApellido = etApellido.text.toString().trim()
        val nuevoCelular = etCelular.text.toString().trim()
        val nuevaFechaNacimiento = etFechaNacimiento.text.toString().trim()
        val nuevaCondicion = spCondicion.selectedItem.toString()

        val pesoStr = etPeso.text.toString().trim()
        val estaturaStr = etEstatura.text.toString().trim()

        val pacienteData = hashMapOf(
            "nombre" to nuevoNombre,
            "apellido" to nuevoApellido,
            "celular" to nuevoCelular,
            "fecha_nacimiento" to nuevaFechaNacimiento,
            "condicion_cronica" to nuevaCondicion,
            "peso" to pesoStr,
            "estatura" to estaturaStr,
            "registrado" to true
        )

        val prefs = getSharedPreferences("usuario_sesion", MODE_PRIVATE)
        val orgCodigo = prefs.getString("id_organizacion", "") ?: ""
        val idCuidador = prefs.getString("id_usuario", "") ?: ""

        val pacientesRef = db.collection("organizacion")
            .document(orgCodigo)
            .collection("cuidadores")
            .document(idCuidador)
            .collection("pacientes")

        pacientesRef.document(pacienteId)
            .set(pacienteData)
            .addOnSuccessListener {
                Toast.makeText(this, "Paciente registrado exitosamente", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, MainActivityCuidador::class.java))
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al registrar paciente", Toast.LENGTH_SHORT).show()
            }
    }

    private fun guardarPacienteConFoto(imageUrl: String) {
        val nuevoNombre = etNombre.text.toString().trim()
        val nuevoApellido = etApellido.text.toString().trim()
        val nuevoCelular = etCelular.text.toString().trim()
        val nuevaFechaNacimiento = etFechaNacimiento.text.toString().trim()
        val nuevaCondicion = spCondicion.selectedItem.toString()

        val pesoStr = etPeso.text.toString().trim()
        val estaturaStr = etEstatura.text.toString().trim()

        val pacienteData = hashMapOf(
            "nombre" to nuevoNombre,
            "apellido" to nuevoApellido,
            "celular" to nuevoCelular,
            "fecha_nacimiento" to nuevaFechaNacimiento,
            "condicion_cronica" to nuevaCondicion,
            "peso" to pesoStr,
            "estatura" to estaturaStr,
            "profile_picture_url" to imageUrl,  // Aquí guardamos la URL de la imagen
            "registrado" to true
        )

        val prefs = getSharedPreferences("usuario_sesion", MODE_PRIVATE)
        val orgCodigo = prefs.getString("id_organizacion", "") ?: ""
        val idCuidador = prefs.getString("id_usuario", "") ?: ""

        val pacientesRef = db.collection("organizacion")
            .document(orgCodigo)
            .collection("cuidadores")
            .document(idCuidador)
            .collection("pacientes")

        pacientesRef.document(pacienteId)
            .set(pacienteData)
            .addOnSuccessListener {
                Toast.makeText(this, "Paciente registrado exitosamente", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, MainActivityCuidador::class.java))
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al registrar paciente", Toast.LENGTH_SHORT).show()
            }
    }
}
