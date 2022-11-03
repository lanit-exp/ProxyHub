package ru.lanit.at.node;

import com.fasterxml.jackson.annotation.JsonFilter;

import java.util.Objects;

@JsonFilter("nodeFilter")
public class Node {

    private String address;
    private String sessionId;
    private long lastActivity;

    public Node(String address) {
        this.address = address;
        this.sessionId = "";
        this.lastActivity = 0;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public long getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(long lastActivity) {
        this.lastActivity = lastActivity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return Objects.equals(address, node.address) &&
                Objects.equals(sessionId, node.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, sessionId);
    }

    @Override
    public String toString() {
        return "Node{" +
                "address='" + address + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", lastActivity='" + lastActivity + '\'' +
                '}';
    }
}
