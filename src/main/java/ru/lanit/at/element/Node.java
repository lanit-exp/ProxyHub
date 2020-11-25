package ru.lanit.at.element;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public class Node {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private String address;
    private boolean free;
    private String idSession;
    private Timer timer;
    private AtomicInteger timeout;

    public Node(String address, int timeout) {
        this.address = address;
        this.free = true;
        this.idSession = " ";
        this.timer = new Timer();
        this.timeout = new AtomicInteger(timeout);
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

    public void startTimer() {
        timeout.set(60);
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (timeout.get() <= 0) {
                    logger.info("Освобождение ресурсов по таймауту.");
                    clearInfo();
                    timer.cancel();
                } else {
                    timeout.decrementAndGet();
                }
            }
        }, 0, 1000);
    }

    private void clearInfo() {
        this.setFree(true);
        this.setIdSession(" ");
    }

    public void setTimeout(int timeout) {
        this.timeout.set(timeout);
    }

    public String getTimeout() {
        return String.valueOf(timeout.get());
    }

    public Timer getTimer() {
        return timer;
    }

    @Override
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        String jsonString = "";
        try {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            jsonString = mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return jsonString;
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
