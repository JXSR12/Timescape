package edu.bluejack22_2.timescape;

import android.os.Build;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.FirebaseFirestore;

import edu.bluejack22_2.timescape.model.ApkVersion;

public class UpdaterViewModel extends ViewModel {

    public LiveData<ApkVersion> getLatestApkVersion() {
        MutableLiveData<ApkVersion> apkVersionLiveData = new MutableLiveData<>();

        String arch = Build.SUPPORTED_ABIS[0];
        String docId = switch (arch) {
            case "arm64-v8a" -> "latest-apk-arm64-v8a";
            case "armeabi-v7a", "armeabi" -> "latest-apk-armeabi-v7a";
            case "x86_64" -> "latest-apk-x86-64";
            case "x86" -> "latest-apk-x86";
            default -> "latest-apk";
        };

        FirebaseFirestore.getInstance().collection("distributions")
                .document(docId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    ApkVersion apkVersion = documentSnapshot.toObject(ApkVersion.class);
                    apkVersionLiveData.setValue(apkVersion);
                });
        return apkVersionLiveData;
    }

}
