package edu.bluejack22_2.timescape;

import static android.content.ContentValues.TAG;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import edu.bluejack22_2.timescape.model.Message;

public class FullScreenImageActivity extends AppCompatActivity {

    RelativeLayout bottomBar;
    RelativeLayout topBar;
    ViewPager2 viewPager;
    TextView imageFileName;
    ImageButton backButton;
    Button downloadImageButton;

    private String currentImageUrl;
    private String currentFileName;
    private String projectId;
    private int currentPosition;

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
        imageFileName = findViewById(R.id.imageFileName);
        bottomBar = findViewById(R.id.bottomBar);
        topBar = findViewById(R.id.topBar);
        viewPager = findViewById(R.id.view_pager);
        downloadImageButton = findViewById(R.id.downloadImageButton);

        projectId = getIntent().getStringExtra("project_id");
        currentPosition = 0;

        viewPager.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleUIVisibility();
            }
        });

        fetchImageVideoMessages();

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        downloadImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadImage(currentImageUrl, currentFileName);
            }
        });


        resetHideUITimer();
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

    private void fetchImageVideoMessages() {
        FirebaseFirestore.getInstance()
                .collection("chats")
                .document(projectId)
                .collection("messages")
                .whereEqualTo("message_type", Message.MessageType.IMAGE)
                .orderBy("timestamp")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Message> messages = queryDocumentSnapshots.toObjects(Message.class);
                    setupViewPager(messages);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch image/video messages", e);
                });
    }

    private void setupViewPager(List<Message> messages) {
        MyPagerAdapter adapter = new MyPagerAdapter(this, messages);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(currentPosition);


        currentImageUrl = messages.get(currentPosition).getContent();
        currentFileName = messages.get(currentPosition).getFileName();
        imageFileName.setText(messages.get(currentPosition).getFileName());

        String messageId = getIntent().getStringExtra("message_id");

        // Find the position of the message with this `messageId` in `messages`
        int position = 0;
        for (Message message : messages) {
            if (messageId.equals(message.getId())) {
                break;
            }
            position++;
        }
        // Use `position` as the initial position for the `ViewPager`
        viewPager.setCurrentItem(position);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentImageUrl = messages.get(position).getContent();
                currentFileName = messages.get(position).getFileName();
                imageFileName.setText(messages.get(position).getFileName());
            }
        });
    }

    void toggleUIVisibility() {
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
}
