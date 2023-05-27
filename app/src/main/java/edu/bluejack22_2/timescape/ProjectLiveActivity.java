package edu.bluejack22_2.timescape;

import static android.content.ContentValues.TAG;

import static edu.bluejack22_2.timescape.services.ScreenSharingForegroundService.CHANNEL_ID;

import android.app.Notification;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import edu.bluejack22_2.timescape.custom_ui.AgoraButton;
import edu.bluejack22_2.timescape.custom_ui.AgoraConnectionData;
import edu.bluejack22_2.timescape.custom_ui.AgoraSettings;
import edu.bluejack22_2.timescape.custom_ui.AgoraVideoViewer;
import edu.bluejack22_2.timescape.custom_ui.AgoraVideoViewerDelegate;
import edu.bluejack22_2.timescape.custom_ui.AgoraViewerColors;
import edu.bluejack22_2.timescape.services.ScreenSharingForegroundService;
import io.agora.rtc2.Constants;
import io.agora.media.RtcTokenBuilder2;
import io.agora.media.RtcTokenBuilder2.Role;

import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.ChannelMediaOptions;
import kotlin.jvm.functions.Function1;


//public class ProjectLiveActivity extends BaseActivity {
//
//    private static final int PERMISSION_REQ_ID = 22;
//    private static final String[] REQUESTED_PERMISSIONS =
//            {
//                    android.Manifest.permission.RECORD_AUDIO,
//                    android.Manifest.permission.CAMERA
//            };
//    private final String appId = "8922e10ce4734dbbb09a66d965cc0a08";
//    private final String appCertificate = "93a77278a7c74869a0052b5aff9a1b03";
//    private String channelName = "";
//    private String token = "";
//
//    private int uid = 0;
//    private int expirationTimeSeconds = 3600;
//
//    private boolean isJoined = false;
//
//    private RtcEngine agoraEngine;
//
//    private SurfaceView localSurfaceView;
//
//    private SurfaceView remoteSurfaceView;
//
//    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
//        @Override
//        public void onUserJoined(int uid, int elapsed) {
//            showMessage("Remote user joined " + uid);
//            runOnUiThread(() -> setupRemoteVideo(uid));
//        }
//
//
//        @Override
//        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
//            isJoined = true;
//            showMessage("Joined Channel " + channel);
//        }
//
//        @Override
//        public void onUserOffline(int uid, int reason) {
//            showMessage("Remote user offline " + uid + " " + reason);
//            runOnUiThread(() -> remoteSurfaceView.setVisibility(View.GONE));
//        }
//
//        @Override
//        public void onError(int err) {
//            super.onError(err);
//            showMessage(RtcEngine.getErrorDescription(err));
//        }
//    };
//
//    public void joinChannel() {
//        if (checkSelfPermission()) {
//            ChannelMediaOptions options = new ChannelMediaOptions();
//            options.autoSubscribeAudio = true;
//            options.autoSubscribeVideo = true;
//            options.publishMicrophoneTrack = true;
//            options.publishCameraTrack = true;
//            options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION;
//            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
//
//            setupLocalVideo();
//            localSurfaceView.setVisibility(View.VISIBLE);
//
//            agoraEngine.startPreview();
//            int res = agoraEngine.joinChannel(token, channelName, uid, options);
//            if(res != 0){
//                Toast.makeText(getApplicationContext(), RtcEngine.getErrorDescription(Math.abs(res)), Toast.LENGTH_SHORT).show();
//            }
//        } else {
//            Toast.makeText(getApplicationContext(), "Permissions was not granted", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//
//    private boolean checkSelfPermission()
//    {
//        if (ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[0]) !=  PackageManager.PERMISSION_GRANTED ||
//                ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[1]) !=  PackageManager.PERMISSION_GRANTED)
//        {
//            return false;
//        }
//        return true;
//    }
//
//    void showMessage(String message) {
//        runOnUiThread(() ->
//                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show());
//    }
//
//    private void setupVideoSDKEngine() {
//        try {
//            RtcEngineConfig config = new RtcEngineConfig();
//            config.mContext = getBaseContext();
//            config.mAppId = appId;
//            config.mEventHandler = mRtcEventHandler;
//            agoraEngine = RtcEngine.create(config);
//            // By default, the video module is disabled, call enableVideo to enable it.
//            agoraEngine.enableVideo();
//
//            joinChannel();
//
//        } catch (Exception e) {
//            showMessage(e.toString());
//        }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == PERMISSION_REQ_ID) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
//                setupVideoSDKEngine();
//            } else {
//                Toast.makeText(this, R.string.please_grant_camera_and_microphone_permissions, Toast.LENGTH_LONG).show();
//            }
//        }
//    }
//
//    private void setupRemoteVideo(int uid) {
//        FrameLayout container = findViewById(R.id.remote_video_view_container);
//        remoteSurfaceView = new SurfaceView(getBaseContext());
//        remoteSurfaceView.setZOrderMediaOverlay(true);
//        container.addView(remoteSurfaceView);
//        agoraEngine.setupRemoteVideo(new VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_FIT, uid));
//        // Display RemoteSurfaceView.
//        remoteSurfaceView.setVisibility(View.VISIBLE);
//    }
//
//    private void setupLocalVideo() {
//        FrameLayout container = findViewById(R.id.local_video_view_container);
//        // Create a SurfaceView object and add it as a child to the FrameLayout.
//        localSurfaceView = new SurfaceView(getBaseContext());
//        container.addView(localSurfaceView);
//        // Call setupLocalVideo with a VideoCanvas having uid set to 0.
//
//        agoraEngine.setupLocalVideo(new VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
//        agoraEngine.setDefaultAudioRoutetoSpeakerphone(true);
//        localSurfaceView.setVisibility(View.VISIBLE);
//    }
//
//    private void fetchToken() {
//        uid = 0;
//        RtcTokenBuilder2 tokenBuilder = new RtcTokenBuilder2();
//
//        int timestamp = (int)(System.currentTimeMillis() / 1000 + expirationTimeSeconds);
//
//        String result = tokenBuilder.buildTokenWithUid(appId, appCertificate,
//                channelName, uid, Role.ROLE_PUBLISHER, timestamp, timestamp);
//        Log.d("TOKEN GENERATOR", "Generated token = " + result);
//        token = result;
//
//        setupVideoSDKEngine();
//    }
//
//    public void leaveChannel(View view) {
//        if (!isJoined) {
//            showMessage("Join a channel first");
//        } else {
//            agoraEngine.leaveChannel();
//            showMessage("You left the channel");
//            // Stop remote video rendering.
//            if (remoteSurfaceView != null) remoteSurfaceView.setVisibility(View.GONE);
//            // Stop local video rendering.
//            if (localSurfaceView != null) localSurfaceView.setVisibility(View.GONE);
//            isJoined = false;
//        }
//    }
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_project_live);
//        // If all the permissions are granted, initialize the RtcEngine object and join a channel.
//
//        Intent intent = getIntent();
//        channelName = intent.getStringExtra("channelId");
//    }
//
//    public void joinAction(View view){
//        if (!checkSelfPermission()) {
//            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID);
//        }else{
//            fetchToken();
//        }
//    }
//
//    protected void onDestroy() {
//        super.onDestroy();
//        agoraEngine.stopPreview();
//        agoraEngine.leaveChannel();
//
//        // Destroy the engine in a sub-thread to avoid congestion
//        new Thread(() -> {
//            RtcEngine.destroy();
//            agoraEngine = null;
//        }).start();
//    }
//
//    @Override
//    public void onBackPressed() {
//        super.onBackPressed();
//    }
//
//    public static int generateUid() {
//        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
//        if (user != null) {
//            String uidString = user.getUid();
//            return uidString.hashCode();
//        }
//        return 0;
//    }
//}

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.os.Build;
import android.util.DisplayMetrics;
import android.widget.ProgressBar;

