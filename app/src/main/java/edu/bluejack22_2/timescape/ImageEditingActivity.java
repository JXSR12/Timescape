package edu.bluejack22_2.timescape;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.view.GestureCropImageView;
import com.yalantis.ucrop.view.OverlayView;
import com.yalantis.ucrop.view.UCropView;

import java.io.File;

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
        mOverlayView = uCropView.getOverlayView();
        Button confirmButton = findViewById(R.id.confirmButton);
        Button cancelButton = findViewById(R.id.cancelButton);

        // Get image Uri from the intent that started this activity
        sourceUri = getIntent().getData();
        if (sourceUri != null) {
            try {
                mGestureCropImageView.setImageUri(sourceUri, null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            mGestureCropImageView.setScaleEnabled(true);
            mGestureCropImageView.setRotateEnabled(true);
            mOverlayView.setShowCropFrame(true);
            mOverlayView.setShowCropGrid(true);
        }

        // Get the original file name and append "-cropped" before the extension
        String originalFileName = getFileNameFromUri(this, sourceUri);
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String croppedFileName = originalFileName.substring(0, originalFileName.lastIndexOf(".")) + "-cropped" + extension;
        destinationUri = Uri.fromFile(new File(getCacheDir(), croppedFileName));

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Use UCrop to crop the image and save the result to the destinationUri
                UCrop.of(sourceUri, destinationUri).start(ImageEditingActivity.this);
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


