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

                    try {
                        URL url = new URL(address + "/health");
                        HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
                        urlConn.connect();

                        if (urlConn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                            logger.info(String.valueOf(urlConn.getResponseCode()));
                            logger.error("Connection is not established");
                        } else {
                            logger.info(String.format("Connection with %s is established", address));
                            getNodes().put(temp, new Node(address, CommonUtils.RESOURCE_TIMEOUT.get()));
                        }

                        urlConn.disconnect();
                    } catch (IOException e) {
                        logger.error("Error creating HTTP connection with {}", address);
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
            node.startTimer();

            logger.info(String.format("Start session with sessionId %s and parameters: %s",
                    node.getSessionId(),
                    responseBody));
        } catch (Exception e) {
            node.setTimeout(0);
            node.setFree(true);

            e.printStackTrace();
            return e.getMessage();
        }

        return responseBody.toString();
    }

    @Override
    public Map<String, Node> getNodes() {
        return nodes;
    }

    @Override
    public void setNode(String name, Node node) {
        this.nodes.put(name, node);
    }

    @Override
    public Node getNode(String key) {
        return nodes.get(key);
    }

    @Override
    public synchronized Node getNodeByName(String name) {
        Node node = getNode(name);

        while (true) {
            if(node.isFree()) {
                node.setFree(false);
                break;
            }
        }

        return node;
    }

    @Override
    public Optional<Node> getNodeBySessionId(String sessionId) {
        return getNodes().values().stream()
                .filter(node -> node.getSessionId().equals(sessionId))
                .findFirst();
    }

    @Override
    public synchronized Node getFreeNode() {
        Node node;

        while (true) {
            Optional<Map.Entry<String, Node>> freeNode = getNodes().entrySet().stream()
                    .filter(entry -> entry.getValue().isFree())
                    .findFirst();

            if (freeNode.isPresent()) {
                node = freeNode.get().getValue();
                node.setFree(false);

                break;
            }
        }

        return node;
    }

    public Optional<Node> getWorkedNode(String uri) {
        return getNodes().values().stream()
                .filter(element -> uri.contains(element.getSessionId()))
                .findFirst();
    }

    @Override
    public void releaseNode(Node node) {
        restApiService.executeRequest("GET", node.getAddress(), "/rest/api/v1/release-connections", null);

        node.setFree(true);
        node.setSessionId("");
        node.setTimeout(0);
    }
}
