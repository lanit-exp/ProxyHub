package ru.lanit.at.element;

import org.springframework.stereotype.Component;

import java.util.Objects;

public class Node {
    private String address;
    private boolean free;
    private String idSession;

    public Node(String address) {
        this.address = address;
        this.free = false;
        this.idSession = " ";
    }

    public boolean isFree() {
        return free;
    }

    public void setFree(boolean free) {
        this.free = free;
    }

    public String getIdSession() {
        return idSession;
    }

    public void setIdSession(String idSession) {
        this.idSession = idSession;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "Node {" +
                "address='" + address + '\'' +
                ", free=" + free +
                ", idSession='" + idSession + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return free == node.free &&
                Objects.equals(address, node.address) &&
                Objects.equals(idSession, node.idSession);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, free, idSession);
    }
}
