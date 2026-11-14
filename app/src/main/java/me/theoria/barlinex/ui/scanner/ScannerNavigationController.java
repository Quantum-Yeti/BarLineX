package me.theoria.barlinex.ui.scanner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.TransformExperimental;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import me.theoria.barlinex.R;

@OptIn(markerClass = TransformExperimental.class)
public class ScannerNavigationController {

    private final AppCompatActivity activity;

    private final ActivityResultLauncher<String> permissionLauncher;

    private final NumberScannerController scannerController;

    // Views
    private final View createContent;
    private final View scanOverlay;
    private final ScannerOverlay scannerOverlay;

    private final MaterialButton saveButton;
    private final MaterialButton exportButton;

    private final BottomNavigationView bottomNavigation;

    // Back navigation
    private final OnBackPressedCallback backCallback =
            new OnBackPressedCallback(false) {
                @Override
                public void handleOnBackPressed() {
                    bottomNavigation.setSelectedItemId(R.id.nav_create);
                }
            };

    public ScannerNavigationController(
            AppCompatActivity activity,
            ActivityResultLauncher<String> permissionLauncher,
            NumberScannerController scannerController,
            View createContent,
            View scanOverlay,
            ScannerOverlay scannerOverlay,
            MaterialButton saveButton,
            MaterialButton exportButton,
            BottomNavigationView bottomNavigation
    ) {
        this.activity = activity;
        this.permissionLauncher = permissionLauncher;
        this.scannerController = scannerController;
        this.createContent = createContent;
        this.scanOverlay = scanOverlay;
        this.scannerOverlay = scannerOverlay;
        this.saveButton = saveButton;
        this.exportButton = exportButton;
        this.bottomNavigation = bottomNavigation;

        activity.getOnBackPressedDispatcher().addCallback(
                activity,
                backCallback
        );
    }

    // Permission handling
    public void requestScan() {

        boolean permissionGranted =
                ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED;

        if (permissionGranted) {
            showScanner();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    public void onPermissionResult(boolean granted) {

        if (granted) {
            showScanner();
            return;
        }

        Toast.makeText(
                activity,
                "Camera permission is needed to scan numbers.",
                Toast.LENGTH_LONG
        ).show();

        bottomNavigation.setSelectedItemId(R.id.nav_create);
    }

    // Scanner visibility

    public void showScanner() {

        hideKeyboard();

        createContent.setVisibility(View.GONE);
        saveButton.setVisibility(View.GONE);
        exportButton.setVisibility(View.GONE);

        scanOverlay.setVisibility(View.VISIBLE);
        scannerOverlay.setVisibility(View.VISIBLE);

        backCallback.setEnabled(true);

        scannerController.startCamera();
    }

    public void exitScanMode() {

        backCallback.setEnabled(false);

        scannerController.stopCamera();

        scanOverlay.setVisibility(View.GONE);
        scannerOverlay.setVisibility(View.GONE);

        createContent.setVisibility(View.VISIBLE);
        saveButton.setVisibility(View.VISIBLE);
        exportButton.setVisibility(View.VISIBLE);
    }

    private void hideKeyboard() {

        View focusedView = activity.getCurrentFocus();

        if (focusedView == null) {
            return;
        }

        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(
                        AppCompatActivity.INPUT_METHOD_SERVICE
                );

        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(
                    focusedView.getWindowToken(),
                    0
            );
        }
    }
}

