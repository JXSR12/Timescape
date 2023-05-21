package edu.bluejack22_2.timescape;

import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FullScreenImageActivity extends AppCompatActivity {

    RelativeLayout bottomBar;
    RelativeLayout topBar;
    PhotoView fullScreenImageView;
    Button downloadImageButton;
    TextView imageFileName;
    ImageButton backButton;

    StyledPlayerView playerView;
    ExoPlayer player;

    private String imageUrl;
    private String fileName;

    private Handler hideUIHandler = new Handler();
    private Runnable hideUIRunnable = new Runnable() {
        @Override
        public void run() {
            toggleUIVisibility();
        }
    };

    private void resetHideUITimer() {
        hideUIHandler.removeCallbacks(hideUIRunnable);
        hideUIHandler.postDelayed(hideUIRunnable, 3000);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);

        backButton = findViewById(R.id.backButton);
        fullScreenImageView = findViewById(R.id.fullScreenImageView);
        imageFileName = findViewById(R.id.imageFileName);
        downloadImageButton = findViewById(R.id.downloadImageButton);
        bottomBar = findViewById(R.id.bottomBar);
        topBar = findViewById(R.id.topBar);
        playerView = findViewById(R.id.player_view);

        playerView.setVisibility(View.GONE);

        imageUrl = getIntent().getStringExtra("image_url");
        fileName = getIntent().getStringExtra("file_name");

        fullScreenImageView.setMaximumScale(5.0f);
        fullScreenImageView.setMinimumScale(1.0f);


        // Set the file name
        imageFileName.setText(fileName);

        // Set the download button click listener
        downloadImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadImage(imageUrl, fileName);
            }
        });

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReferenceFromUrl(imageUrl);

        Task<StorageMetadata> metadataTask = storageRef.getMetadata();
        metadataTask.addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
            @Override
            public void onSuccess(StorageMetadata storageMetadata) {
                String mimeType = storageMetadata.getContentType();
                if (mimeType != null && mimeType.startsWith("video")) {
                    initializePlayer();
                } else {
                    loadImage();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
            }
        });

        fullScreenImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleUIVisibility();
            }
        });

        playerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleUIVisibility();
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        resetHideUITimer();
    }

    private void initializePlayer() {
        if (player == null) {
            player = new ExoPlayer.Builder(this).build();

            playerView.setPlayer(player);

            MediaItem mediaItem = MediaItem.fromUri(imageUrl);
            player.setMediaItem(mediaItem);
            player.prepare();
            player.play();
        }
        playerView.setVisibility(View.VISIBLE);
        fullScreenImageView.setVisibility(View.GONE);
    }

    private void loadImage(){
        Glide.with(this)
                .load(imageUrl)
                .into(fullScreenImageView);
        playerView.setVisibility(View.GONE);
        fullScreenImageView.setVisibility(View.VISIBLE);
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }
    private void downloadImage(String imageUrl, String fileName) {
        if (imageUrl == null) {
            Toast.makeText(this, "Error: Image URL is null", Toast.LENGTH_SHORT).show();
            return;
        }

        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        saveImageToGallery(resource, fileName);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }

    private void saveImageToGallery(Bitmap bitmap, String fileName) {
        OutputStream fos;

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

        Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        try {
            fos = getContentResolver().openOutputStream(imageUri);

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);

            if (fos != null) {
                fos.flush();
                fos.close();
            }

            Toast.makeText(this, R.string.image_saved_to_gallery, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.error_saving_image_to_gallery, Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleUIVisibility() {
        int newVisibility = bottomBar.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE;

        topBar.setVisibility(newVisibility);
        bottomBar.setVisibility(newVisibility);
        imageFileName.setVisibility(newVisibility);
        backButton.setVisibility(newVisibility);

        if (newVisibility == View.VISIBLE) {
            resetHideUITimer();
        } else {
            hideUIHandler.removeCallbacks(hideUIRunnable);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Util.SDK_INT >= 24) {
            initializePlayer();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Util.SDK_INT < 24 || player == null) {
            initializePlayer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Util.SDK_INT < 24) {
            releasePlayer();
        }
        player.stop();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Util.SDK_INT < 24) {
            releasePlayer();
        }
        player.stop();
    }


    }