import io.agora.rtc2.ScreenCaptureParameters;


public class ProjectLiveActivity extends BaseActivity{
    // Object of AgoraVideoVIewer class
    private AgoraVideoViewer agView = null;

    // Fill the channel name.
    Button joinButton;

    private final int DEFAULT_SHARE_FRAME_RATE = 24;
    private boolean isSharingScreen = false;
    private Intent fgServiceIntent;

    private final String appCertificate = "93a77278a7c74869a0052b5aff9a1b03";
    private final String appId = "8922e10ce4734dbbb09a66d965cc0a08";

    private String token = "WILL BE SET";
    private String channelName = "WILL BE SET";

    private String projectId = "NONE";

    private RtcEngine agoraEngine;

    private void initializeAndJoinChannel() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DocumentReference docRef = db.collection("users").document(userId);

        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        String displayName = document.getString("displayName");

                        // Now that we have the displayName, initialize AgoraVideoViewer.
                        try {
                            AgoraSettings agSettings = new AgoraSettings();
                            AgoraViewerColors agColors = agSettings.getColors();
                            agColors.setButtonBackgroundColor(getColor(R.color.orange_def2));
                            agColors.setFloatingBackgroundColor(getColor(R.color.orange_def2));
                            agColors.setButtonBackgroundAlpha(0);
                            agColors.setFloatingBackgroundAlpha(0);
                            agColors.setMicFlag(getColor(R.color.orange_acc0));
                            agSettings.setColors(agColors);

                            AgoraButton screenShare = new AgoraButton(ProjectLiveActivity.this);
                            screenShare.setClickAction(new Function1<AgoraButton, Object>() {
                                @Override
                                public Object invoke(AgoraButton agoraButton) {
                                    shareScreen(agoraButton);
                                    return null;
                                }
                            });

                            screenShare.setImageResource(R.drawable.round_mobile_screen_share_24);
                            screenShare.getBackground().setTint(Color.GRAY);

                            List<AgoraButton> extraBtns = new ArrayList<>();

                            AgoraButton endBtn = new AgoraButton(ProjectLiveActivity.this);
                            endBtn.setImageResource(R.drawable.round_close_24);
                            endBtn.getBackground().setTint(getColor(R.color.colorRed));
                            endBtn.setClickAction(new Function1<AgoraButton, Object>() {
                                @Override
                                public Object invoke(AgoraButton agoraButton) {
                                    agView.getAgkit().stopPreview();
                                    agView.leaveChannel();
                                    agoraEngine.stopScreenCapture();
                                    onBackPressed();
                                    return null;
                                }
                            });

                            extraBtns.add(screenShare);
                            extraBtns.add(endBtn);

                            agSettings.setExtraButtons(extraBtns);

                            Set<AgoraSettings.BuiltinButton> enabledButtons = new HashSet<>();
                            enabledButtons.add(AgoraSettings.BuiltinButton.CAMERA);
                            enabledButtons.add(AgoraSettings.BuiltinButton.MIC);
                            enabledButtons.add(AgoraSettings.BuiltinButton.FLIP);

                            agSettings.setEnabledButtons(enabledButtons);

                            agView = new AgoraVideoViewer(ProjectLiveActivity.this,
                                    new AgoraConnectionData(appId, token),
                                    AgoraVideoViewer.Style.FLOATING,
                                    agSettings,
                                    userId,
                                    displayName);

                            agView.setStyle(AgoraVideoViewer.Style.FLOATING);

                            agoraEngine = agView.getAgkit();

                        } catch (Exception e) {
                            Log.e("AgoraVideoViewer",
                                    "Could not initialize AgoraVideoViewer. Check that your app Id is valid.");
                            Log.e("Exception", e.toString());
                            return;
                        }

