package com.example.appv1.admin

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.appv1.R
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate

class HomeAdmin : Fragment() {
    private lateinit var tvLlamadosTotales: TextView
    private lateinit var tvLlamadosAsistidos: TextView
    private lateinit var tvCuidadorMenosRecibidos: TextView
    private lateinit var tvCuidadorMenosAsistidos: TextView
    private lateinit var tvrarezaPacientes: TextView
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home_admin, container, false)

        // Inicializar vistas
        tvLlamadosTotales = view.findViewById(R.id.tvLlamadosTotales)
        tvLlamadosAsistidos = view.findViewById(R.id.tvLlamadosAsistidos)
        tvCuidadorMenosRecibidos = view.findViewById(R.id.tvCuidadorMenosRecibidos)
        tvCuidadorMenosAsistidos = view.findViewById(R.id.tvCuidadorMenosAsistidos)
        tvrarezaPacientes=view.findViewById(R.id.txtRarezaPac)

        val prefs = requireContext().getSharedPreferences("usuario_sesion", Context.MODE_PRIVATE)
        val orgid = prefs.getString("id_organizacion", null)


        val today = LocalDate.now()
        val dayId = String.format("%02d", today.dayOfMonth)

        // Leer datos de Firestore
        if (orgid != null) {
            db.collection("organizacion").document(orgid)
                .collection("resumen_diario").document(dayId)
                .addSnapshotListener { snapshot, e ->
                    if (e != null || snapshot == null || !snapshot.exists()) {
                        return@addSnapshotListener
                    }
                    val llamadosTotales = snapshot.getLong("llamadosTotalesHoy")?.toString() ?: "0"
                    val asistenciasTotales = snapshot.getLong("asistenciasTotalesHoy")?.toString() ?: "0"
                    val cuidadorMenosLlamados = snapshot.getString("cuidadorMenosLlamados") ?: "N/A"
                    val cuidadorMenosAsistencias = snapshot.getString("cuidadorMenosAsistencias") ?: "N/A"
                    val rarezaPacientes=snapshot.getString("rareza")?: "Nada raro por el momento"
                    tvrarezaPacientes.text= rarezaPacientes
                    tvLlamadosTotales.text = "$llamadosTotales hoy"
                    tvLlamadosAsistidos.text = "$asistenciasTotales hoy"
                    tvCuidadorMenosRecibidos.text = cuidadorMenosLlamados
                    tvCuidadorMenosAsistidos.text = cuidadorMenosAsistencias
                }
        }

        return view
    }
}