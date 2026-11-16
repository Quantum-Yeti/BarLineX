package me.theoria.barlinex.ui.advanced;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import me.theoria.barlinex.R;
import me.theoria.barlinex.ui.common.BottomNavigationHelper;
import me.theoria.barlinex.ui.common.WindowsInsetHelper;
import me.theoria.barlinex.ui.decode.DecodeActivity;
import me.theoria.barlinex.ui.history.HistoryActivity;
import me.theoria.barlinex.ui.main.MainActivity;

public class AdvancedActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_advanced);

        initializeViews();
        initializeWindowInsets();
        setupNavigation();
    }

    private void initializeViews() {

        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void initializeWindowInsets() {

        WindowsInsetHelper.applySystemBarInsets(
                findViewById(R.id.advancedRoot)
        );
    }

    private void setupNavigation() {

        BottomNavigationHelper.setup(
                bottomNavigation,
                R.id.nav_advanced,
                new BottomNavigationHelper.Listener() {

                    @Override
                    public void onCreateSelected() {
                        Intent intent = new Intent(AdvancedActivity.this, MainActivity.class);

                        intent.addFlags(
                                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                        );

                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onDecodeSelected() {
                        openActivity(DecodeActivity.class);
                    }

                    @Override
                    public void onAdvancedSelected() {
                        // Already on Advanced.
                    }

                    @Override
                    public void onHistorySelected() {
                        openActivity(HistoryActivity.class);
                    }
                }
        );
    }

    private void openActivity(Class<?> activityClass) {

        startActivity(new Intent(this, activityClass));
        finish();
    }
}