package ru.lanit.at.node;

import java.util.Map;
import java.util.Optional;

public interface NodeService {
    String takeNode(String response, Node node);

    Map<String, Node> getNodes();

    void setNode(String name, Node node);

    Node getNode(String key);

    Node getNodeByName(String name);

    Optional<Node> getNodeBySessionId(String sessionId);

    Node getFreeNode();

    Optional<Node> getWorkedNode(String uri);

    void releaseNode(Node node);
}
