package edu.bluejack22_2.timescape2.model;

import java.util.List;

public class ChatDocument {
    private List<Message> messages;

    public ChatDocument() {
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}
