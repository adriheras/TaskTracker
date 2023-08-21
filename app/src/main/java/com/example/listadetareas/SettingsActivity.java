package com.example.listadetareas;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            Preference emailPreference = findPreference("email_preference");
            emailPreference.setSummary(getPreferenceManager().getSharedPreferences().getString("email_preference", ""));

            emailPreference.setOnPreferenceClickListener(preference -> {
                // Abrir un diálogo de edición para el correo electrónico
                showDialogToEditEmail();
                return true;
            });
        }

        private void showDialogToEditEmail() {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Edit Email");

            // Obtener el correo electrónico actual de las preferencias
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            String currentEmail = sharedPreferences.getString("email_preference", "");

            // Agregar un EditText al diálogo y establecer el texto con el correo actual
            EditText editText = new EditText(getContext());
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            editText.setText(currentEmail); // Establecer el texto del EditText con el correo actual
            builder.setView(editText);

            // Agregar botones OK y Cancelar al diálogo
            builder.setPositiveButton("OK", (dialog, which) -> {
                String newEmail = editText.getText().toString();

                // Actualizar la preferencia y su resumen
                Preference emailPreference = findPreference("email_preference");
                emailPreference.setSummary(newEmail);

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("email_preference", newEmail);
                editor.apply();
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

            builder.show();
        }

    }
}
