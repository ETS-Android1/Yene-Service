package com.example.yeneservice.Users;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.yeneservice.HomeActivity;
import com.example.yeneservice.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class ProfileRegisterActivity extends AppCompatActivity {

    private CircleImageView setupImage;
    private Uri mainImageURI = null;
    private String user_id;
    private boolean isChanged = false;

    private EditText setupName;
    private Button setupBtn;
    private ProgressBar setupProgress;

    private StorageReference storageReference;
    private FirebaseAuth firebaseAuth;
    private DocumentReference reference;
    private FirebaseFirestore firebaseFirestore;

    private Bitmap compressedImageFile;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_register);

//        Toolbar setupToolbar = findViewById(R.id.setupToolbar);
//        setSupportActionBar(setupToolbar);
//        getSupportActionBar().setTitle("Account Setup");

        firebaseAuth = FirebaseAuth.getInstance();
        user_id = firebaseAuth.getCurrentUser().getUid();
        reference = FirebaseFirestore.getInstance().collection("Users").document(user_id);
        firebaseFirestore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();

        setupImage = findViewById(R.id.setup_image);
        setupName = findViewById(R.id.setup_name);
        setupBtn = findViewById(R.id.setup_btn);
        setupProgress = findViewById(R.id.setup_progress);

        setupProgress.setVisibility(View.VISIBLE);
//        setupBtn.setEnabled(false);

//        firebaseFirestore.collection("Users").document(user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
//            @SuppressLint("CheckResult")
//            @Override
//            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
//                if(task.isSuccessful()){
//                    if(task.getResult().exists()){
//                        String name = task.getResult().getString("name");
//                        String image = task.getResult().getString("image");
//
//                        mainImageURI = Uri.parse(image);
//                        setupName.setText(name);
//                        RequestOptions placeholderRequest = new RequestOptions();
//                        placeholderRequest.placeholder(R.drawable.default_image);
//
//                        Glide.with(ProfileRegisterActivity.this).setDefaultRequestOptions(placeholderRequest).load(image).into(setupImage);
//                    }
//                } else {
//                    String error = task.getException().getMessage();
//                    Toast.makeText(ProfileRegisterActivity.this, "(FIRESTORE Retrieve Error) : " + error, Toast.LENGTH_LONG).show();
//                }
//                setupProgress.setVisibility(View.INVISIBLE);
//                setupBtn.setEnabled(true);
//            }
//        });


        setupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String user_name = setupName.getText().toString();

                if (!TextUtils.isEmpty(user_name) && mainImageURI != null) {
                    setupProgress.setVisibility(View.VISIBLE);
                    if (isChanged) {
                        user_id = firebaseAuth.getCurrentUser().getUid();
                        File newImageFile = new File(mainImageURI.getPath());
                        try {
                            compressedImageFile = new Compressor(ProfileRegisterActivity.this)
                                    .setMaxHeight(125)
                                    .setMaxWidth(125)
                                    .setQuality(50)
                                    .compressToBitmap(newImageFile);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        compressedImageFile.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                        byte[] thumbData = baos.toByteArray();

                        UploadTask image_path = storageReference.child("profile_images").child(user_id + ".jpg").putBytes(thumbData);

                        image_path.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                if (task.isSuccessful()) {
                                    storeFirestore(task, user_name);
                                } else {
                                    String error = task.getException().getMessage();
                                    Toast.makeText(ProfileRegisterActivity.this, "(IMAGE Error) : " + error, Toast.LENGTH_LONG).show();

                                    setupProgress.setVisibility(View.INVISIBLE);
                                }
                            }
                        });
                    } else {
                        storeFirestore(null, user_name);
                    }
                }
            }
        });

        setupImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if(ContextCompat.checkSelfPermission(ProfileRegisterActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(ProfileRegisterActivity.this, "Permission Request", Toast.LENGTH_LONG).show();
                        ActivityCompat.requestPermissions(ProfileRegisterActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                    } else {
                        BringImagePicker();
                    }
                } else {
                    BringImagePicker();
                }
            }
        });
    }

    private void storeFirestore(@NonNull Task<UploadTask.TaskSnapshot> task, String user_name) {

        Uri download_uri;
        if(task != null) {
            download_uri = task.getResult().getUploadSessionUri();
        } else {
            download_uri = mainImageURI;
        }

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("username", user_name);
        userMap.put("image", download_uri.toString());

        firebaseFirestore.collection("Users").document(user_id).update(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Toast.makeText(ProfileRegisterActivity.this, "The user Settings are updated.", Toast.LENGTH_LONG).show();
                    Intent mainIntent = new Intent(ProfileRegisterActivity.this, HomeActivity.class);
                    startActivity(mainIntent);
                    finish();

                } else {
                    String error = task.getException().getMessage();
                    Toast.makeText(ProfileRegisterActivity.this, "(FIRESTORE Error) : " + error, Toast.LENGTH_LONG).show();
                }
                setupProgress.setVisibility(View.INVISIBLE);
            }
        });

    }

    private void BringImagePicker() {
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1, 1)
                .start(ProfileRegisterActivity.this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                mainImageURI = result.getUri();
                setupImage.setImageURI(mainImageURI);
                isChanged = true;
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        reference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                String img = documentSnapshot.getString("image");
                String usr = documentSnapshot.getString("username");

                if (img != null && usr != null && !img.equals("default")) {
                    sendToMain();
                }
            }
        });
    }
    private void sendToMain() {
        Intent mainIntent = new Intent(ProfileRegisterActivity.this, HomeActivity.class);
        startActivity(mainIntent);
        finish();

    }
}
