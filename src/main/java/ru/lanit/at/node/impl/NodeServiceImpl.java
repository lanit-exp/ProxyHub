package ru.lanit.at.node.impl;

import kong.unirest.HttpResponse;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.yaml.snakeyaml.Yaml;
import ru.lanit.at.node.Node;
import ru.lanit.at.node.NodeService;
import ru.lanit.at.utils.CommonUtils;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NodeServiceImpl implements NodeService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    @Value("${node.list.file}")
    private String nodeListFile;

    private final Map<String, Node> nodes;

    public NodeServiceImpl() {
        nodes = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void readNodeListFromFile() {
        Yaml yaml = new Yaml();

        try (FileInputStream in = new FileInputStream(nodeListFile)) {
            Map<String, Object> elements = yaml.load(StreamUtils.copyToString(in, StandardCharsets.UTF_8));

            JSONArray nodes = new JSONArray(elements.get("nodes").toString());
            logger.info("Start hub with nodes: " + nodes);

            for(Object nodeObject : nodes) {
                JSONObject node = (JSONObject) nodeObject;
                Iterator<String> iterator = node.keys();

                while (iterator.hasNext()) {
                    String temp = iterator.next();
                    JSONObject data = node.getJSONObject(temp);

                    String address = String.format("http://%s:%s", data.getString("host"),
                            data.getString("port"));

                    getNodes().put(temp, new Node(address, CommonUtils.RESOURCE_TIMEOUT.get()));
                }
            }
        } catch(FileNotFoundException ex) {
            logger.warn("The file nodes.yaml is missing. You need to check its location or register nodes via API request.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String takeNode(HttpResponse<String> response, Node node) {
        JSONObject responseBody;
        try {
            responseBody = new JSONObject(response.getBody());

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

            node.setIdSession(id);
            node.startTimer();

            logger.info(String.format("Start session with sessionId %s and parameters: %s",
                    node.getIdSession(),
                    responseBody.toString()));
        } catch (Exception e) {
            node.getTimer().cancel();
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
        return getNodes()
                .values()
                .stream()
                .filter(element -> uri.contains(element.getIdSession()))
                .findFirst();
    }

    @Override
    public void releaseNode(Node node) {
        node.setFree(true);
        node.setIdSession("");
        node.getTimer().cancel();
    }
}
