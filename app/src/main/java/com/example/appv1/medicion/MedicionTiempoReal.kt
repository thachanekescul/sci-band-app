package com.example.appv1.medicion



import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.appv1.R
import com.google.firebase.firestore.FirebaseFirestore

class MedicionTiempoReal : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var txtTemperatura: TextView
    private lateinit var txtPulso: TextView
    private lateinit var txtOxigeno: TextView

    private lateinit var pacienteId: String // ID del paciente, pasado desde la vista anterior

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medicion_tiempo_real)

        // Inicializar los elementos de la vista
        txtTemperatura = findViewById(R.id.temperatura)
        txtPulso = findViewById(R.id.hrtpulso)
        txtOxigeno = findViewById(R.id.oxigeno)

        // Obtener el ID del paciente desde el Intent (el ID del paciente debe ser pasado desde la vista anterior)
        pacienteId = intent.getStringExtra("idPaciente") ?: ""

        db = FirebaseFirestore.getInstance()

        // Comenzamos a escuchar los cambios en Firestore en tiempo real
        escucharDatosPaciente(pacienteId)
    }

    private fun escucharDatosPaciente(pacienteId: String) {
        db.collection("ultimos-datos")
            .document(pacienteId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    // Si hay un error, lo mostramos en un Toast o en la UI
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    // Si los datos del paciente están disponibles, actualizamos la UI
                    val oxigeno = snapshot.getDouble("o")?.toString() ?: "0"
                    val pulso = snapshot.getDouble("p")?.toString() ?: "0"
                    val temperatura = snapshot.getDouble("t")?.toString() ?: "0"

                    // Actualizamos los TextViews en la UI
                    txtOxigeno.text = "$oxigeno %"
                    txtPulso.text = "$pulso bpm"
                    txtTemperatura.text = "$temperatura °C"
                }
            }
    }
}
