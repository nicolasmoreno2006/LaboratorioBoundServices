# Laboratorio: Bound Services

Aplicación Android (Java) que implementa un servicio enlazado (`OdometerService`,
extiende `Service`) siguiendo la guía del laboratorio, para registrar y mostrar la
distancia recorrida en metros por un estudiante que se desplaza a una sesión de
tutoría presencial.

## Funcionalidad base (guía)

`MainActivity` se enlaza (`bindService`) a `OdometerService` mientras está visible
(`onStart`/`onStop`), y usa `LocationManager` para recibir actualizaciones de
ubicación y calcular la distancia recorrida con `Location.distanceTo()`.

## Variables de configuración (requisito del laboratorio)

Capturadas en la interfaz antes de iniciar el recorrido:

- **Precisión del movimiento (metros):** distancia mínima entre ubicaciones para
  que `requestLocationUpdates` dispare una nueva lectura.
- **Tiempo de actualización (segundos):** intervalo mínimo entre lecturas de GPS.
- **Ubicación actual del estudiante:** se muestra en pantalla (latitud, longitud
  y precisión del GPS) y se actualiza cada segundo mientras el servicio está
  enlazado.

## Persistencia y reporte consolidado

Al pulsar "Detener y guardar recorrido", la distancia acumulada se guarda en una
base de datos SQLite (`OdometerDbHelper`, tabla `RECORRIDO`). El botón "Reporte
consolidado" muestra el total acumulado y el detalle de cada recorrido guardado.

## Ejecutar

1. Abrir esta carpeta en Android Studio.
2. Sincronizar Gradle y ejecutar `app` en un emulador o dispositivo con Android 7.0
   (API 24) o superior. En el emulador, usa los controles de ubicación extendidos
   para simular desplazamiento.
3. Escribir la precisión y el intervalo, pulsar "Iniciar recorrido", simular
   movimiento y luego "Detener y guardar recorrido".
