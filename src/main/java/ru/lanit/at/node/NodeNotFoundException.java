package ru.lanit.at.node;

public class NodeNotFoundException extends Exception {

    public NodeNotFoundException(String message) {
        super(message);
    }

    public NodeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public NodeNotFoundException() {
    }
}
