package com.example.appv1.cuidador

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.appv1.R
import com.example.appv1.cuidador.datos.AlertasForegroundService
import com.example.appv1.cuidador.hist.HistorialCUIDFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivityCuidador : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_cuidador)
        val serviceIntent = Intent(this, AlertasForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.NavegacionCuidador)

        // Cargar por defecto el fragmento de "Casa"
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeCuidadorFragment())
                .commit()
        }

        bottomNavigationView.setOnNavigationItemSelectedListener(object :
            BottomNavigationView.OnNavigationItemSelectedListener {
            var nav_home: Int = R.id.nav_home
            var nav_pacientes: Int = R.id.nav_pacientes
            var nav_historial: Int = R.id.nav_historial


            override fun onNavigationItemSelected(item: MenuItem): Boolean {
                var selectedFragment: Fragment? = null

                if (item.itemId == nav_home) {
                    selectedFragment = HomeCuidadorFragment()
                } else if (item.itemId == nav_pacientes) {
                    selectedFragment = PacientesCUIDFragment()
                } else if (item.itemId == nav_historial) {
                    selectedFragment = HistorialCUIDFragment()
                }



                if (selectedFragment != null) {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit()
                }


                return true
            }
        })
    }
}