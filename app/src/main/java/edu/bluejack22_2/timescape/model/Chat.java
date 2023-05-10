package edu.bluejack22_2.timescape.model;

public class Chat {
    private String projectId;
    private String projectTitle;
    private Message latestMessage;
    private String senderName;
    private String timestamp;
    private int unreadCount;
    private Message.MessageType messageType;

    public Chat(){

    }

    public Chat(String projectId, String projectTitle, Message latestMessage, String senderName, String timestamp, int unreadCount, Message.MessageType messageType) {
        this.projectId = projectId;
        this.projectTitle = projectTitle;
        this.latestMessage = latestMessage;
        this.senderName = senderName;
        this.timestamp = timestamp;
        this.unreadCount = unreadCount;
        this.messageType = messageType;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    public Message getLatestMessage() {
        return latestMessage;
    }

    public void setLatestMessage(Message latestMessage) {
        this.latestMessage = latestMessage;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public Message.MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(Message.MessageType messageType) {
        this.messageType = messageType;
    }
}
