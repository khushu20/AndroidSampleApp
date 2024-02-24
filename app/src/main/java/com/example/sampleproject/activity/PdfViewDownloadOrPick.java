package com.example.sampleproject.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;

import com.example.sampleproject.R;
import com.example.sampleproject.databinding.ActivityPdfviewBinding;
import com.example.sampleproject.utility.FileUtils;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PdfViewDownloadOrPick extends AppCompatActivity {
    PdfViewDownloadOrPick activity = this;
    ActivityPdfviewBinding binding;
    public static final int PICK_FROM_GALLERY = 102;
    String pdfurl = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf";
    ActivityResultLauncher<Intent> documentActivityResultLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_pdfview);
        documentActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Uri selectedImage = null;
                        if (data != null) {
                            selectedImage = data.getData();
                            String imagePath = FileUtils.getPath(activity, selectedImage);
                            Log.e("uri", String.valueOf(imagePath));
                            if (imagePath != null) {
                                File file = new File(imagePath);
                                binding.pdfview.fromFile(file)
                                        .defaultPage(0)
                                        .enableAnnotationRendering(true)
                                        .scrollHandle(new DefaultScrollHandle(this))
                                        .spacing(10)
                                        .load();
                                Log.e("uri", String.valueOf(file));
                            }


                        }

                    }
                });
        binding.pickfromstorage.setOnClickListener(v -> {
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PICK_FROM_GALLERY);
                } else {
                    pdfpick();
                }
            }
            else {
                if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PICK_FROM_GALLERY);
                } else {
                    pdfpick();
                }
            }

        });
        ExecutorService service = Executors.newSingleThreadExecutor();
        service.execute(() -> {
            //onpre execute method use loading
            runOnUiThread(() -> {
            });
            try {
                URL url = new URL(pdfurl);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.connect();
                binding.pdfview.fromStream(httpURLConnection.getInputStream()).load();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //onpost execute method remove loading
            runOnUiThread(() -> {

            });
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==PICK_FROM_GALLERY)
        {
            pdfpick();
        }
    }

    private void pdfpick() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        documentActivityResultLauncher.launch(intent);
    }


}
