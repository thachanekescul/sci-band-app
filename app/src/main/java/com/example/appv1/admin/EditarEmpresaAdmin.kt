package com.example.appv1.admin

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.appv1.R
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class EditarEmpresaAdmin : AppCompatActivity() {

    private lateinit var etNombreEmpresa: EditText
    private lateinit var etFechaFundacion: EditText
    private lateinit var etCelular: EditText
    private lateinit var etDireccionORG: EditText
    private lateinit var btnGuardar: AppCompatButton

    private lateinit var db: FirebaseFirestore
    private lateinit var idOrganizacion: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_empresa_admin)

        // Inicializamos las vistas
        etNombreEmpresa = findViewById(R.id.etNombreEmpresa)
        etFechaFundacion = findViewById(R.id.etFechafundacion)
        etCelular = findViewById(R.id.et)
        etDireccionORG = findViewById(R.id.etDireccionORG)
        btnGuardar = findViewById(R.id.btnGuardar)

        db = FirebaseFirestore.getInstance()


        val prefs = getSharedPreferences("usuario_sesion", MODE_PRIVATE)
        idOrganizacion = prefs.getString("id_organizacion", "") ?: ""

        // Cargar los datos de la empresa
        cargarDatosEmpresa()

        // Configurar DatePickerDialog para la fecha de fundación
        etFechaFundacion.setOnClickListener {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            // Muestra el DatePickerDialog
            DatePickerDialog(this, { _, y, m, d ->
                val date = "$d/${m + 1}/$y"  // Formato d/m/a
                etFechaFundacion.setText(date)  // Seteamos el texto de la fecha seleccionada
            }, year, month, day).show()
        }

        // Guardar los cambios al hacer clic en "Guardar"
        btnGuardar.setOnClickListener {
            guardarCambios()
        }
    }

    private fun cargarDatosEmpresa() {
        db.collection("organizacion")
            .document(idOrganizacion)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    etNombreEmpresa.setText(document.getString("nombre"))
                    etDireccionORG.setText(document.getString("direccion"))
                    etFechaFundacion.setText(document.getString("fecha_fundacion"))
                    etCelular.setText(document.getString("celular"))
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar los datos de la empresa", Toast.LENGTH_SHORT).show()
            }
    }

    private fun guardarCambios() {
        val nombreEmpresa = etNombreEmpresa.text.toString().trim()
        val direccion = etDireccionORG.text.toString().trim()
        val fechaFundacion = etFechaFundacion.text.toString().trim()
        val celular = etCelular.text.toString().trim()

        if (nombreEmpresa.isEmpty() || direccion.isEmpty() || fechaFundacion.isEmpty() || celular.isEmpty()) {
            Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val datosEmpresa = hashMapOf(
            "nombre" to nombreEmpresa,
            "direccion" to direccion,
            "fecha_fundacion" to fechaFundacion,
            "celular" to celular
        )

        db.collection("organizacion")
            .document(idOrganizacion)
            .update(datosEmpresa as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(this, "Datos de la empresa actualizados con éxito", Toast.LENGTH_SHORT).show()
                finish() // Cierra la actividad después de actualizar
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar los datos de la empresa", Toast.LENGTH_SHORT).show()
            }
    }
}
