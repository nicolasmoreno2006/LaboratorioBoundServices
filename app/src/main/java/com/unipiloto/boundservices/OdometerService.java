package com.unipiloto.boundservices;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import androidx.core.content.ContextCompat;

public class OdometerService extends Service {

    private final IBinder binder = new OdometerBinder();

    private LocationManager locationManager;
    private LocationListener listener;

    private double distanciaMetros = 0.0;
    private Location ultimaUbicacion;
    private Location ubicacionActual;

    public class OdometerBinder extends Binder {
        OdometerService getOdometer() {
            return OdometerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void iniciarActualizaciones(float precisionMetros, long intervaloSegundos) {
        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        }
        if (listener == null) {
            listener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    ubicacionActual = location;
                    if (ultimaUbicacion != null) {
                        distanciaMetros += ultimaUbicacion.distanceTo(location);
                    }
                    ultimaUbicacion = location;
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                @Override
                public void onProviderEnabled(String provider) {
                }

                @Override
                public void onProviderDisabled(String provider) {
                }
            };
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        String proveedor = mejorProveedor();
        if (proveedor != null) {
            locationManager.requestLocationUpdates(proveedor, intervaloSegundos * 1000L, precisionMetros, listener);
        }
    }

    public void detenerActualizaciones() {
        if (locationManager != null && listener != null
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(listener);
        }
        ultimaUbicacion = null;
    }

    public void reiniciarDistancia() {
        distanciaMetros = 0.0;
    }

    public double getDistancia() {
        return distanciaMetros;
    }

    public Location getUbicacionActual() {
        return ubicacionActual;
    }

    private String mejorProveedor() {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return LocationManager.GPS_PROVIDER;
        }
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            return LocationManager.NETWORK_PROVIDER;
        }
        return null;
    }

    @Override
    public void onDestroy() {
        detenerActualizaciones();
        super.onDestroy();
    }
}
