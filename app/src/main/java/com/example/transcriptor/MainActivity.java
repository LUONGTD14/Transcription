package com.example.transcriptor;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import androidx.annotation.Nullable;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.transcriptor.databinding.ActivityMainBinding;

import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final int PAIR_REQUEST = 2;
    public static final int MIC_REQUEST = 3;
    public static final int CAMERA_PERMISSION_REQUEST = 6;
    private static final String PREF_NAME = "MyAppPreferences";
    private static final String KEY_UUID = "UUID";
    private FloatingActionButton btnConnect, btnRecord;
    private TextView tvScription, tvTranscription, tvUUID, tvUUIDPair;
    private MediaRecorder mediaRecorder;
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private DatabaseReference database;
    SharedPreferences sharedPreferences;
    static String uuid = null;
    public static String uuidC = null;
    private static final String fromLanguage = "en";
    private static final String toLanguage = "vi";
    private TranslatorOptions translatorOptions;
    private Translator translator;
    private boolean isConnecting;

    static {
        System.loadLibrary("transcriptor");
    }

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        btnConnect = binding.btnConnect;
        btnRecord = binding.btnRecord;
        tvScription = binding.tvScription;
        tvTranscription = binding.tvTranscription;
        tvUUID = binding.tvUUID;
        tvUUIDPair = binding.tvUUIDPair;

        database = FirebaseDatabase.getInstance().getReference();
        getUUID(false);
        isConnecting = false;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onResume() {
        super.onResume();

        tvUUIDPair.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                uuidC = null;
                Log.e("Firebase", uuidC == null ? "null" : "not null");
                tvUUIDPair.setText("");
                isConnecting = false;
                deleteConversation();
                return false;
            }
        });
        btnConnect.setOnClickListener(v -> {
            showConnectDialog();
        });

        btnRecord.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, new String[]{
                                Manifest.permission.RECORD_AUDIO}, MIC_REQUEST);
                    } else {
                        startRecording();
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    stopRecording();
                    break;
            }
            return true;
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PAIR_REQUEST && resultCode == RESULT_OK && data != null) {
            uuidC = data.getStringExtra("uuidC");
            if (uuidC != null) {
                showConnectConfirmDialog();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MIC_REQUEST && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startRecording();
        } else {
            tvScription.setText("Record permission!");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    private void initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                tvTranscription.setText("Transcription message.");
                tvScription.setText("Listening...");
            }

            @Override
            public void onBeginningOfSpeech() {
                tvScription.setText("Recording...");
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
                tvScription.setText("Processing...");
            }

            @Override
            public void onError(int error) {
                tvScription.setText("Error occurred. Try again.");
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String transcription = matches.get(0);
                    tvScription.setText(transcription);
                    tvTranscription.setText("Translating...");
                    translateText(transcription, fromLanguage, toLanguage);
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        });
    }

    private void translateText(String textToTranslate, String fromLanguage, String toLanguage) {
        translatorOptions = new TranslatorOptions.Builder()
                .setSourceLanguage(fromLanguage)
                .setTargetLanguage(toLanguage)
                .build();
        translator = Translation.getClient(translatorOptions);
        DownloadConditions downloadConditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();
        translator.downloadModelIfNeeded(downloadConditions)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        translator.translate(textToTranslate)
                                .addOnSuccessListener(new OnSuccessListener<String>() {
                                    @Override
                                    public void onSuccess(String s) {
                                        tvTranscription.setText(s);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        e.printStackTrace();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        e.printStackTrace();
                    }
                });

    }

    private void startRecording() {
        btnRecord.setImageResource(R.drawable.mic_on);
        tvTranscription.setText("");
        if (speechRecognizer == null) {
            initializeSpeechRecognizer();
        }
        speechRecognizer.startListening(recognizerIntent);
    }

    private void stopRecording() {
        btnRecord.setImageResource(R.drawable.mic_off);
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
    }

    private void showConnectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Role")
                .setIcon(R.drawable.transciptor)
                .setMessage("Are you Receiver or Connector?")
                .setPositiveButton("Connector", (dialog, which) -> startQRCodeScanner())
                .setNegativeButton("Receiver", (dialog, which) -> showQRCodeDialog());
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setAllCaps(false);
        });
        dialog.show();
    }

    private void showConnectConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Accept connection")
                .setIcon(R.drawable.transciptor)
                .setMessage("Are you accept connect to" + uuidC + "?")
                .setPositiveButton("Agree", (dialog, which) -> createConversation())
                .setNegativeButton("Disagree", (dialog, which) -> {
                });
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setAllCaps(false);
        });
        dialog.show();
    }

    private void createConversation() {
        tvUUIDPair.setText(uuidC);
        isConnecting = true;
        Map<String, Object> conversationMap = new HashMap<>();
        conversationMap.put("conversations/" + uuid + "/with_uuid", uuidC);
        conversationMap.put("conversations/" + uuidC + "/with_uuid", uuid);

        database.updateChildren(conversationMap)
                .addOnSuccessListener(aVoid -> Log.d("Firebase", "Conversation created successfully"))
                .addOnFailureListener(e -> Log.e("Firebase",
                        "Failed to create conversation", e));
    }

    private void deleteConversation() {
        Map<String, Object> deleteMap = new HashMap<>();
        deleteMap.put("conversations/" + uuid, null);
        deleteMap.put("conversations/" + uuidC, null);

        database.updateChildren(deleteMap)
                .addOnSuccessListener(aVoid -> Log.d("Firebase", "Conversation deleted successfully"))
                .addOnFailureListener(e -> Log.e("Firebase", "Failed to delete conversation", e));
    }


    private void startQRCodeScanner() {
        uuidC = null;
        Intent intent = new Intent(this, QRScannerActivity.class);
        startActivityForResult(intent, PAIR_REQUEST);
    }

    private void showQRCodeDialog() {
        Intent intent = new Intent(this, QRCodeGeneratorActivity.class);
        intent.putExtra("UUID", uuid);
        startActivity(intent);
    }

    private void getUUID(boolean exist) {
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        uuid = sharedPreferences.getString(KEY_UUID, null);
        if (exist || uuid == null) {
            uuid = createUUID();
            if (exist) {
                Toast.makeText(getApplicationContext(), "Create new uuid",
                        Toast.LENGTH_SHORT).show();
            }
            sharedPreferences.edit().putString(KEY_UUID, uuid).apply();
            saveUUIDToFirebase(uuid);
        }
        tvUUID.setText(uuid);
    }

    private void saveUUIDToFirebase(String uuid) {
        DatabaseReference uuidRef = database.child("users").child(uuid);
        uuidRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    getUUID(true);
                } else {
                    uuidRef.setValue(true)
                            .addOnSuccessListener(aVoid -> Log.d("Firebase",
                                    "UUID saved successfully"))
                            .addOnFailureListener(e -> Log.e("Firebase",
                                    "Failed to save UUID", e));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Firebase", "Error checking UUID", databaseError.toException());
            }
        });
    }

    private String createUUID() {
        return uuid = UUID.randomUUID().toString();
    }
}