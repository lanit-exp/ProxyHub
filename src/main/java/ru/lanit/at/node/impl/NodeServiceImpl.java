package ru.lanit.at.node.impl;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.yaml.snakeyaml.Yaml;
import ru.lanit.at.node.Node;
import ru.lanit.at.node.NodeService;
import ru.lanit.at.rest.RestApiService;
import ru.lanit.at.util.CommonUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NodeServiceImpl implements NodeService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    private final RestApiService restApiService;

    @Value("${node.list.file}")
    private String nodeListFile;

    private final Map<String, Node> nodes;

    @Autowired
    public NodeServiceImpl(RestApiService restApiService) {
        this.restApiService = restApiService;

        nodes = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void readNodeListFromFile() {

        boolean isNodeListFileExist = new File(nodeListFile).exists();

        if (isNodeListFileExist) {
            readNodesList();
        }
    }

    public void readNodesList() {
        Yaml yaml = new Yaml();

        try (FileInputStream in = new FileInputStream(nodeListFile)) {
            Map<String, Object> elements = yaml.load(StreamUtils.copyToString(in, StandardCharsets.UTF_8));

            JSONArray nodes = new JSONArray(elements.get("nodes").toString());

            for(Object nodeObject : nodes) {
                JSONObject node = (JSONObject) nodeObject;
                Iterator<String> iterator = node.keys();

                while (iterator.hasNext()) {
                    String temp = iterator.next();
                    JSONObject data = node.getJSONObject(temp);

                    String address = String.format("http://%s:%s", data.getString("host"),
                            data.getString("port"));

                    Node newNode = new Node(address);

                    if (checkNodeConnection(temp, newNode)) {
                        this.nodes.put(temp, newNode);
                    }
                }
            }

            logger.info("Start hub with nodes: " + getNodes());
        } catch(FileNotFoundException ex) {
            logger.warn("The file nodes.yaml is missing. You need to check its location or register nodes via API request.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String takeNode(String response, Node node) {
        JSONObject responseBody;
        try {
            responseBody = new JSONObject(response);

            String id;

            if(responseBody.has("sessionId")) {
                id = responseBody.getString("sessionId");
            } else {
                if(responseBody.has("value")) {
                    JSONObject value = (JSONObject) responseBody.get("value");
                    id = value.getString("sessionId");
                } else {
                    id = UUID.randomUUID().toString().replace("-", "");
                }
            }

            node.setSessionId(id);

            logger.info(String.format("Start session with sessionId %s and parameters: %s",
                    node.getSessionId(),
                    responseBody));
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }

        return responseBody.toString();
    }

    private boolean checkNodeConnection(String name, Node node) {
        try {
            URL url = new URL(node.getAddress() + "/health");
            HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
            urlConn.connect();

            if (urlConn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                logger.error("Connection is not established");
            } else {
                logger.info(String.format("Connection with %s is established", node.getAddress()));
                return true;
            }

            urlConn.disconnect();
        } catch (IOException e) {
            logger.error("Error creating HTTP connection with {}", node.getAddress());
        }

        return false;
    }

    @Override
    public Map<String, Node> getNodes() {
        return nodes;
    }

    @Override
    public String registerNode(String name, Node node) {
        if (nodes.containsValue(node)) {
            return "The node is already registered";
        }

        if (checkNodeConnection(name, node)) {
            this.nodes.put(name, node);
            return String.format("The node has been registered successfully. The name - \"" + name + "\"", name);
        } else {
            return String.format("Connection error to node %s", name);
        }
    }

    @Override
    public Node getNode(String key) {
        return nodes.get(key);
    }

    @Override
    public synchronized Node getNodeByName(String name) {
        Node node = getNode(name);

        while (true) {
            if(checkNodeFree(node)) {
                return node;
            }
        }
    }

    public boolean checkNodeFree(Node node) {
        long lastActivity = node.getLastActivity();
        long currentTime = System.currentTimeMillis();

        return (currentTime - lastActivity) > CommonUtils.RESOURCE_TIMEOUT.get();
    }

    @Override
    public Optional<Node> getNodeBySessionId(String sessionId) {
        return getNodes().values().stream()
                .filter(node -> node.getSessionId().equals(sessionId))
                .findFirst();
    }

    @Override
    public synchronized Node getFreeNode() {
        while (true) {
            Optional<Node> freeNode = getNodes().values().stream()
                    .filter(this::checkNodeFree)
                    .findFirst();

            if (freeNode.isPresent()) {
                return freeNode.get();
            }
        }
    }

    public Optional<Node> getWorkedNode(String uri) {
        return getNodes().values().stream()
                .filter(element -> uri.contains(element.getSessionId()))
                .findFirst();
    }

    @Override
    public void releaseNode(Node node) {
        restApiService.executeRequest("GET", node.getAddress(), "/rest/api/v1/release-connections", null);
    }
}
