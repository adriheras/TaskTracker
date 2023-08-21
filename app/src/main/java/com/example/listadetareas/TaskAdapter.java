package com.example.listadetareas;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends ArrayAdapter<Task> {
    private final Context context;
    private final List<Task> taskList;
    private boolean selectionMode;
    private final List<Integer> selectedPositions;

    public TaskAdapter(Context context, List<Task> taskList) {
        super(context, 0, taskList);
        this.context = context;
        this.taskList = taskList;
        this.selectionMode = false;
        this.selectedPositions = new ArrayList<>();
    }

    public void setSelectionMode(boolean selectionMode) {
        this.selectionMode = selectionMode;
    }

    public void toggleSelection(int position) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(Integer.valueOf(position));
        } else {
            selectedPositions.add(position);
        }
    }

    public List<Task> getSelectedTasks() {
        List<Task> selectedTasks = new ArrayList<>();
        for (int position : selectedPositions) {
            selectedTasks.add(taskList.get(position));
        }
        return selectedTasks;
    }

    public void clearSelection() {
        selectedPositions.clear();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(context).inflate(R.layout.list_item_task, parent, false);
        }

        final Task currentTask = taskList.get(position);

        TextView textViewTitle = listItemView.findViewById(R.id.textViewTaskTitle);
        textViewTitle.setText(currentTask.getTitle());

        TextView textViewDescription = listItemView.findViewById(R.id.textViewTaskDescription);
        textViewDescription.setText(currentTask.getDescription());

        TextView textViewDate = listItemView.findViewById(R.id.textViewTaskDate);
        textViewDate.setText(currentTask.getDate());

        ImageView imageViewThumbnail = listItemView.findViewById(R.id.imageViewTaskThumbnail);

        if (currentTask.getImagePath() != null) {
            imageViewThumbnail.setVisibility(View.VISIBLE);
            Bitmap thumbnailBitmap = BitmapFactory.decodeByteArray(currentTask.getImagePath(), 0, currentTask.getImagePath().length);
            imageViewThumbnail.setImageBitmap(thumbnailBitmap);
        } else {
            imageViewThumbnail.setVisibility(View.GONE);
        }

        final MaterialCardView cardViewTask = listItemView.findViewById(R.id.cardViewTask);
        cardViewTask.setStrokeColor(ContextCompat.getColor(context, R.color.card_stroke_color));
        cardViewTask.setStrokeWidth(5);

        CheckBox checkBoxSelection = listItemView.findViewById(R.id.checkBoxSelection);
        if (selectionMode) {
            checkBoxSelection.setVisibility(View.VISIBLE);
            animateCardView(cardViewTask);
            checkBoxSelection.setChecked(selectedPositions.contains(position));
        } else {
            checkBoxSelection.setVisibility(View.GONE);
            cardViewTask.clearAnimation();
        }

        checkBoxSelection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSelection(position);
                notifyDataSetChanged();
            }
        });

        return listItemView;
    }

    public void setTasks(List<Task> tasks) {
        taskList.clear();
        taskList.addAll(tasks);
        notifyDataSetChanged();
    }

    private void animateCardView(final MaterialCardView cardView) {
        cardView.animate()
                .rotationBy(1f)
                .setDuration(100)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        cardView.animate()
                                .rotationBy(-2f)
                                .setDuration(200)
                                .withEndAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        cardView.animate()
                                                .rotationBy(1f)
                                                .setDuration(100)
                                                .withEndAction(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        animateCardView(cardView);
                                                    }
                                                });
                                    }
                                });
                    }
                });
    }

}
