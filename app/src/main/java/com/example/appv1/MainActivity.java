package com.example.appv1;
import android.content.SharedPreferences;
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

import com.example.appv1.admin.MainAdministrador;
import com.example.appv1.cuidador.HomeCuidadorFragment;
import com.example.appv1.cuidador.MainActivityCuidador;
import com.example.appv1.logins.CuidadorLogin;
import com.example.appv1.paciente.HomePaciente;
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
        SharedPreferences prefs = getSharedPreferences("usuario_sesion", MODE_PRIVATE);
        String tipoUsuario = prefs.getString("tipo_usuario", null);
        String idUsuario = prefs.getString("id_usuario", null);
        String idOrganizacion = prefs.getString("id_organizacion", null);

        if ("cuidador".equals(tipoUsuario) && idUsuario != null && idOrganizacion != null) {
            Intent intent = new Intent(MainActivity.this, MainActivityCuidador.class);
            startActivity(intent);
            finish();
        }

        if ("admin".equals(tipoUsuario) && idUsuario != null && idOrganizacion != null) {
            Intent intent = new Intent(MainActivity.this, MainAdministrador.class);
            startActivity(intent);
            finish();
        }


    }
}
