package com.example.transcriptor;

import static com.example.transcriptor.MainActivity.CAMERA_PERMISSION_REQUEST;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.annotation.Nullable;

import android.content.Intent;

import android.widget.Toast;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.widget.ImageButton;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.HybridBinarizer;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.io.InputStream;

public class QRScannerActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private DecoratedBarcodeView barcodeView;
    private ImageButton btnPickImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrscanner);

        barcodeView = findViewById(R.id.barcode_scanner);
        btnPickImage = findViewById(R.id.btn_pick_image);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
        }
    }

    private void pickImageFromDevice() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            decodeQRCodeFromImage(selectedImage);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                barcodeView.resume();
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void decodeQRCodeFromImage(Uri imageUri) {
        try {
            InputStream imageStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(
                    new BitmapLuminanceSource(bitmap)
            ));
            Result result = new com.google.zxing.MultiFormatReader().decode(binaryBitmap);
            if (result.getText() != null) {
                Intent intent = new Intent();
                intent.putExtra("uuidC", result.getText());
                setResult(RESULT_OK, intent);
                finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            barcodeView.decodeContinuous(new BarcodeCallback() {
                @Override
                public void barcodeResult(BarcodeResult result) {
                    if (result != null && result.getText() != null) {
                        Intent intent = new Intent();
                        intent.putExtra("uuidC", result.getText());
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                }

                @Override
                public void possibleResultPoints(java.util.List<ResultPoint> resultPoints) {
                }
            });
            barcodeView.resume();
        }
        btnPickImage.setOnClickListener(v -> pickImageFromDevice());
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }
}