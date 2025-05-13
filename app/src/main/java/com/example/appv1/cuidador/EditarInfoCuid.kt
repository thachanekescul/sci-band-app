package com.example.appv1.cuidador

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.appv1.R
import com.google.firebase.firestore.FirebaseFirestore
class EditarInfoCuid : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var etNombre: EditText
    private lateinit var etApellido: EditText
    private lateinit var etEmail: EditText
    private lateinit var etTelefono: EditText
    private lateinit var btnGuardar: Button

    private lateinit var cuidadorId: String
    private lateinit var organizacionId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_info_cuid)

        db = FirebaseFirestore.getInstance()

        // Inicializar vistas
        etNombre = findViewById(R.id.etNombre)
        etApellido = findViewById(R.id.etApellido)
        etEmail = findViewById(R.id.etEmail)
        etTelefono = findViewById(R.id.etCelular)
        btnGuardar = findViewById(R.id.btnGuardar)

        val prefs = getSharedPreferences("usuario_sesion", Context.MODE_PRIVATE)
        cuidadorId = prefs.getString("id_usuario", "")!!
        organizacionId = prefs.getString("id_organizacion", "")!!

        cargarDatosCuidador()

        btnGuardar.setOnClickListener {
            guardarCambios()
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

        val cuidadorRef = db.collection("organizacion")
            .document(organizacionId)
            .collection("cuidadores")
            .document(cuidadorId)

        cuidadorRef.update(
            mapOf(
                "nombre" to nuevoNombre,
                "ape" to nuevoApellido,
                "email" to nuevoEmail,
                "cel" to nuevoTelefono
            )
        ).addOnSuccessListener {
            Toast.makeText(this, "Datos actualizados exitosamente", Toast.LENGTH_LONG).show()
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "Error al actualizar datos", Toast.LENGTH_SHORT).show()
        }
    }
}
