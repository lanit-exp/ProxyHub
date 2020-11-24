package ru.lanit.at.service;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.yaml.snakeyaml.Yaml;
import ru.lanit.at.Application;
import ru.lanit.at.controller.NodeController;
import ru.lanit.at.element.Node;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

@Service
public class FileReaderService {
    private Logger logger = LoggerFactory.getLogger(Application.class);

    private Nodes nodes;

    @Autowired
    public FileReaderService(Nodes nodes) {
        this.nodes = nodes;
    }

    public void readListConnections() {
        Node connection;
        Yaml yaml = new Yaml();

        try(FileInputStream in = new FileInputStream(new File("nodes.yaml"))) {
            Map<String, Object> elements = yaml.load(StreamUtils.copyToString(in, StandardCharsets.UTF_8));

            logger.info("Start hub with parameters: " + elements.get("nodes").toString());

            JSONObject jsonObject = new JSONObject(elements);
            JSONArray jsonArray = new JSONArray(jsonObject.get("nodes").toString());

            for(Object jsonElement : jsonArray.toList()) {
                JSONObject jsonObject1 = new JSONObject(jsonElement.toString());
                Iterator<String> iterator = jsonObject1.keys();

                while (iterator.hasNext()) {
                    String temp = iterator.next();
                    JSONObject jsonObject2 = jsonObject1.getJSONObject(temp);

                    String address = String.format("http://%s:%s", jsonObject2.getString("host"),
                            jsonObject2.getString("port"));
                    connection = new Node(address, NodeController.getTimeout());

                    nodes.getNodeConcurrentHashMap().put(temp, connection);
                }
            }
        } catch(FileNotFoundException ex) {
            logger.warn("The file nodes.yaml is missing. You need to check its location or register nodes via API request.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
