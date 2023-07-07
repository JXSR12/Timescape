package edu.bluejack22_2.timescape2.ui.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class DashboardViewModel extends ViewModel {

    private final MutableLiveData<String> mText;
    private final MutableLiveData<Boolean> dialogDismissed = new MutableLiveData<>();


    public DashboardViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Your project management dashboard");
    }

    public LiveData<String> getText() {
        return mText;
    }

    public void setDialogDismissed(boolean isDismissed) {
        dialogDismissed.setValue(isDismissed);
    }

    public LiveData<Boolean> getDialogDismissed() {
        return dialogDismissed;
    }
}