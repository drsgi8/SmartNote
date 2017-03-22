package com.rogowiczdawid.smartnote;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public final static String TAG = "MyApp_TAG";
    final static String NOTE = "NOTE_FRAGMENT";
    final static String TODO = "TO_DO_FRAGMENT";
    final static String GALLERY = "GALLERY_FRAGMENT";
    final static String SETTINGS = "SETTINGS_FRAGMENT";
    List<MyOnTouchListener> listeners;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    public static <T extends Fragment> void replaceFragment(T fragment, String tag, FragmentTransaction transaction) {
        transaction.replace(R.id.main_frame, fragment, tag);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utilities.setTheme(this);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SettingsFragment.write_to_external = preferences.getBoolean("pref_storage_dir", false);

        setContentView(R.layout.activity_main);

        //Firebase
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null) {
                    Log.d(TAG, "onAuthStateChanged: signed in:" + user.getUid());
                } else {
                    Log.d(TAG, "onAuthStateChanged: signed out");
                }
            }
        };

        //Navigation, Toolbar etc
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //Initialise custom listeners
        if (listeners == null) {
            listeners = new ArrayList<>();
        }

        //Add first Fragment to container
        if (findViewById(R.id.content_main) != null) {
            if (savedInstanceState != null) {
                return;
            }
            GalleryFragment firstFragment = new GalleryFragment();
            firstFragment.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().add(R.id.main_frame, firstFragment).commit();

        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_delete) {

            //Check if button was pressed in the right fragment
            final ToDoFragment toDoFragment = (ToDoFragment) getFragmentManager().findFragmentByTag(TODO);
            final NoteFragment noteFragment = (NoteFragment) getFragmentManager().findFragmentByTag(NOTE);

            if ((toDoFragment != null && toDoFragment.isVisible()) || (noteFragment != null && noteFragment.isVisible())) {

                //Create AlertDialog so the user won't accidentally delete file
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.delete_note);
                builder.setMessage(R.string.you_wont_be_able);
                builder.setNegativeButton(R.string.no, null);
                builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        //Prepare transaction to gallery fragment after deleting file
                        GalleryFragment galleryFragment = new GalleryFragment();
                        galleryFragment.setArguments(getIntent().getExtras());
                        FragmentTransaction transaction = getFragmentManager().beginTransaction();
                        transaction.replace(R.id.main_frame, galleryFragment);
                        transaction.addToBackStack(null);

                        if (toDoFragment != null && toDoFragment.isVisible()) {

                            if (Utilities.onDeleteNote(toDoFragment.getTitleValue(), getApplicationContext())) {
                                transaction.commit();
                                Toast.makeText(getApplicationContext(), getString(R.string.file_deleted), Toast.LENGTH_SHORT).show();
                            } else
                                Toast.makeText(getApplicationContext(), getString(R.string.couldnt_delete), Toast.LENGTH_SHORT).show();
                        } else if (noteFragment != null && noteFragment.isVisible()) {

                            if (Utilities.onDeleteNote(noteFragment.getTitleValue(), getApplicationContext())) {
                                transaction.commit();
                                Toast.makeText(getApplicationContext(), getString(R.string.file_deleted), Toast.LENGTH_SHORT).show();
                            } else
                                Toast.makeText(getApplicationContext(), getString(R.string.couldnt_delete), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                builder.create().show();
            }

            return true;

        } else if (id == R.id.action_save) {

            ToDoFragment toDoFragment = (ToDoFragment) getFragmentManager().findFragmentByTag(TODO);
            if (toDoFragment != null && toDoFragment.isVisible()) {
                if (!toDoFragment.getTitleValue().equals("Title")) {
                    if (Utilities.onSaveNote(new Note(toDoFragment.getTitleValue(), toDoFragment.getUserList(), toDoFragment.getCheckbox_state_list()), getApplicationContext()))
                        Toast.makeText(this, R.string.saved_todo, Toast.LENGTH_SHORT).show();
                    else {
                        Toast.makeText(this, R.string.wrong_todo, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                } else Toast.makeText(this, R.string.set_title, Toast.LENGTH_SHORT).show();

            }

            NoteFragment noteFragment = (NoteFragment) getFragmentManager().findFragmentByTag(NOTE);
            if (noteFragment != null && noteFragment.isVisible()) {
                if (!noteFragment.getTitleValue().equals("Title")) {
                    if (Utilities.onSaveNote(new Note(noteFragment.getTitleValue(), noteFragment.getTextVal()), getApplicationContext()))
                        Toast.makeText(this, R.string.saved_note, Toast.LENGTH_SHORT).show();
                    else {
                        Toast.makeText(this, R.string.wrong_note, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                } else Toast.makeText(this, R.string.set_title, Toast.LENGTH_SHORT).show();
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_gallery:
                replaceFragment(new GalleryFragment(), GALLERY, getFragmentManager().beginTransaction());
                break;
            case R.id.nav_todo:
                replaceFragment(new ToDoFragment(), TODO, getFragmentManager().beginTransaction());
                break;
            case R.id.nav_note:
                replaceFragment(new NoteFragment(), NOTE, getFragmentManager().beginTransaction());
                break;
            case R.id.nav_settings:
                replaceFragment(new SettingsFragment(), SETTINGS, getFragmentManager().beginTransaction());
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        for (MyOnTouchListener listener : listeners) {
            listener.onTouch(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    public void addMyOnTouchListener(MyOnTouchListener listener) {
        listeners.add(listener);
    }


    interface MyOnTouchListener {
        void onTouch(MotionEvent ev);
    }
}