                        // Add the AgoraVideoViewer to the Activity layout
                        ProjectLiveActivity.this.addContentView(agView, new FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT)
                        );

                        // Check permission and join a channel
                        checkPermissions();
                    } else {
                        Log.d("ProjectLiveActivity", "No such document");
                    }
                } else {
                    Log.d("ProjectLiveActivity", "get failed with ", task.getException());
                }
            }
        });
    }

    public void shareScreen(View view) {
        AgoraButton sharingButton = (AgoraButton) view;

        if (!isSharingScreen) { // Start sharing
            // Ensure that your Android version is Lollipop or higher.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                fgServiceIntent = new Intent(this, ScreenSharingForegroundService.class);
                startForegroundService(fgServiceIntent);
            }
            // Get the screen metrics
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);

            // Set screen capture parameters
            ScreenCaptureParameters screenCaptureParameters = new ScreenCaptureParameters();
            screenCaptureParameters.captureVideo = true;
            screenCaptureParameters.videoCaptureParameters.width = metrics.widthPixels;
            screenCaptureParameters.videoCaptureParameters.height = metrics.heightPixels;
            screenCaptureParameters.videoCaptureParameters.framerate = DEFAULT_SHARE_FRAME_RATE;
            screenCaptureParameters.captureAudio = true;
            screenCaptureParameters.audioCaptureParameters.captureSignalVolume = 50;

            // Start screen sharing
            agoraEngine.startScreenCapture(screenCaptureParameters);
            isSharingScreen = true;
//            startScreenSharePreview();
            // Update channel media options to publish the screen sharing video stream
            updateMediaPublishOptions(true);
            sharingButton.getBackground().setTint(getColor(R.color.colorGreen));

        } else { // Stop sharing
            agoraEngine.stopScreenCapture();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (fgServiceIntent!=null) stopService(fgServiceIntent);
            }
            isSharingScreen = false;
            sharingButton.getBackground().setTint(Color.GRAY);

            // Restore camera and microphone publishing
            updateMediaPublishOptions(false);
