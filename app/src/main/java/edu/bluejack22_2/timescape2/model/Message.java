package edu.bluejack22_2.timescape2.model;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class Message {
    public enum MessageType {
        TEXT, IMAGE, FILE, PROJECT_INVITE, DATE_SEPARATOR, UNSENT, REPLY;

        public static MessageType fromString(String value) {
            return MessageType.valueOf(value.toUpperCase());
        }
    }

    private String id;
    private DocumentReference sender;
    private MessageType message_type;
    private String content;
    private String fileName;
    private String replyingTo;
    private Timestamp timestamp;
    private Date timestampAsDate;
    private HashMap<String, String> mentions;
    private List<String> reads;
    public Message(){

    }
    public Message(String id, DocumentReference sender, MessageType message_type, String content, Timestamp timestamp) {
        this.id = id;
        this.sender = sender;
        this.message_type = message_type;
        this.content = content;
        this.timestamp = timestamp;
    }

    public List<String> getReads() {
        return reads;
    }

    public void setReads(List<String> reads) {
        this.reads = reads;
    }

    public HashMap<String, String> getMentions() {
        return mentions;
    }

    public void setMentions(HashMap<String, String> mentions) {
        this.mentions = mentions;
    }

    public String getReplyingTo() {
        return replyingTo;
    }

    public void setReplyingTo(String replyingTo) {
        this.replyingTo = replyingTo;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DocumentReference getSender() {
        return sender;
    }

    public void setSender(DocumentReference sender) {
        this.sender = sender;
    }

    public MessageType getMessage_type() {
        return message_type;
    }

    public void setMessage_type(MessageType message_type) {
        this.message_type = message_type;
    }

    public Task<DocumentSnapshot> fetchSenderData() {
        return sender.get();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
    public Date getTimestampAsDate() {
        return timestamp != null ? timestamp.toDate() : Date.from(Instant.now());
    }
    public void setTimestampAsDate(Date timestampAsDate) {
        this.timestampAsDate = timestampAsDate;
    }

    public boolean isCurrentUser(String currentUserId) {
        return sender.getId().equals(currentUserId);
    }
}

