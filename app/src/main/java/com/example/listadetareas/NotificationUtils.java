package com.example.listadetareas;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.preference.PreferenceManager;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class NotificationUtils {

    private static final int PERMISSION_REQUEST_NOTIFICATION = 123;
    static Boolean notified = false;

    public static void scheduleNotification(Context context, Task task) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("channel_id", "Channel Name", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static void showNotification(Context context, Task task) {
        if (!isNotificationPermissionGranted(context) && !notified) {
            requestNotificationPermission((Activity) context);
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "channel_id")
                .setContentTitle("¡Tienes una tarea para mañana!")
                .setContentText("La tarea '" + task.getTitle() + "' está programada para mañana")
                .setSmallIcon(R.drawable.ic_stat_name)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NOTIFICATION_POLICY) != PackageManager.PERMISSION_GRANTED) {
            // Si no tienes el permiso, solicítalo al usuario
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY}, PERMISSION_REQUEST_NOTIFICATION);
            return;
        }


        // Verificar si el correo ya ha sido enviado para esta tarea
        boolean correoEnviado = checkCorreoEnviado(context, task.getId());

        if (!correoEnviado) {
            notificationManager.notify((int) task.getId(), builder.build());
            new SendEmailTask(context, task).execute();
            markCorreoEnviado(context, task.getId()); // Marcar como correo enviado en la base de datos
        }
    }

    private static boolean checkCorreoEnviado(Context context, long taskId) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(DatabaseContract.TaskEntry.TABLE_NAME,
                new String[]{DatabaseContract.TaskEntry.COLUMN_CORREO_ENVIADO},
                DatabaseContract.TaskEntry.COLUMN_ID + " = ?",
                new String[]{String.valueOf(taskId)},
                null, null, null);

        boolean correoEnviado = false;
        if (cursor != null && cursor.moveToFirst()) {
            int correoEnviadoIndex = cursor.getColumnIndex(DatabaseContract.TaskEntry.COLUMN_CORREO_ENVIADO);
            int correoEnviadoValue = cursor.getInt(correoEnviadoIndex);
            correoEnviado = correoEnviadoValue == 1;
            cursor.close();
        }

        db.close();
        return correoEnviado;
    }

    private static void markCorreoEnviado(Context context, long taskId) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DatabaseContract.TaskEntry.COLUMN_CORREO_ENVIADO, 1);

        db.update(DatabaseContract.TaskEntry.TABLE_NAME,
                values,
                DatabaseContract.TaskEntry.COLUMN_ID + " = ?",
                new String[]{String.valueOf(taskId)});

        db.close();
    }

    public static boolean isNotificationPermissionGranted(Context context) {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    private static void requestNotificationPermission(Activity activity) {
        // Mostrar mensaje de explicación
        new AlertDialog.Builder(activity)
                .setTitle("Permiso de notificación")
                .setMessage("La aplicación necesita el permiso de notificación para mostrar las notificaciones de tareas.")
                .setPositiveButton("Ok", (dialog, which) -> {
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY}, PERMISSION_REQUEST_NOTIFICATION);
                })
                .show();
        notified = true;
    }

    static void sendEmailNotification(Context context, Task task) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String toEmail = preferences.getString("email_preference", "correo@example.com");
        String fromEmail = "tasktracker24@gmail.com";
        String password = context.getString(R.string.password);

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
                new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(fromEmail, password);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail, "TaskTracker"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("¡Tienes una tarea programada para mañana!");
            message.setText("La tarea '" + task.getTitle() + "' está programada para mañana.");

            Transport.send(message);

            System.out.println("Correo enviado exitosamente");

        } catch (MessagingException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
