package com.example.listadetareas;

import static com.example.listadetareas.NotificationUtils.sendEmailNotification;

import android.content.Context;
import android.os.AsyncTask;

public class SendEmailTask extends AsyncTask<Void, Void, Void> {
    private final Context context;
    private final Task task;

    public SendEmailTask(Context context, Task task) {
        this.context = context;
        this.task = task;
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            sendEmailNotification(context, task);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
    }
}
