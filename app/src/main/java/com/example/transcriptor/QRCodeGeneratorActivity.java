package com.example.transcriptor;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class QRCodeGeneratorActivity extends AppCompatActivity {

    TextView tvUUIDShow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode_generator);

        tvUUIDShow = findViewById(R.id.tvUUIDShow);
        String uuid = getIntent().getStringExtra("UUID");
        ImageView qrImageView = findViewById(R.id.qrImageView);

        tvUUIDShow.setText(uuid);
        generateQRCode(uuid, qrImageView);
    }

    private void generateQRCode(String uuid, ImageView imageView) {
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(uuid, BarcodeFormat.QR_CODE,
                    400, 400);
            imageView.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}