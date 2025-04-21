package com.example.appv1;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.widget.TextView;
import android.widget.Button;
import android.content.Intent;
import android.view.View;

import android.graphics.Color;


import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.appv1.cuidador.MainActivityCuidador;
import com.example.appv1.logins.CuidadorLogin;
import com.example.appv1.paciente.PacienteQR;



public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ConstraintLayout mainLayout = findViewById(R.id.main);
        ImageView imgfondo = findViewById(R.id.imgfondo);
        ImageView imgPulsera = findViewById(R.id.imgpulsera);
        TextView tvNombreLogo = findViewById(R.id.tvNombreLogo);
        TextView tvWelcome = findViewById(R.id.TvWelcome);
        Button btnPaciente = findViewById(R.id.btnPaciente);
        Button btnCuidador = findViewById(R.id.btncuidador);
        CircleAnimationView circleAnimationView = findViewById(R.id.circleAnimationView);

        // Cargar animaciones
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation fadeInDelay = AnimationUtils.loadAnimation(this, R.anim.fade_in_delay);

        // Aplicar animaciones
        imgPulsera.startAnimation(fadeIn);
        imgfondo.startAnimation(fadeInDelay);
        tvNombreLogo.startAnimation(fadeIn);
        tvWelcome.startAnimation(fadeInDelay);
        btnPaciente.startAnimation(fadeInDelay);
        btnCuidador.startAnimation(fadeInDelay);
        circleAnimationView.startAnimation(1500);

        new Handler().postDelayed(() -> {
            mainLayout.setBackgroundColor(Color.WHITE);
            tvNombreLogo.setTextColor(Color.WHITE);
        }, 2000);

        // Botón para el paciente
        btnPaciente.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PacienteQR.class);
            startActivity(intent);
        });

        // Botón para el cuidador
        btnCuidador.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CuidadorLogin.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Verificar sesión de cuidador
        String idCuidador = getSharedPreferences("cuidador_sesion", MODE_PRIVATE).getString("id_cuidador", null);
        String idOrganizacion = getSharedPreferences("cuidador_sesion", MODE_PRIVATE).getString("id_organizacion", null);

        if (idCuidador != null && idOrganizacion != null) {
            // Si ya hay sesión, ir directamente al panel de cuidador
            Intent intent = new Intent(MainActivity.this, MainActivityCuidador.class);
            startActivity(intent);
            finish();
        }
    }
}
