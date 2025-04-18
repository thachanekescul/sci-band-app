package com.example.appv1.registro
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.appv1.R
import android.app.DatePickerDialog
import android.content.Intent
import android.widget.*
import java.util.*

class RegistroDeLaOrg : AppCompatActivity() {

    private lateinit var etNombre: EditText
    private lateinit var etDireccion: EditText
    private lateinit var etCelular: EditText
    private lateinit var etFechaFundacion: EditText
    private lateinit var btnRegistro: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_de_la_org)

        // Referencias
        etNombre = findViewById(R.id.etNombreORG)
        etDireccion = findViewById(R.id.etDireccionORG) // Este lo puedes renombrar a etDireccion en el XML
        etCelular = findViewById(R.id.etCelORG)
        etFechaFundacion = findViewById(R.id.etFechafundacion)
        btnRegistro = findViewById(R.id.btnRegistro)

        // DatePicker para fecha de fundación
        etFechaFundacion.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(this, { _, y, m, d ->
                etFechaFundacion.setText(String.format("%02d/%02d/%04d", d, m + 1, y))
            }, year, month, day)

            datePicker.show()
        }

        // Validar y continuar
        btnRegistro.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val direccion = etDireccion.text.toString().trim()
            val celular = etCelular.text.toString().trim()
            val fechaFundacion = etFechaFundacion.text.toString().trim()

            if (nombre.isEmpty() || direccion.isEmpty() || celular.isEmpty() || fechaFundacion.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }



            // Enviar a RegistroAdmin
            val intent = Intent(this, RegistroAdmin::class.java).apply {
                putExtra("nombre_org", nombre)
                putExtra("direccion", direccion)
                putExtra("celular", celular)
                putExtra("fecha_fundacion", fechaFundacion)
            }
            startActivity(intent)
        }
    }
}