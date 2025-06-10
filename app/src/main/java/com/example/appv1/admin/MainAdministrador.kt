package com.example.appv1.admin

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.appv1.R
import com.example.appv1.ui.admin.HistorialAdminFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainAdministrador : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_administrador)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.NavegacionAdmin)

        // Cargar por defecto el fragmento de "Casa"
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeAdmin())
                .commit()
        }

        bottomNavigationView.setOnNavigationItemSelectedListener(object :
            BottomNavigationView.OnNavigationItemSelectedListener {
            var nav_home: Int = R.id.nav_home
            var nav_grupos: Int = R.id.nav_grupos
            var nav_historial: Int = R.id.nav_historial


            override fun onNavigationItemSelected(item: MenuItem): Boolean {
                var selectedFragment: Fragment? = null

                if (item.itemId == nav_home) {
                    selectedFragment = HomeAdmin()
                } else if (item.itemId == nav_grupos) {
                    selectedFragment = GruposAdminFragment()
                } else if (item.itemId == nav_historial) {
                    selectedFragment = HistorialAdminFragment()
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