package edu.bluejack22_2.timescape.model;

import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;

public class InboxMessage {
    private String receiverUserId;
    private String title;
    private String content;
    private Timestamp sentTime;
    private boolean isRead;

    public InboxMessage(){

    }

    public InboxMessage(String receiverUserId, String title, String content, Timestamp sentTime, boolean isRead) {
        this.receiverUserId = receiverUserId;
        this.title = title;
        this.content = content;
        this.sentTime = sentTime;
        this.isRead = isRead;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this.receiverUserId.equals(((InboxMessage)obj).receiverUserId)
                && this.title.equals(((InboxMessage)obj).title)
                && this.content.equals(((InboxMessage)obj).content);
    }

    public String getReceiverUserId() {
        return receiverUserId;
    }

    public void setReceiverUserId(String receiverUserId) {
        this.receiverUserId = receiverUserId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Timestamp getSentTime() {
        return sentTime;
    }

    public void setSentTime(Timestamp sentTime) {
        this.sentTime = sentTime;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}
