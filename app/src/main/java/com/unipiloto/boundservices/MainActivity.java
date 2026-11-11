package com.unipiloto.boundservices;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int CODIGO_PERMISO_UBICACION = 42;

    private OdometerService odometerService;
    private boolean bound = false;
    private boolean recorriendo = false;

    private EditText etPrecision;
    private EditText etIntervalo;
    private TextView tvDistancia;
    private TextView tvUbicacion;
    private TextView tvEstado;
    private Button btnIniciar;
    private Button btnDetener;

    private OdometerDbHelper dbHelper;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable actualizarUi = new Runnable() {
        @Override
        public void run() {
            if (bound && odometerService != null) {
                tvDistancia.setText(String.format(Locale.getDefault(),
                        "Distancia recorrida: %.2f m", odometerService.getDistancia()));
                Location ubicacion = odometerService.getUbicacionActual();
                if (ubicacion != null) {
                    tvUbicacion.setText(String.format(Locale.getDefault(),
                            "Ubicación actual: %.6f, %.6f (precisión GPS: %.0f m)",
                            ubicacion.getLatitude(), ubicacion.getLongitude(), ubicacion.getAccuracy()));
                } else {
                    tvUbicacion.setText("Ubicación actual: esperando señal del GPS...");
                }
            }
            handler.postDelayed(this, 1000);
        }
    };

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            odometerService = ((OdometerService.OdometerBinder) binder).getOdometer();
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            odometerService = null;
            bound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new OdometerDbHelper(this);

        etPrecision = findViewById(R.id.et_precision);
        etIntervalo = findViewById(R.id.et_intervalo);
        tvDistancia = findViewById(R.id.tv_distancia);
        tvUbicacion = findViewById(R.id.tv_ubicacion);
        tvEstado = findViewById(R.id.tv_estado);
        btnIniciar = findViewById(R.id.btn_iniciar);
        btnDetener = findViewById(R.id.btn_detener);
        Button btnReporte = findViewById(R.id.btn_reporte);

        btnIniciar.setOnClickListener(view -> iniciarRecorrido());
        btnDetener.setOnClickListener(view -> detenerRecorrido());
        btnReporte.setOnClickListener(view -> mostrarReporte());

        actualizarEstadoBotones();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, OdometerService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        handler.post(actualizarUi);
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(actualizarUi);
        if (bound) {
            unbindService(connection);
            bound = false;
        }
    }

    private void iniciarRecorrido() {
        Float precision = leerPrecision();
        Long intervalo = leerIntervalo();
        if (precision == null || intervalo == null) {
            return;
        }
        if (!tienePermiso()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    CODIGO_PERMISO_UBICACION);
            return;
        }
        if (odometerService == null) {
            Toast.makeText(this, "Conectando con el servicio, intenta de nuevo.", Toast.LENGTH_SHORT).show();
            return;
        }
        odometerService.reiniciarDistancia();
        odometerService.iniciarActualizaciones(precision, intervalo);
        recorriendo = true;
        etPrecision.setEnabled(false);
        etIntervalo.setEnabled(false);
        tvEstado.setText("Recorrido en curso...");
        actualizarEstadoBotones();
    }

    private void detenerRecorrido() {
        if (odometerService == null) {
            return;
        }
        odometerService.detenerActualizaciones();
        double metros = odometerService.getDistancia();
        dbHelper.guardarRecorrido(metros);
        recorriendo = false;
        etPrecision.setEnabled(true);
        etIntervalo.setEnabled(true);
        tvEstado.setText(String.format(Locale.getDefault(), "Recorrido guardado: %.2f m", metros));
        actualizarEstadoBotones();
    }

    private void actualizarEstadoBotones() {
        btnIniciar.setEnabled(!recorriendo);
        btnDetener.setEnabled(recorriendo);
    }

    private Float leerPrecision() {
        String texto = etPrecision.getText().toString().trim().replace(',', '.');
        try {
            float valor = Float.parseFloat(texto);
            if (valor <= 0) {
                throw new NumberFormatException();
            }
            return valor;
        } catch (NumberFormatException e) {
            etPrecision.setError("Escribe un número positivo");
            return null;
        }
    }

    private Long leerIntervalo() {
        String texto = etIntervalo.getText().toString().trim();
        try {
            long valor = Long.parseLong(texto);
            if (valor <= 0) {
                throw new NumberFormatException();
            }
            return valor;
        } catch (NumberFormatException e) {
            etIntervalo.setError("Escribe segundos enteros positivos");
            return null;
        }
    }

    private boolean tienePermiso() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CODIGO_PERMISO_UBICACION) {
            if (tienePermiso()) {
                iniciarRecorrido();
            } else {
                Toast.makeText(this, "Se necesita el permiso de ubicación para registrar el recorrido.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void mostrarReporte() {
        List<OdometerDbHelper.Recorrido> recorridos = dbHelper.listarRecorridos();
        double total = dbHelper.totalMetros();
        StringBuilder mensaje = new StringBuilder();
        mensaje.append(String.format(Locale.getDefault(), "Total acumulado: %.2f m\n\n", total));
        if (recorridos.isEmpty()) {
            mensaje.append("Todavía no hay recorridos guardados.");
        } else {
            SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            for (OdometerDbHelper.Recorrido recorrido : recorridos) {
                mensaje.append(formato.format(new Date(recorrido.fecha)))
                        .append(" — ")
                        .append(String.format(Locale.getDefault(), "%.2f m", recorrido.metros))
                        .append("\n");
            }
        }
        new AlertDialog.Builder(this)
                .setTitle("Reporte consolidado")
                .setMessage(mensaje.toString())
                .setPositiveButton("Cerrar", null)
                .show();
    }
}
