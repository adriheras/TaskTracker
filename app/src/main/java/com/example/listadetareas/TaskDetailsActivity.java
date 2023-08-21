package com.example.listadetareas;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class TaskDetailsActivity extends AppCompatActivity {

    private TextView textViewTitle;
    private TextView textViewDescription;
    private TextView textViewDate;

    private ImageView imageViewTaskImage;

    private Task task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_details);

        textViewTitle = findViewById(R.id.textViewTaskTitle);
        textViewDescription = findViewById(R.id.textViewTaskDescription);
        textViewDate = findViewById(R.id.textViewTaskDate);
        imageViewTaskImage = findViewById(R.id.imageViewTaskImage);


        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("task")) {
            task = (Task) intent.getSerializableExtra("task");
            if (task != null) {
                displayTaskDetails(task);

                Button btnEditTask = findViewById(R.id.btnEditTask);
                btnEditTask.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        editTaskLauncher.launch(new Intent(TaskDetailsActivity.this, EditTaskActivity.class)
                                .putExtra("task", task));
                    }
                });
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update the task details if the task was edited
        if (task != null) {
            displayTaskDetails(task);
        }
    }

    private void displayTaskDetails(Task task) {
        textViewTitle.setText(task.getTitle());
        textViewDescription.setText(task.getDescription());
        textViewDate.setText(task.getDate());
        if (task.getImagePath() != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(task.getImagePath(), 0, task.getImagePath().length);
            imageViewTaskImage.setImageBitmap(bitmap);
            imageViewTaskImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        }

    }
    ActivityResultLauncher<Intent> editTaskLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.hasExtra("updatedTask")) {
                            // Update the task if it was edited
                            task = (Task) data.getSerializableExtra("updatedTask");
                            displayTaskDetails(task);
                        }
                    }
                }
            }
    );
}
