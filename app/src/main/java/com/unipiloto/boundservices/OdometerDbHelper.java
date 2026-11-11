package com.unipiloto.boundservices;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class OdometerDbHelper extends SQLiteOpenHelper {

    private static final String NOMBRE_BD = "odometro.db";
    private static final int VERSION_BD = 1;

    public OdometerDbHelper(Context context) {
        super(context, NOMBRE_BD, null, VERSION_BD);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE RECORRIDO (_id INTEGER PRIMARY KEY AUTOINCREMENT, FECHA INTEGER NOT NULL, METROS REAL NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int antigua, int nueva) {
        db.execSQL("DROP TABLE IF EXISTS RECORRIDO");
        onCreate(db);
    }

    public void guardarRecorrido(double metros) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put("FECHA", System.currentTimeMillis());
        valores.put("METROS", metros);
        db.insert("RECORRIDO", null, valores);
    }

    public static class Recorrido {
        public final long fecha;
        public final double metros;

        public Recorrido(long fecha, double metros) {
            this.fecha = fecha;
            this.metros = metros;
        }
    }

    public List<Recorrido> listarRecorridos() {
        List<Recorrido> resultado = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT FECHA, METROS FROM RECORRIDO ORDER BY _id DESC", null)) {
            while (cursor.moveToNext()) {
                resultado.add(new Recorrido(cursor.getLong(0), cursor.getDouble(1)));
            }
        }
        return resultado;
    }

    public double totalMetros() {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT SUM(METROS) FROM RECORRIDO", null)) {
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                return cursor.getDouble(0);
            }
        }
        return 0.0;
    }
}
