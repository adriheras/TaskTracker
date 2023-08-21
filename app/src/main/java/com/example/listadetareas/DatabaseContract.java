package com.example.listadetareas;

import android.provider.BaseColumns;

public class DatabaseContract {

    private DatabaseContract() {
        // Constructor privado para evitar instanciación accidental
    }

    public static class TaskEntry implements BaseColumns {

        public static final String TABLE_NAME = "tasks";

        public static final String COLUMN_ID = "id";

        public static final String COLUMN_CORREO_ENVIADO = "correo_enviado";
    }
}

