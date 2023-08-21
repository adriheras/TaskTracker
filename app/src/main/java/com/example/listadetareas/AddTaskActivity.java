package com.example.listadetareas;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddTaskActivity extends AppCompatActivity {
    private EditText editTextTitle;
    private EditText editTextDescription;
    private TextView textViewDate;
    private ImageView imageView;
    private String selectedDate;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        editTextTitle = findViewById(R.id.editTextTitle);
        editTextDescription = findViewById(R.id.editTextDescription);
        textViewDate = findViewById(R.id.textViewDate);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String currentDate = dateFormat.format(Calendar.getInstance().getTime());
        textViewDate.setText(currentDate);
        imageView = findViewById(R.id.imageView);
        Button buttonSave = findViewById(R.id.buttonSave);
        Button buttonAddImage = findViewById(R.id.buttonAddImage);

        buttonSave.setOnClickListener(v -> saveTask());

        buttonAddImage.setOnClickListener(v -> openImagePicker());

        Button buttonPickDate = findViewById(R.id.buttonPickDate);
        buttonPickDate.setOnClickListener(v -> showDatePickerDialog());
    }

    private void showDatePickerDialog() {
        // Obtener la fecha actual como valores para inicializar el DatePicker
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        // Crear una instancia de DatePickerDialog y configurarla con la fecha actual
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                AddTaskActivity.this,
                (view, year1, month1, dayOfMonth1) -> {
                    // Obtener la fecha seleccionada del DatePicker
                    selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth1, month1 + 1, year1);

                    // Mostrar la fecha seleccionada en el TextView
                    textViewDate.setText(selectedDate);
                },
                year, month, dayOfMonth);

        // Mostrar el diálogo del DatePicker
        datePickerDialog.show();
    }

    private void openImagePicker() {
        // Crear un Intent para abrir el selector de imágenes
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");

        // Crear un Intent adicional para abrir la cámara
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Crear un Intent para mostrar un diálogo de selección entre la galería y la cámara
        Intent chooserIntent = Intent.createChooser(intent, "Select Image");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { cameraIntent });

        // Iniciar la actividad y esperar el resultado
        startActivityForResult(chooserIntent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
                if (data.getData() != null) {
                    // El usuario eligió una imagen de la galería
                    imageUri = data.getData();
                    imageView.setImageURI(imageUri);
                } else if (data.getExtras() != null) {
                    // El usuario tomó una foto con la cámara
                    Bitmap photo = (Bitmap) data.getExtras().get("data");
                    imageUri = bitmapToUri(photo);
                    imageView.setImageBitmap(photo);
                }
        }
    }

    private void saveTask() {
        String title = editTextTitle.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();
        String date = textViewDate.getText().toString().trim();

        // Verificar si el título o la descripción están vacíos
        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(AddTaskActivity.this, "Please fill in the title and description", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar si se ha seleccionado fecha
        if (date.equals("No selected date")) {
            Toast.makeText(AddTaskActivity.this, "Please select a date", Toast.LENGTH_SHORT).show();
            return;
        }

        byte[] compressedImage = compressImage(imageUri);

        // Crear una nueva instancia de Task con los datos ingresados
        Task newTask = new Task(title, description, date, compressedImage);

        // Guardar la nueva tarea en la base de datos
        try (DatabaseHelper databaseHelper = new DatabaseHelper(AddTaskActivity.this)) {
            databaseHelper.insertTask(newTask);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent resultIntent = new Intent();
        setResult(RESULT_OK, resultIntent);

        Toast.makeText(AddTaskActivity.this, "Task created correctly", Toast.LENGTH_SHORT).show();

        finish(); // Cerrar la Activity actual y volver a la anterior
    }

    private byte[] compressImage(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);

            int maxWidth = 800;
            int maxHeight = 800;

            int width = originalBitmap.getWidth();
            int height = originalBitmap.getHeight();

            float scale = Math.min((float) maxWidth / width, (float) maxHeight / height);

            int newWidth = Math.round(scale * width);
            int newHeight = Math.round(scale * height);

            Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);

            return outputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Uri bitmapToUri(Bitmap bitmap) {
        // Guardar el Bitmap en un archivo en el almacenamiento
        File file = new File(getCacheDir(), "image.jpg");
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Obtener la Uri del archivo guardado
        return Uri.fromFile(file);
    }



}

