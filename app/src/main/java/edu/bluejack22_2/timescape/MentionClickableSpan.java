package edu.bluejack22_2.timescape;

import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

import androidx.annotation.NonNull;

class MentionClickableSpan extends ClickableSpan {
    private String userId;

    public MentionClickableSpan(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    @Override
    public void onClick(@NonNull View widget) {
        // Handle mention click if needed
    }

    @Override
    public void updateDrawState(@NonNull TextPaint ds) {
        super.updateDrawState(ds);
        ds.setUnderlineText(false); // Remove underline
    }
}