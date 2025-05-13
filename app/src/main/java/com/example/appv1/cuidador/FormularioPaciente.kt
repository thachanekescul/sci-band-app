package com.example.appv1.cuidador

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.appv1.R
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class FormularioPaciente : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var etNombre: EditText
    private lateinit var etApellido: EditText
    private lateinit var etCelular: EditText
    private lateinit var etFechaNacimiento: EditText
    private lateinit var spCondicion: Spinner
    private lateinit var btnRegistro: Button

    private lateinit var codigoQR: String
    private val condiciones = arrayOf("Ninguna", "Diabetes", "Hipertensión", "Cáncer", "Obesidad", "Otro")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_formulario_paciente)

        db = FirebaseFirestore.getInstance()

        // Inicializar vistas
        etNombre = findViewById(R.id.etNombre)
        etApellido = findViewById(R.id.etApellido)
        etCelular = findViewById(R.id.etCelular)
        etFechaNacimiento = findViewById(R.id.etFechaNacimiento)
        spCondicion = findViewById(R.id.spCondicion)
        btnRegistro = findViewById(R.id.btnRegistro)

        // Obtener código QR desde el intent
        codigoQR = intent.getStringExtra("codigo_qr") ?: ""

        // Configurar spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, condiciones)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCondicion.adapter = adapter

        // Selector de fecha
        etFechaNacimiento.setOnClickListener {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, y, m, d ->
                etFechaNacimiento.setText(String.format("%02d/%02d/%04d", d, m + 1, y))
            }, year, month, day).show()
        }

        btnRegistro.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val apellido = etApellido.text.toString().trim()
            val celular = etCelular.text.toString().trim()
            val fechaNac = etFechaNacimiento.text.toString().trim()
            val condicion = spCondicion.selectedItem.toString()

            if (nombre.isEmpty() || apellido.isEmpty() || celular.isEmpty() || fechaNac.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val pacienteData = hashMapOf(
                "nombre" to nombre,
                "apellido" to apellido,
                "celular" to celular,
                "fecha_nacimiento" to fechaNac,
                "condicion_cronica" to condicion,
                "registrado" to true
            )

            // ✅ Obtener código de organización desde SharedPreferences
            val prefs = getSharedPreferences("usuario_sesion", MODE_PRIVATE)
            val orgCodigo = prefs.getString("id_organizacion", null) ?: ""
            val idCuidador = prefs.getString("id_usuario", null) ?: ""

            db.collection("organizacion")
                .document(orgCodigo)
                .collection("cuidadores") // 🔥 YA BIEN
                .document(idCuidador)
                .collection("pacientes")
                .document(codigoQR)
                .set(pacienteData)
                .addOnSuccessListener {
                    db.collection("codigos_espera").document(codigoQR).delete()

                    Toast.makeText(this, "Paciente registrado exitosamente", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, MainActivityCuidador::class.java))
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al registrar paciente", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