//            agView.addLocalVideo();
        }
    }

    private void startScreenSharePreview() {
        // Create render view by RtcEngine
        FrameLayout container = agView;
        SurfaceView surfaceView = new SurfaceView(getBaseContext());
        if (container.getChildCount() > 0) {
            container.removeAllViews();
        }
        // Add SurfaceView to the local FrameLayout
        container.addView(surfaceView,
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));

        // Setup local video to render your local camera preview
        agoraEngine.setupLocalVideo(new VideoCanvas(surfaceView, Constants.RENDER_MODE_FIT, 0));
        agoraEngine.startPreview(Constants.VideoSourceType.VIDEO_SOURCE_SCREEN_PRIMARY);
    }


    void updateMediaPublishOptions(boolean publishScreen) {
        ChannelMediaOptions mediaOptions = new ChannelMediaOptions();
        mediaOptions.publishCameraTrack = !publishScreen;
        mediaOptions.publishMicrophoneTrack = !publishScreen;
        mediaOptions.publishScreenCaptureVideo = publishScreen;
        mediaOptions.publishScreenCaptureAudio = publishScreen;
        agoraEngine.updateChannelMediaOptions(mediaOptions);
    }


    void joinChannel(){
        Intent intent = getIntent();
        channelName = intent.getStringExtra("channelId");
        RtcTokenBuilder2 tokenBuilder = new RtcTokenBuilder2();

        int timestamp = (int)(System.currentTimeMillis() / 1000 + 3600);
        int uid = generateUid(FirebaseAuth.getInstance().getCurrentUser().getUid());

        String result = tokenBuilder.buildTokenWithUid(appId, appCertificate,
                channelName, uid, Role.ROLE_PUBLISHER, timestamp, timestamp);
        Log.d("TOKEN GENERATOR", "Generated token = " + result);
        token = result;

        agView.join(channelName, token, Constants.CLIENT_ROLE_BROADCASTER, uid);
    }

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    public int generateUid(String userId){
        int uid = userId.hashCode();
        updateUidMapping(userId, uid); // update mapping on Firestore
        return uid;
    }

    private void updateUidMapping(String userId, int uid) {
        DocumentReference docRef = db.collection("liveRooms").document(projectId);

        // Get the document and check if the uidMapping field exists
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Map<String, Integer> uidMapping = (Map<String, Integer>) document.get("uidMapping");
                        if (uidMapping == null) {
                            uidMapping = new HashMap<>();
                        }
                        uidMapping.put(userId, uid);

                        // Update the document with the new uidMapping
                        docRef.update("uidMapping", uidMapping);
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }

    public static void getUserId(String projectId, int uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("liveRooms").document(projectId);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Map<String, Integer> uidMapping = (Map<String, Integer>) document.get("uidMapping");
                        if (uidMapping != null) {
                            for (Map.Entry<String, Integer> entry : uidMapping.entrySet()) {
                                if (entry.getValue().equals(uid)) {
                                    Log.d(TAG, "User ID is: " + entry.getKey());
                                    return;
                                }
                            }
                        }
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }



    public void checkPermissions() {
        if (checkPermission()) {
            joinChannel();
        } else {
            joinButton = new Button(this);
            joinButton.setText(R.string.allow_camera_and_microphone_access_then_click_here_if_you_did_not_join_automatically);
            joinButton.setOnClickListener(new View.OnClickListener() {
                // When the button is clicked, check permissions again and join channel
                @Override
                public void onClick(View view) {
                    if (checkPermission()) {
                        ((ViewGroup) joinButton.getParent()).removeView(joinButton);
                        joinChannel();
                    }
                }
            });
            this.addContentView(joinButton, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 200));

            requestPermission();
        }
    }

    private boolean checkPermission() {
        int resultCam = ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA);
        int resultMic = ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.RECORD_AUDIO);
        return resultCam == PackageManager.PERMISSION_GRANTED && resultMic == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO}, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean microphoneAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (cameraAccepted && microphoneAccepted) {
                        ((ViewGroup) joinButton.getParent()).removeView(joinButton);
                        joinChannel();
                    }
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_live_uikit);
        projectId = getIntent().getStringExtra("projectId");

        initializeAndJoinChannel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(agView != null){
            agView.leaveChannel();
        }
    }
}
