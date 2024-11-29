package com.example.transcriptor;

import static com.example.transcriptor.MainActivity.database;
import static com.example.transcriptor.MainActivity.uuid;
import static com.example.transcriptor.MainActivity.uuidC;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class QRCodeGeneratorActivity extends AppCompatActivity {

    TextView tvUUIDShow;
    private ValueEventListener conversationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode_generator);

        tvUUIDShow = findViewById(R.id.tvUUIDShow);
        String uuid = getIntent().getStringExtra("UUID");
        ImageView qrImageView = findViewById(R.id.qrImageView);

        tvUUIDShow.setText(uuid);        DatabaseReference conversationRef = database.child("conversations").child(uuid).child("with_uuid");
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
                Log.e("Firebase", "Failed to listen to conversation creation", error.toException());
            }
        };
        conversationRef.addValueEventListener(conversationListener);

        generateQRCode(uuid, qrImageView);
    }

    @Override
    protected void onResume() {
        super.onResume();

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