package com.example.appv1.paciente;
import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.appv1.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class HomePaciente extends AppCompatActivity {
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket btSocket;
    private InputStream inputStream;
    private Handler handler;

    private TextView temperaturaView, pulsoView, oxigenoView, estadoConexionView;

    private final String DEVICE_NAME = "ESP32_HealthMonitor"; // Nombre del Bluetooth del ESP32
    private final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final int REQUEST_BLUETOOTH_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_paciente); // Usa la vista que pasaste

        temperaturaView = findViewById(R.id.temperatura);
        pulsoView = findViewById(R.id.hrtpulso);
        oxigenoView = findViewById(R.id.oxigeno);
        estadoConexionView = findViewById(R.id.txtEstadoConexion);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        handler = new Handler(Looper.getMainLooper());

        // Pedir permisos si es necesario
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestBluetoothPermissions();
        } else {
            connectToESP32();
        }
    }

    private void requestBluetoothPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSION);
        } else {
            connectToESP32(); // Si ya tiene permisos, conectar al ESP32
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                connectToESP32();
            } else {
                Toast.makeText(this, "Permiso de Bluetooth denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void connectToESP32() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth no disponible o apagado", Toast.LENGTH_SHORT).show();
            estadoConexionView.setText("Desconectado");
            estadoConexionView.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            return;
        }

        for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
            if (device.getName().equals(DEVICE_NAME)) {
                try {
                    btSocket = device.createRfcommSocketToServiceRecord(BT_UUID);
                    btSocket.connect();
                    inputStream = btSocket.getInputStream();
                    estadoConexionView.setText("Conectado");
                    estadoConexionView.setTextColor(getResources().getColor(android.R.color.holo_green_light));
                    readData();
                    return;
                } catch (IOException e) {
                    Toast.makeText(this, "Error al conectar", Toast.LENGTH_SHORT).show();
                    estadoConexionView.setText("Desconectado");
                    estadoConexionView.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                }
            }
        }
    }

    private void readData() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    if (inputStream.available() > 0) {
                        bytes = inputStream.read(buffer);
                        String data = new String(buffer, 0, bytes);
                        updateUI(data);
                    }
                } catch (IOException e) {
                    handler.post(() -> {
                        estadoConexionView.setText("Desconectado");
                        estadoConexionView.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                    });
                    break;
                }
            }
        }).start();
    }

    private void updateUI(String data) {
        handler.post(() -> {
            try {
                String[] values = data.split(" ");

                    temperaturaView.setText(values[0]); // Ej: Temp:36.5
                    pulsoView.setText(values[1]); // Ej: HR:78
                    oxigenoView.setText(values[2]); // Ej: SpO2:98


            } catch (Exception e) {
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (btSocket != null) {
                btSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
