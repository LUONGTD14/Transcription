package com.example.transcriptor;

import static com.example.transcriptor.MainActivity.database;
import static com.example.transcriptor.MainActivity.uuid;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.OutputStream;

public class QRCodeGeneratorActivity extends AppCompatActivity {
    private static final String TAG = QRCodeGeneratorActivity.class.getName();
    private static final int WRITE_REQUEST = 100;
    TextView tvUUIDShow, tvSave;
    private ValueEventListener conversationListener;
    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode_generator);

        tvUUIDShow = findViewById(R.id.tvUUIDShow);
        tvSave = findViewById(R.id.tvSave);
        String uuid = getIntent().getStringExtra("UUID");
        ImageView qrImageView = findViewById(R.id.qrImageView);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.MANAGE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE,
            Manifest.permission.READ_MEDIA_IMAGES}, WRITE_REQUEST);
        }


        tvUUIDShow.setText(uuid);
        DatabaseReference conversationRef = database.child("conversations").child(uuid).child("with_uuid");
        conversationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String withUuid = snapshot.getValue(String.class);
                if (withUuid != null && !withUuid.isEmpty()) {
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to listen to conversation creation", error.toException());
            }
        };
        conversationRef.addValueEventListener(conversationListener);

        generateQRCode(uuid, qrImageView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        tvSave.setOnClickListener(v -> {
            saveQRCodeAndUUID(bitmap, uuid);
        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_REQUEST && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        }
    }

    private void saveQRCodeAndUUID(Bitmap qrCodeBitmap, String uuid) {
        try {
            saveBitmapToFile(qrCodeBitmap, "QR_Code_" + System.currentTimeMillis() + ".png");
            Toast.makeText(this, "Saved to: Download/Transcriptor", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save QR Code", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveBitmapToFile(Bitmap bitmap, String fileName) throws Exception {
        try {
            String relativePath = Environment.DIRECTORY_DOWNLOADS + "/Transcriptor" ;

            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName); // Tên file
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png"); // Loại file
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath); // Đường dẫn tương đối

            Uri imageUri = getApplicationContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);

            if (imageUri != null) {
                try (OutputStream outputStream = getApplicationContext().getContentResolver().openOutputStream(imageUri)) {
                    // Ghi bitmap vào file
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                }
                System.out.println("File saved: " + imageUri.toString());
            } else {
                System.err.println("Failed to create MediaStore entry.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void generateQRCode(String uuid, ImageView imageView) {
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            bitmap = barcodeEncoder.encodeBitmap(uuid, BarcodeFormat.QR_CODE,
                    400, 400);
            imageView.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}