package com.example.appv1.cuidador

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.appv1.CircleAnimationView
import com.example.appv1.MainActivity
import com.example.appv1.R
import com.google.firebase.firestore.FirebaseFirestore

class ConfiguracionCuidador : AppCompatActivity() {

    private lateinit var btnEditarInfo: Button
    private lateinit var btnCerrarSesion: Button
    private lateinit var btnEliminarCuenta: Button

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuracion_cuidador)

        btnEditarInfo = findViewById(R.id.btnEditarInfo)
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion)
        btnEliminarCuenta = findViewById(R.id.btnEliminarCuenta)
        db = FirebaseFirestore.getInstance()

        btnEditarInfo.setOnClickListener {
            startActivity(Intent(this, EditarInfoCuid::class.java))
        }

        btnCerrarSesion.setOnClickListener {
            mostrarDialogoCerrarSesion()
        }

        btnEliminarCuenta.setOnClickListener {
            mostrarDialogoEliminarCuenta()
        }
    }

    private fun mostrarDialogoCerrarSesion() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                val prefs = getSharedPreferences("usuario_sesion", MODE_PRIVATE)
                prefs.edit().clear().apply()
                Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoEliminarCuenta() {
        val input = EditText(this)
        input.hint = "Ingresa tu contraseña"
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle("Eliminar cuenta")
            .setMessage("Esta acción eliminará todos tus datos personales permanentemente.")
            .setView(layout)
            .setPositiveButton("Eliminar") { _, _ ->
                val passwordIngresada = input.text.toString().trim()
                if (passwordIngresada.isEmpty()) {
                    Toast.makeText(this, "La contraseña no puede estar vacía", Toast.LENGTH_SHORT).show()
                } else {
                    eliminarCuenta(passwordIngresada)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarCuenta(passwordIngresada: String) {
        val prefs = getSharedPreferences("usuario_sesion", MODE_PRIVATE)
        val idOrganizacion = prefs.getString("id_organizacion", null)
        val idCuidador = prefs.getString("id_usuario", null)

        if (idOrganizacion == null || idCuidador == null) {
            Toast.makeText(this, "Error al obtener datos de sesión", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val cuidadorRef = db.collection("organizacion")
            .document(idOrganizacion)
            .collection("cuidadores")
            .document(idCuidador)

        cuidadorRef.get().addOnSuccessListener { doc ->
            val passwordReal = doc.getString("password")
            if (passwordReal == passwordIngresada) {
                // PASO 1: Eliminar subcolecciones si las tienes
                borrarSubcolecciones(cuidadorRef) {
                    // PASO 2: Borrar documento del cuidador
                    cuidadorRef.delete().addOnSuccessListener {
                        prefs.edit().clear().apply()
                        Toast.makeText(this, "Cuenta eliminada correctamente", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }.addOnFailureListener {
                        Toast.makeText(this, "Error al eliminar la cuenta", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al verificar la cuenta", Toast.LENGTH_SHORT).show()
        }
    }


    private fun borrarSubcolecciones(docRef: com.google.firebase.firestore.DocumentReference, onComplete: () -> Unit) {
        val subcolecciones = listOf("pacientes")

        val db = FirebaseFirestore.getInstance()
        var pendientes = subcolecciones.size

        if (pendientes == 0) {
            onComplete()
            return
        }

        for (sub in subcolecciones) {
            docRef.collection(sub).get()
                .addOnSuccessListener { query ->
                    val batch = db.batch()
                    for (document in query.documents) {
                        batch.delete(document.reference)
                    }

                    batch.commit().addOnCompleteListener {
                        pendientes--
                        if (pendientes == 0) {
                            onComplete()
                        }
                    }
                }
                .addOnFailureListener {
                    pendientes--
                    if (pendientes == 0) {
                        onComplete()
                    }
                }
        }
    }


}
