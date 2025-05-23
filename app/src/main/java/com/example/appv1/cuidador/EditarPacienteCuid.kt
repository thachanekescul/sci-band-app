package com.example.appv1.cuidador

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.appv1.R
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class EditarPacienteCuid : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var etNombre: EditText
    private lateinit var etApellido: EditText
    private lateinit var etCelular: EditText
    private lateinit var etFechaNacimiento: EditText
    private lateinit var spCondicion: Spinner
    private lateinit var btnGuardar: Button

    private lateinit var pacienteId: String
    private lateinit var cuidadorId: String
    private lateinit var organizacionId: String

    private val condiciones = arrayOf("Ninguna", "Diabetes", "Hipertensión", "Cáncer", "Obesidad", "Otro")

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
                etNombre.setText(document.getString("nombre"))
                etApellido.setText(document.getString("apellido"))
                etCelular.setText(document.getString("celular"))
                etFechaNacimiento.setText(document.getString("fecha_nacimiento"))

                val condicion = document.getString("condicion_cronica") ?: "Ninguna"
                val index = condiciones.indexOf(condicion)
                if (index >= 0) {
                    spCondicion.setSelection(index)
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show()
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

}
