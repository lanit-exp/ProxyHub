package ru.lanit.at.node;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.lanit.at.utils.CommonUtils;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

@JsonFilter("nodeFilter")
public class Node {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private String address;
    private boolean free;
    private String idSession;
    private Timer timer;
    private final AtomicInteger timeout;

    public Node(String address, int timeout) {
        this.address = address;
        this.free = true;
        this.idSession = "";
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

    /*TODO - maybe it doesn't work*/
    public void startTimer() {
        timeout.set(CommonUtils.RESOURCE_TIMEOUT.get());
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
        this.setIdSession("");
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
                Objects.equals(idSession, node.idSession);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, free, idSession);
    }

    @Override
    public String toString() {
        return "Node{" +
                "address='" + address + '\'' +
                ", free=" + free +
                ", idSession='" + idSession + '\'' +
                ", timeout=" + timeout +
                '}';
    }
}
