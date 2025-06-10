
package com.example.appv1.charts

import android.content.Context
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.db.williamchart.view.LineChartView
import com.example.appv1.R
import com.google.android.gms.tasks.Tasks
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class ChartCalls(
    private val context: Context,
    private val container: LinearLayout,
    private val organizacionId: String,
    private val cuidadorId: String,
    private val pacienteId: String,

    ) {
    private val db = FirebaseFirestore.getInstance()

    fun loadChart() {
        val layout = LayoutInflater.from(context)
            .inflate(R.layout.itr_grafica_calls, container, false) as LinearLayout
        container.addView(layout)

        val Mensaje = layout.findViewById<TextView>(R.id.txtNombremsg)
        val chart = layout.findViewById<LineChartView>(R.id.maiamimeloconfirmo)
        val btn1Dia = layout.findViewById<MaterialButton>(R.id.btn1Dia)
        val btn15Dias = layout.findViewById<MaterialButton>(R.id.btn15Dias)
        val btn1Mes = layout.findViewById<MaterialButton>(R.id.btn1Mes)




        chart.setOnTouchListener { _, _ -> true }

        Mensaje.text= "Llamados Mandados"
        fun updateChart(days: Int, ids: List<String>) {

            val collectionRef = db.collection(
                "organizacion/$organizacionId/cuidadores/$cuidadorId/pacientes/$pacienteId/llamados"
            )

            val tasks = ids.map { id -> collectionRef.document(id).get() }

            Tasks.whenAllSuccess<DocumentSnapshot>(tasks)
                .addOnSuccessListener { results ->
                    val mapaOrdenado = linkedMapOf<String, Float>()
                    for ((index, id) in ids.withIndex()) {
                        val doc = results[index]
                        val valor = doc.getLong("llamado")?.toFloat()
                        if (valor != null) {
                            mapaOrdenado[id] = valor
                        }
                    }

                    if (mapaOrdenado.isEmpty()) {
                        Toast.makeText(context, "Aún no hay datos que mostrar", Toast.LENGTH_SHORT).show()
                    } else {
                        chart.show(mapaOrdenado)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error al cargar datos", Toast.LENGTH_SHORT).show()
                }
        }
        btn1Dia.setOnClickListener { updateChart(5, listOf("01", "02", "03", "05")) }
        btn15Dias.setOnClickListener { updateChart(15, listOf("01", "03", "07", "15")) }
        btn1Mes.setOnClickListener { updateChart(30, listOf("01", "10", "20", "30")) }

        btn1Dia.performClick()
    }
}
