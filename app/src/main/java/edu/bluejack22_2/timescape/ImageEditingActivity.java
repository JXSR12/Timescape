package edu.bluejack22_2.timescape;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.callback.BitmapCropCallback;
import com.yalantis.ucrop.view.CropImageView;
import com.yalantis.ucrop.view.GestureCropImageView;
import com.yalantis.ucrop.view.OverlayView;
import com.yalantis.ucrop.view.UCropView;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class ImageEditingActivity extends AppCompatActivity {

    private UCropView uCropView;
    private GestureCropImageView mGestureCropImageView;
    private OverlayView mOverlayView;

    private Uri sourceUri;
    private Uri destinationUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_editing);

        uCropView = findViewById(R.id.ucrop);

        uCropView.getCropImageView().setMaxScaleMultiplier(10.0f);
        uCropView.getOverlayView().setDimmedColor(Color.BLACK);
        uCropView.getOverlayView().setShowCropFrame(true);
        uCropView.getOverlayView().setShowCropGrid(true);
        uCropView.getCropImageView().setRotateEnabled(true);
        uCropView.getCropImageView().setScaleEnabled(true);

        mGestureCropImageView = uCropView.getCropImageView();

        mGestureCropImageView.setTargetAspectRatio(0);

        mOverlayView = uCropView.getOverlayView();
        Button confirmButton = findViewById(R.id.confirmButton);
        Button cancelButton = findViewById(R.id.cancelButton);

        // Get image Uri from the intent that started this activity
        String sourceUriString = getIntent().getStringExtra("sourceUri");
        sourceUri = Uri.parse(sourceUriString);
        String destinationUriString = getIntent().getStringExtra("destinationUri");
        destinationUri = Uri.parse(destinationUriString);
        if (sourceUri != null) {
            try {
                mGestureCropImageView.setImageUri(sourceUri, destinationUri);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            mGestureCropImageView.setScaleEnabled(true);
            mGestureCropImageView.setRotateEnabled(true);
            mOverlayView.setShowCropFrame(true);
            mOverlayView.setShowCropGrid(true);
        }
        destinationUri = Uri.parse(destinationUriString);

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Use UCrop to crop the image and save the result to the destinationUri
                mGestureCropImageView.cropAndSaveImage(Bitmap.CompressFormat.JPEG, 100, new BitmapCropCallback() {
                    @Override
                    public void onBitmapCropped(@NonNull Uri resultUri, int offsetX, int offsetY, int imageWidth, int imageHeight) {
                        // The image was successfully cropped and saved to resultUri.
                        // You can now do something with the resultUri.

                        // Create a new intent and put the destinationUri as an extra
                        Intent intent = new Intent();
                        intent.setData(resultUri);
                        setResult(RESULT_OK, intent);

                        // Close this activity
                        finish();
                    }

                    @Override
                    public void onCropFailure(@NonNull Throwable t) {
                        // Something went wrong during cropping or saving.
                        // You should handle this error.
                        Log.e("ImageEditingActivity", "Crop failure", t);
                    }
                });
            }
        });



        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    @SuppressLint("Range")
    public static String getFileNameFromUri(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            Uri resultUri = UCrop.getOutput(data);
            Intent resultIntent = new Intent();
            resultIntent.setData(resultUri);
            setResult(RESULT_OK, resultIntent);
            finish();
        } else if (resultCode == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(data);
            Log.e("UCrop", "Crop error: " + cropError);
        }
    }
}


