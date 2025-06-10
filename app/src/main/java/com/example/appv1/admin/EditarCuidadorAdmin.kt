package com.example.appv1.admin

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.appv1.R
import com.google.firebase.firestore.FirebaseFirestore

class EditarCuidadorAdmin : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var etNombre: EditText
    private lateinit var etApellido: EditText
    private lateinit var etCelular: EditText
    private lateinit var etEmail: EditText
    private lateinit var btnGuardar: Button
    private lateinit var btnEliminar: Button

    private lateinit var cuidadorId: String
    private lateinit var organizacionId: String
    private lateinit var adminId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_cuidador_admin) // tu layout nuevo

        db = FirebaseFirestore.getInstance()

        etNombre = findViewById(R.id.etNombre)
        etApellido = findViewById(R.id.etApellido)
        etCelular = findViewById(R.id.etCelular)
        etEmail = findViewById(R.id.etEmail)
        btnGuardar = findViewById(R.id.btnGuardar)
        btnEliminar = findViewById(R.id.btnEliminar)

        cuidadorId = intent.getStringExtra("idCuidador") ?: ""
        val prefs = getSharedPreferences("usuario_sesion", Context.MODE_PRIVATE)
        organizacionId = prefs.getString("id_organizacion", "")!!
        adminId = prefs.getString("id_usuario", "")!!

        cargarDatosCuidador()

        btnGuardar.setOnClickListener {
            guardarCambios()
        }

        btnEliminar.setOnClickListener {
            mostrarDialogoConfirmacion()
        }
    }

    private fun cargarDatosCuidador() {
        val cuidadorRef = db.collection("organizacion")
            .document(organizacionId)
            .collection("cuidadores")
            .document(cuidadorId)

        cuidadorRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                etNombre.setText(document.getString("nombre") ?: "")
                etApellido.setText(document.getString("ape") ?: "")
                etCelular.setText(document.getString("cel") ?: "")
                etEmail.setText(document.getString("email") ?: "")
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al cargar datos del cuidador", Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardarCambios() {
        val nuevoNombre = etNombre.text.toString().trim()
        val nuevoApellido = etApellido.text.toString().trim()
        val nuevoCelular = etCelular.text.toString().trim()
        val nuevoEmail = etEmail.text.toString().trim()

        if (nuevoNombre.isEmpty() || nuevoApellido.isEmpty() || nuevoCelular.isEmpty() || nuevoEmail.isEmpty()) {
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
                "cel" to nuevoCelular,
                "email" to nuevoEmail
            )
        ).addOnSuccessListener {
            Toast.makeText(this, "Datos actualizados exitosamente", Toast.LENGTH_LONG).show()
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "Error al actualizar cuidador", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarDialogoConfirmacion() {
        val editText = EditText(this)
        editText.hint = "Contraseña"
        editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD

        AlertDialog.Builder(this)
            .setTitle("Confirmar Eliminación")
            .setMessage("Ingrese su contraseña para eliminar este cuidador.")
            .setView(editText)
            .setPositiveButton("Eliminar") { dialog, _ ->
                val passwordIngresada = editText.text.toString().trim()
                verificarPassword(passwordIngresada)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun verificarPassword(passwordIngresada: String) {
        db.collection("organizacion")
            .document(organizacionId)
            .collection("administradores")
            .document(adminId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val passwordReal = document.getString("password") ?: ""
                    if (passwordIngresada == passwordReal) {
                        eliminarCuidador()
                    } else {
                        Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al verificar contraseña", Toast.LENGTH_SHORT).show()
            }
    }

    private fun eliminarCuidador() {
        db.collection("organizacion")
            .document(organizacionId)
            .collection("cuidadores")
            .document(cuidadorId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Cuidador eliminado exitosamente", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al eliminar cuidador", Toast.LENGTH_SHORT).show()
            }
    }
}
