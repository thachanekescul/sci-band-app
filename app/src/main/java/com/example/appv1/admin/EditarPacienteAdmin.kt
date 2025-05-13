package com.example.appv1.admin

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.appv1.R
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class EditarPacienteAdmin : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var etNombre: EditText
    private lateinit var etApellido: EditText
    private lateinit var etCelular: EditText
    private lateinit var etFechaNacimiento: EditText
    private lateinit var spCondicion: Spinner
    private lateinit var btnGuardar: Button
    private lateinit var btnEliminar: Button

    private lateinit var pacienteId: String
    private lateinit var cuidadorId: String
    private lateinit var organizacionId: String
    private lateinit var adminId: String

    private val condiciones = arrayOf("Ninguna", "Diabetes", "Hipertensión", "Cáncer", "Obesidad", "Otro")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_paciente_admin)

        db = FirebaseFirestore.getInstance()

        etNombre = findViewById(R.id.etNombre)
        etApellido = findViewById(R.id.etApellido)
        etCelular = findViewById(R.id.etCelular)
        etFechaNacimiento = findViewById(R.id.etFechaNacimiento)
        spCondicion = findViewById(R.id.spCondicion)
        btnGuardar = findViewById(R.id.btnGuardar)
        btnEliminar = findViewById(R.id.btnEliminar)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, condiciones)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCondicion.adapter = adapter

        etFechaNacimiento.setOnClickListener {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, y, m, d ->
                etFechaNacimiento.setText(String.format("%02d/%02d/%04d", d, m + 1, y))
            }, year, month, day).show()
        }

        // Recibir ambos IDs
        pacienteId = intent.getStringExtra("idPaciente") ?: ""
        cuidadorId = intent.getStringExtra("idCuidador") ?: ""

        val prefs = getSharedPreferences("usuario_sesion", Context.MODE_PRIVATE)
        organizacionId = prefs.getString("id_organizacion", "")!!
        adminId = prefs.getString("id_usuario", "")!!

        cargarDatosPaciente()

        btnGuardar.setOnClickListener {
            guardarCambios()
        }

        btnEliminar.setOnClickListener {
            mostrarDialogoConfirmacion()
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
                val celular = document.get("celular")?.toString() ?: ""
                etCelular.setText(celular)
                etFechaNacimiento.setText(document.getString("fecha_nacimiento") ?: "")

                val condicion = document.getString("condicion_cronica") ?: "Ninguna"
                val index = condiciones.indexOf(condicion)
                if (index >= 0) {
                    spCondicion.setSelection(index)
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al cargar datos del paciente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardarCambios() {
        val nuevoNombre = etNombre.text.toString().trim()
        val nuevoApellido = etApellido.text.toString().trim()
        val nuevoCelular = etCelular.text.toString().trim()
        val nuevaFechaNacimiento = etFechaNacimiento.text.toString().trim()
        val nuevaCondicion = spCondicion.selectedItem.toString()

        if (nuevoNombre.isEmpty() || nuevoApellido.isEmpty() || nuevoCelular.isEmpty() || nuevaFechaNacimiento.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val pacienteRef = db.collection("organizacion")
            .document(organizacionId)
            .collection("cuidadores")
            .document(cuidadorId)
            .collection("pacientes")
            .document(pacienteId)

        pacienteRef.update(
            mapOf(
                "nombre" to nuevoNombre,
                "apellido" to nuevoApellido,
                "celular" to nuevoCelular,
                "fecha_nacimiento" to nuevaFechaNacimiento,
                "condicion_cronica" to nuevaCondicion
            )
        ).addOnSuccessListener {
            Toast.makeText(this, "Datos actualizados exitosamente", Toast.LENGTH_LONG).show()
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "Error al actualizar paciente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarDialogoConfirmacion() {
        val editText = EditText(this)
        editText.hint = "Contraseña"
        editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD

        AlertDialog.Builder(this)
            .setTitle("Confirmar Eliminación")
            .setMessage("Ingrese su contraseña para eliminar este paciente.")
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
            .collection("admin")
            .document(adminId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val passwordReal = document.getString("password") ?: ""
                    if (passwordIngresada == passwordReal) {
                        eliminarPaciente()
                    } else {
                        Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al verificar contraseña", Toast.LENGTH_SHORT).show()
            }
    }

    private fun eliminarPaciente() {
        db.collection("organizacion")
            .document(organizacionId)
            .collection("cuidadores")
            .document(cuidadorId)
            .collection("pacientes")
            .document(pacienteId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Paciente eliminado exitosamente", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al eliminar paciente", Toast.LENGTH_SHORT).show()
            }
    }
}
