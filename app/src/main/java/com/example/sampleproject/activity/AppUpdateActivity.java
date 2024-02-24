package com.example.sampleproject.activity;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.widget.Toast;

import com.example.sampleproject.R;
import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;

public class AppUpdateActivity extends AppCompatActivity {
    private AppUpdateManager appUpdateManager;
    private static final int MY_REQUEST_CODE = 22;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        appUpdateManager = AppUpdateManagerFactory.create(this);
        appUpdateManager.registerListener(listener);
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            this,
                            MY_REQUEST_CODE);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(this, "No update available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    InstallStateUpdatedListener listener = state -> {

        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            appUpdateManager.completeUpdate();
        }

    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MY_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                onResume();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        appUpdateManager
                .getAppUpdateInfo()
                .addOnSuccessListener(appUpdateInfo -> {
                    if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                        appUpdateManager.completeUpdate();
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        appUpdateManager.registerListener(listener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        //appUpdateManager.unregisterListener(listener);
    }
}