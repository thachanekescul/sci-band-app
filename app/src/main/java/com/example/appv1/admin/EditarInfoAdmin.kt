package com.example.appv1.admin

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.appv1.R
import com.google.firebase.firestore.FirebaseFirestore

class EditarInfoAdmin : AppCompatActivity() {

    private lateinit var etNombre: EditText
    private lateinit var etApellido: EditText
    private lateinit var etCelular: EditText
    private lateinit var etEmail: EditText
    private lateinit var btnGuardar: AppCompatButton

    private lateinit var db: FirebaseFirestore
    private lateinit var idOrganizacion: String
    private lateinit var idAdmin: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_info_admin)

        // Inicializamos los campos
        etNombre = findViewById(R.id.etNombre)
        etApellido = findViewById(R.id.etApellido)
        etCelular = findViewById(R.id.etCelular)
        etEmail = findViewById(R.id.etEmail)
        btnGuardar = findViewById(R.id.btnGuardar)

        db = FirebaseFirestore.getInstance()

        // Obtener el ID de la organización y el admin desde SharedPreferences
        val prefs = getSharedPreferences("usuario_sesion", MODE_PRIVATE)
        idOrganizacion = prefs.getString("id_organizacion", "") ?: ""
        idAdmin = prefs.getString("id_usuario", "") ?: ""

        // Cargar los datos del admin
        cargarDatosAdmin()

        // Guardar los cambios al hacer clic en "Guardar"
        btnGuardar.setOnClickListener {
            guardarCambios()
        }
    }

    private fun cargarDatosAdmin() {
        db.collection("organizacion")
            .document(idOrganizacion)
            .collection("administradores")
            .document(idAdmin)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    etNombre.setText(document.getString("nombre"))
                    etApellido.setText(document.getString("apellido"))
                    etCelular.setText(document.getString("celular"))
                    etEmail.setText(document.getString("email"))
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar los datos", Toast.LENGTH_SHORT).show()
            }
    }

    private fun guardarCambios() {
        val nombre = etNombre.text.toString().trim()
        val apellido = etApellido.text.toString().trim()
        val celular = etCelular.text.toString().trim()
        val email = etEmail.text.toString().trim()

        if (nombre.isEmpty() || apellido.isEmpty() || celular.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Actualizar los datos del admin en Firestore
        val datosAdmin = hashMapOf(
            "nombre" to nombre,
            "apellido" to apellido,
            "celular" to celular,
            "email" to email
        )

        db.collection("organizacion")
            .document(idOrganizacion)
            .collection("administradores")
            .document(idAdmin)
            .update(datosAdmin as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(this, "Datos actualizados con éxito", Toast.LENGTH_SHORT).show()
                finish()  // Cierra la actividad después de actualizar
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar los datos", Toast.LENGTH_SHORT).show()
            }
    }
}
