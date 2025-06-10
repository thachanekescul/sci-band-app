package com.example.appv1.cuidador

import android.app.DatePickerDialog
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

class FormularioPaciente : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var ivFotoPerfil: ImageView
    private lateinit var btnSeleccionarFoto: Button
    private lateinit var btnRegistro: Button
    private lateinit var etNombre: EditText
    private lateinit var etApellido: EditText
    private lateinit var etCelular: EditText
    private lateinit var etFechaNacimiento: EditText
    private lateinit var spCondicion: Spinner
    private lateinit var etPeso: EditText
    private lateinit var etEstatura: EditText

    private lateinit var codigoQR: String
    private val condiciones = arrayOf("Ninguna", "Diabetes", "Hipertensión", "Cáncer", "Obesidad", "Otro")
    private var imageUri: Uri? = null

    private val storageRef: StorageReference = FirebaseStorage.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_formulario_paciente)

        db = FirebaseFirestore.getInstance()

        ivFotoPerfil = findViewById(R.id.ivFotoPerfil)
        btnSeleccionarFoto = findViewById(R.id.btnSeleccionarFoto)  // Botón para seleccionar foto
        btnRegistro = findViewById(R.id.btnRegistro)
        etNombre = findViewById(R.id.etNombre)
        etApellido = findViewById(R.id.etApellido)
        etCelular = findViewById(R.id.etCelular)
        etFechaNacimiento = findViewById(R.id.etFechaNacimiento)
        spCondicion = findViewById(R.id.spCondicion)
        etPeso = findViewById(R.id.etPeso)
        etEstatura = findViewById(R.id.etEstatura)

        codigoQR = intent.getStringExtra("codigo_qr") ?: ""

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, condiciones)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCondicion.adapter = adapter

        // Selección de la fecha de nacimiento
        etFechaNacimiento.setFocusable(false)
        etFechaNacimiento.setOnClickListener {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, y, m, d ->
                etFechaNacimiento.setText(String.format("%02d/%02d/%04d", d, m + 1, y))
            }, year, month, day).show()
        }

        // Botón para seleccionar la foto
        btnSeleccionarFoto.setOnClickListener {
            // Abre la galería para seleccionar una imagen
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            startActivityForResult(intent, 71)  // 71 es el código para seleccionar imagen
        }

        btnRegistro.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val apellido = etApellido.text.toString().trim()
            val celular = etCelular.text.toString().trim()
            val fechaNac = etFechaNacimiento.text.toString().trim()
            val pesoStr = etPeso.text.toString().trim()
            val estaturaStr = etEstatura.text.toString().trim()
            val condicion = spCondicion.selectedItem.toString()

            // Validaciones
            if (nombre.isEmpty() || apellido.isEmpty() || celular.isEmpty() || fechaNac.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Convertir a números los campos de peso y estatura
            val peso = pesoStr.toDoubleOrNull()
            val estatura = estaturaStr.toDoubleOrNull()

            if (pesoStr.isNotEmpty() && peso == null) {
                Toast.makeText(this, "Peso inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (estaturaStr.isNotEmpty() && estatura == null) {
                Toast.makeText(this, "Estatura inválida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Si se seleccionó una foto, la subimos primero
            imageUri?.let {
                subirImagenAFirebase(it)  // Subimos la imagen y guardamos todos los datos
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 71 && resultCode == RESULT_OK && data != null) {
            imageUri = data.data
            ivFotoPerfil.setImageURI(imageUri)  // Mostrar la imagen seleccionada en el ImageView
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

    private fun guardarPacienteConFoto(imageUrl: String) {
        val nombre = etNombre.text.toString().trim()
        val apellido = etApellido.text.toString().trim()
        val celular = etCelular.text.toString().trim()
        val fechaNac = etFechaNacimiento.text.toString().trim()
        val pesoStr = etPeso.text.toString().trim()
        val estaturaStr = etEstatura.text.toString().trim()
        val condicion = spCondicion.selectedItem.toString()

        // Convertir a números los campos de peso y estatura
        val peso = pesoStr.toDoubleOrNull()
        val estatura = estaturaStr.toDoubleOrNull()

        if (nombre.isEmpty() || apellido.isEmpty() || celular.isEmpty() || fechaNac.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (pesoStr.isNotEmpty() && peso == null) {
            Toast.makeText(this, "Peso inválido", Toast.LENGTH_SHORT).show()
            return
        }

        if (estaturaStr.isNotEmpty() && estatura == null) {
            Toast.makeText(this, "Estatura inválida", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences("usuario_sesion", MODE_PRIVATE)
        val orgCodigo = prefs.getString("id_organizacion", "") ?: ""
        val idCuidador = prefs.getString("id_usuario", "") ?: ""

        val pacientesRef = db.collection("organizacion")
            .document(orgCodigo)
            .collection("cuidadores")
            .document(idCuidador)
            .collection("pacientes")

        val pacienteData = hashMapOf(
            "nombre" to nombre,
            "apellido" to apellido,
            "celular" to celular,
            "fecha_nacimiento" to fechaNac,
            "condicion_cronica" to condicion,
            "peso" to (peso ?: 0.0),
            "estatura" to (estatura ?: 0.0),
            "profile_picture_url" to imageUrl,  // Aquí guardamos la URL de la imagen
            "registrado" to true
        )

        pacientesRef.document(codigoQR)
            .set(pacienteData)
            .addOnSuccessListener {
                // Aquí sí actualizamos "escaneado" a true y eliminamos el código
                db.collection("codigos_espera").document(codigoQR)
                    .update("escaneado", true)
                    .addOnSuccessListener {
                        db.collection("codigos_espera").document(codigoQR).delete()
                    }

                Toast.makeText(this, "Paciente registrado exitosamente", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, MainActivityCuidador::class.java))
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al registrar paciente", Toast.LENGTH_SHORT).show()
            }
    }
}
