package com.example.listadetareas;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_ADD_TASK = 1;
    private static final int REQUEST_CODE_IMPORT_TASKS = 2;
    private ArrayList<Task> taskList;


    private TextView tvNoTasks;
    private TaskAdapter taskAdapter;
    private ListView listViewTasks;
    private final List<Task> selectedTasksForExport = new ArrayList<>();

    private boolean isSelectionMode = false;
    protected boolean isSearchMode = false;

    private boolean exportMode = false;

    private final List<Task> deletedTasks = new ArrayList<>();

    private Snackbar snackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvNoTasks = findViewById(R.id.tvNoTasks);

        // Inicializar la lista de tareas y el adaptador
        taskList = new ArrayList<>();
        taskAdapter = new TaskAdapter(this, taskList);

        // Obtener una referencia al ListView y establecer el adaptador
        listViewTasks = findViewById(R.id.listViewTasks);
        listViewTasks.setAdapter(taskAdapter);


        // Manejar el evento de hacer clic en una tarea de la lista
        listViewTasks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (isSelectionMode) {
                    toggleSelection(position);
                } else {
                    Task selectedTask = taskList.get(position);
                    openTaskDetailsActivity(selectedTask);
                }
            }
        });
        EditText editTextSearch = findViewById(R.id.editTextSearch);
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String searchText = charSequence.toString().toLowerCase();
                ArrayList<Task> filteredTasks = new ArrayList<>();
                loadTasksFromDatabase();
                for (Task task : taskList) {
                    if (task.getTitle().toLowerCase().contains(searchText)) {
                        filteredTasks.add(task);
                    }
                }
                taskAdapter.setTasks(filteredTasks);
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        // Cargar las tareas existentes desde la base de datos
        loadTasksFromDatabase();

        for (Task task : taskList) {
            if (isTomorrow(task.getDate())) {
                NotificationUtils.scheduleNotification(this, task);
                NotificationUtils.showNotification(this, task);
            }
        }
        if (taskList.isEmpty()) {
            tvNoTasks.setVisibility(View.VISIBLE); // Mostrar el mensaje si no hay tareas
        } else {
            tvNoTasks.setVisibility(View.GONE); // Ocultar el mensaje si hay tareas
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        loadTasksFromDatabase();
        if (taskList.isEmpty()) {
            tvNoTasks.setVisibility(View.VISIBLE); // Mostrar el mensaje si no hay tareas
        } else {
            tvNoTasks.setVisibility(View.GONE); // Ocultar el mensaje si hay tareas
        }
    }

    private void loadTasksFromDatabase() {
        taskList.clear();

        DatabaseHelper databaseHelper = new DatabaseHelper(this); // Crea una instancia de DatabaseHelper
        taskList.addAll(databaseHelper.getAllTasks()); // Llama al método getAllTasks() en la instancia de DatabaseHelper

        taskAdapter.notifyDataSetChanged();
    }

    private void openAddTaskActivity() {
        Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
        startActivity(intent);
    }

    private void openTaskDetailsActivity(Task task) {
        if (snackbar != null && snackbar.isShown()) {
            snackbar.dismiss();
        }
        Intent intent = new Intent(MainActivity.this, TaskDetailsActivity.class);
        intent.putExtra("task", task);
        startActivity(intent);
    }

    private void deleteSelectedTasks() {
        List<Task> selectedTasks = taskAdapter.getSelectedTasks();

        if (selectedTasks.isEmpty()) {
            Toast.makeText(MainActivity.this, "No tasks selected", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Confirm Delete");
        builder.setMessage("Are you sure you want to delete the selected tasks?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Guardar las tareas eliminadas temporalmente
                deletedTasks.addAll(selectedTasks);

                // Eliminar las tareas de la lista actual y actualizar el adaptador
                taskList.removeAll(selectedTasks);
                if (taskList.isEmpty()) {
                    tvNoTasks.setVisibility(View.VISIBLE);
                } else {
                    tvNoTasks.setVisibility(View.GONE);
                }
                taskAdapter.notifyDataSetChanged();

                // Mostrar la Snackbar con la acción Undo
                snackbar = Snackbar.make(findViewById(android.R.id.content),
                        "Selected tasks deleted", Snackbar.LENGTH_SHORT);
                snackbar.setAction("Undo", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Deshacer la eliminación de las tareas
                        taskList.addAll(deletedTasks);
                        if (taskList.isEmpty()) {
                            tvNoTasks.setVisibility(View.VISIBLE);
                        } else {
                            tvNoTasks.setVisibility(View.GONE);
                        }
                        taskAdapter.notifyDataSetChanged();
                        deletedTasks.clear();
                    }
                });

                snackbar.addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);
                        // Si el Snackbar se cierra, eliminar las tareas de la base de datos
                        if (event == DISMISS_EVENT_TIMEOUT || event == DISMISS_EVENT_SWIPE ||
                                event == DISMISS_EVENT_CONSECUTIVE || event == DISMISS_EVENT_MANUAL) {
                            // Eliminar las tareas de la base de datos
                            DatabaseHelper databaseHelper = new DatabaseHelper(MainActivity.this);
                            databaseHelper.deleteTasks(deletedTasks);
                            deletedTasks.clear();
                        }
                    }
                });

                snackbar.show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    @Override
    protected void onPause() {
        super.onPause();

    }

    private boolean isTomorrow(String dateString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        try {
            Date date = dateFormat.parse(dateString);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            Calendar tomorrow = Calendar.getInstance();
            tomorrow.add(Calendar.DAY_OF_YEAR, 1);

            return calendar.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) &&
                    calendar.get(Calendar.MONTH) == tomorrow.get(Calendar.MONTH) &&
                    calendar.get(Calendar.DAY_OF_MONTH) == tomorrow.get(Calendar.DAY_OF_MONTH);
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isSelectionMode) {
            if (exportMode) {
                getMenuInflater().inflate(R.menu.menu_export, menu);
            } else {
                getMenuInflater().inflate(R.menu.menu_delete, menu);
            }
        } else {
            getMenuInflater().inflate(R.menu.menu_main, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem searchTaskMenuItem = menu.findItem(R.id.action_search_task);
        if (searchTaskMenuItem != null) {
            if (isSearchMode) {
                searchTaskMenuItem.setTitle("Stop search");
            } else {
                searchTaskMenuItem.setTitle("Search tasks");
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (snackbar != null && snackbar.isShown()) {
            snackbar.dismiss();
        }
        switch (item.getItemId()) {
            case R.id.action_add_task:
                openAddTaskActivity();
                return true;
            case R.id.action_delete_task:
                if (!isSelectionMode) {
                    startSelectionMode("delete");
                } else {
                    deleteSelectedTasks();
                    stopSelectionMode();
                }
                return true;
            case R.id.action_export_tasks:
                if (!isSelectionMode) {
                    startSelectionMode("export");
                    exportMode = true;
                } else {
                    exportSelectedTasks();
                    stopSelectionMode();
                    exportMode = false;
                }
                return true;
            case R.id.action_import_tasks:
                importTasks();
                return true;
            case R.id.action_search_task:
                setSearchMode();
                return true;
            case R.id.action_settings:
                openSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setSearchMode() {
        isSearchMode = !isSearchMode;
        EditText editTextSearch = findViewById(R.id.editTextSearch);
        LinearLayout linearListViewContainer = findViewById(R.id.linearListViewContainer);

        if (isSearchMode) {
            editTextSearch.setVisibility(View.VISIBLE);
            linearListViewContainer.setLayoutParams(new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT
            ));
        } else {
            editTextSearch.setVisibility(View.GONE);
            linearListViewContainer.setLayoutParams(new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            ));
        }
        invalidateOptionsMenu();
    }


    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }


    private void startSelectionMode(String action) {
        Toast.makeText(MainActivity.this, "Choose the tasks you want to " + action, Toast.LENGTH_SHORT).show();

        isSelectionMode = true;
        taskAdapter.setSelectionMode(true);
        invalidateOptionsMenu();
        listViewTasks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                toggleSelection(position);
            }
        });
        listViewTasks.setOnItemLongClickListener(null);
        taskAdapter.notifyDataSetChanged();
    }

    private void stopSelectionMode() {
        isSelectionMode = false;
        taskAdapter.setSelectionMode(false);
        invalidateOptionsMenu();
        listViewTasks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Task selectedTask = taskList.get(position);
                openTaskDetailsActivity(selectedTask);
            }
        });
        listViewTasks.setOnItemLongClickListener(null);
        taskAdapter.clearSelection();
        taskAdapter.notifyDataSetChanged();
    }

    private void toggleSelection(int position) {
        taskAdapter.toggleSelection(position);
        taskAdapter.notifyDataSetChanged();
    }

    private void exportSelectedTasks() {
        List<Task> selectedTasksForExport = taskAdapter.getSelectedTasks();

        if (selectedTasksForExport.isEmpty()) {
            Toast.makeText(MainActivity.this, "No tasks selected for export", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Confirm Export");
        builder.setMessage("Are you sure you want to export the selected tasks as JSON?");
        builder.setPositiveButton("Export", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    JSONArray jsonArray = new JSONArray();
                    for (Task task : selectedTasksForExport) {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("title", task.getTitle());
                        jsonObject.put("description", task.getDescription());
                        String date = task.getDate();
                        jsonObject.put("date", date);
                        // Codifica el BLOB de la imagen a Base64
                        if (task.getImagePath()!=null){
                            byte[] imageBlob = task.getImagePath(); // getImagePath() obtiene el BLOB de la imagen
                            String base64Image = Base64.encodeToString(imageBlob, Base64.DEFAULT);
                            jsonObject.put("image_path", base64Image);
                        }
                        jsonArray.put(jsonObject);
                    }

                    File documentsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                    File tasksDirectory = new File(documentsDirectory, "Tasks");

                    if (!tasksDirectory.exists()) {
                        tasksDirectory.mkdirs();
                    }

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
                    String timestamp = sdf.format(new Date());

                    String fileName = "tasks_" + timestamp + ".json";
                    File file = new File(tasksDirectory, fileName);

                    FileWriter fileWriter = new FileWriter(file);
                    fileWriter.write(jsonArray.toString());
                    fileWriter.close();

                    Uri fileUri = FileProvider.getUriForFile(MainActivity.this, "com.example.listadetareas.fileprovider", file);

                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Tasks Export");
                    intent.putExtra(Intent.EXTRA_TEXT, "Attached are the exported tasks.");
                    intent.putExtra(Intent.EXTRA_STREAM, fileUri);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setDataAndType(fileUri, getContentResolver().getType(fileUri));


                    startActivity(Intent.createChooser(intent, "Send email"));
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Error exporting tasks", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void importTasks() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/*");

        startActivityForResult(intent, REQUEST_CODE_IMPORT_TASKS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_IMPORT_TASKS && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                importTasks(uri);
            }
        }
    }

    private void importTasks(Uri uri) {
        String extension = getContentResolver().getType(uri);
        if (extension != null && extension.equals("application/json")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Confirm Import");
            builder.setMessage("Are you sure you want to import tasks from the selected JSON file?");
            builder.setPositiveButton("Import", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                        StringBuilder stringBuilder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            stringBuilder.append(line);
                        }
                        reader.close();
                        inputStream.close();

                        String jsonString = stringBuilder.toString();

                        try {
                            JSONArray jsonArray = new JSONArray(jsonString);
                            List<Task> importedTasks = new ArrayList<>();

                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                String title = jsonObject.getString("title");
                                String description = jsonObject.getString("description");
                                String date = jsonObject.getString("date");
                                Task task;
                                if(jsonObject.has("image_path")){
                                    String base64Image = jsonObject.getString("image_path"); // Obtén la representación Base64 de la imagen
                                    byte[] imageBytes = Base64.decode(base64Image, Base64.DEFAULT);
                                    task = new Task(title, description, date, imageBytes);
                                }
                                else{
                                    task = new Task(title, description, date, null);
                                }
                                importedTasks.add(task);
                            }

                            // Inserta las tareas en la base de datos
                            DatabaseHelper databaseHelper = new DatabaseHelper(MainActivity.this);
                            for (Task task : importedTasks) {
                                databaseHelper.insertTask(task);
                            }

                            taskList.addAll(importedTasks);
                            taskAdapter.notifyDataSetChanged();

                            if (taskList.isEmpty()) {
                                tvNoTasks.setVisibility(View.VISIBLE);
                            } else {
                                tvNoTasks.setVisibility(View.GONE);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            builder.setNegativeButton("Cancel", null);

            AlertDialog dialog = builder.create();
            dialog.show();

        } else {
            // El archivo seleccionado no es un archivo JSON
            Toast.makeText(this, "Please select a JSON file", Toast.LENGTH_SHORT).show();
        }
    }


}