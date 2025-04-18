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
        imgPulsera.startAnimation(fadeIn);  // La pulsera aparece primero
        imgfondo.startAnimation(fadeInDelay);  // El texto "SCI-BAND" aparece después
        tvNombreLogo.startAnimation(fadeIn);  // El texto "SCI-BAND" aparece después
        tvWelcome.startAnimation(fadeInDelay);  // El texto "Quien es usted?" aparece después
        btnPaciente.startAnimation(fadeInDelay);  // El botón "Paciente" aparece después
        btnCuidador.startAnimation(fadeInDelay);  // El botón "Trabajador" aparece después

        circleAnimationView.startAnimation(1500);  // Radio máximo del círculo
        new Handler().postDelayed(() -> {
            mainLayout.setBackgroundColor(Color.WHITE);  // Cambiar el fondo a blanco
            tvNombreLogo.setTextColor(Color.WHITE);  // Cambiar el texto a blanco
        }, 2000);  // Retraso de 1.5 segundos



        //Se accede a la pantalla donde el paciente muestra el QR al cuidador
        btnPaciente.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PacienteQR.class);
                startActivity(intent);
            }
        });

        // Se accede a la pantalla donde el usuario ingresa sus cosas esas
        btnCuidador.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CuidadorLogin.class);
                startActivity(intent);
            }
        });



    }


}