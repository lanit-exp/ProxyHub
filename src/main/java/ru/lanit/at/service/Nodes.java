package ru.lanit.at.service;

import org.springframework.stereotype.Service;
import ru.lanit.at.element.Node;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class Nodes {
    private Map<String, Node> nodeConcurrentHashMap;

    public Nodes() {
        nodeConcurrentHashMap = new ConcurrentHashMap<>();
    }

    public Map<String, Node> getNodeConcurrentHashMap() {
        return nodeConcurrentHashMap;
    }

    public void setNode(String name, Node node) {
        this.nodeConcurrentHashMap.put(name, node);
    }

    public Node getNode(String key) {
        return nodeConcurrentHashMap.get(key);
    }

    @Override
    public String toString() {
        return "Nodes { " +
                nodeConcurrentHashMap +
                " }";
    }
}
