package edu.bluejack22_2.timescape;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.FirebaseFirestore;

import edu.bluejack22_2.timescape.model.ApkVersion;

public class UpdaterViewModel extends ViewModel {

    public LiveData<ApkVersion> getLatestApkVersion() {
        MutableLiveData<ApkVersion> apkVersionLiveData = new MutableLiveData<>();
        FirebaseFirestore.getInstance().collection("distributions")
                .document("latest-apk")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    ApkVersion apkVersion = documentSnapshot.toObject(ApkVersion.class);
                    apkVersionLiveData.setValue(apkVersion);
                });
        return apkVersionLiveData;
    }
}
