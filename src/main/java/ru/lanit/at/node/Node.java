package ru.lanit.at.node;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.lanit.at.util.CommonUtils;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

@JsonFilter("nodeFilter")
public class Node {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private String address;
    private boolean free;
    private String sessionId;
    private Timer timer;
    private final AtomicInteger timeout;

    public Node(String address, int timeout) {
        this.address = address;
        this.free = true;
        this.sessionId = "";
        this.timer = new Timer();
        this.timeout = new AtomicInteger(timeout);
    }

    public boolean isFree() {
        return free;
    }

    public void setFree(boolean free) {
        this.free = free;
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

    /*TODO - maybe it doesn't work*/
    public void startTimer() {
        timeout.set(CommonUtils.RESOURCE_TIMEOUT.get());
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                logger.info(address + " - " + sessionId + " - " + timeout.get());
                if (timeout.get() < 1) {
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
        this.setSessionId("");
    }

    public void setTimeout(int timeout) {
        this.timeout.set(timeout);
    }

    public void increaseTimeout(int value) {
        this.timeout.addAndGet(value);
    }

    public String getTimeout() {
        return String.valueOf(timeout.get());
    }

    public Timer getTimer() {
        return timer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return free == node.free &&
                Objects.equals(address, node.address) &&
                Objects.equals(sessionId, node.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, free, sessionId);
    }

    @Override
    public String toString() {
        return "Node{" +
                "address='" + address + '\'' +
                ", free=" + free +
                ", sessionId='" + sessionId + '\'' +
                ", timeout=" + timeout +
                '}';
    }
}
