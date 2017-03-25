package com.rogowiczdawid.smartnote.Fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.rogowiczdawid.smartnote.Note;
import com.rogowiczdawid.smartnote.R;
import com.rogowiczdawid.smartnote.Utilities;

import java.util.ArrayList;

public class SettingsFragment extends PreferenceFragment {

    public final static String THEME_KEY = "pref_key_theme";
    public final static String EXTERNAL_KEY = "pref_storage_dir";
    public static boolean write_to_external = false;

    SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            switch (s) {
                case THEME_KEY:
                    Preference themePreference = findPreference(s);
                    themePreference.setSummary(sharedPreferences.getString(s, ""));
                    getActivity().finish();
                    getActivity().startActivity(new Intent(getActivity(), getActivity().getClass()));
                    break;
                case EXTERNAL_KEY:
                    final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

                    String move_to, move_from;
                    if (write_to_external) {
                        move_to = "internal storage";
                        move_from = "external storage";
                    } else {
                        move_to = "external storage";
                        move_from = "internal storage";
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Do you want to move your files to " + move_to + "?")
                            .setMessage("All your saved notes will be deleted from "
                                    + move_from + " and added to " + move_to)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    ArrayList<Note> temp_list = Utilities.getNotes(getActivity());
                                    for (Note n : temp_list) {
                                        if (!Utilities.onDeleteNote(n.getTitle(), getActivity()))
                                            Toast.makeText(getActivity(), R.string.couldnt_delete, Toast.LENGTH_SHORT).show();
                                    }
                                    write_to_external = preferences.getBoolean("pref_storage_dir", false);
                                    for (Note n : temp_list) {
                                        if (!Utilities.onSaveNote(n, getActivity()))
                                            Toast.makeText(getActivity(), "couldn't save file", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            })
                            .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    write_to_external = preferences.getBoolean("pref_storage_dir", false);
                                }
                            }).show();
                    break;
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    }
}
