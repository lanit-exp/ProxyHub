package ru.lanit.at.node;

import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import kong.unirest.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ru.lanit.at.util.CommonUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/rest/api/v1/node")
public class NodeController {
    private final NodeService nodeService;

    @Autowired
    public NodeController(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @RequestMapping(value = "/status", method = RequestMethod.GET)
    public ResponseEntity<?> getNodes() {
        SimpleBeanPropertyFilter filter = SimpleBeanPropertyFilter.serializeAllExcept("timer");
        FilterProvider filterProvider = new SimpleFilterProvider().addFilter("nodeFilter", filter);

        MappingJacksonValue mappingJacksonValue = new MappingJacksonValue(nodeService.getNodes());
        mappingJacksonValue.setFilters(filterProvider);

        return new ResponseEntity<>(mappingJacksonValue, HttpStatus.OK);
    }

    @RequestMapping(value = "/register", method = RequestMethod.POST,
            headers="content-type=application/json;charset=UTF-8",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> registerNode(HttpServletRequest request) {
        String body;

        try (BufferedReader reader = request.getReader()) {
            body = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            return new ResponseEntity<>("Error: " + Arrays.toString(e.getStackTrace()), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        JSONObject jsonBody = new JSONObject(body);
        String name;

        if(!jsonBody.has("host") || !jsonBody.has("port")) {
            return new ResponseEntity<>("Host and/or port is missing.", HttpStatus.BAD_REQUEST);
        } else {
            String address = String.format("http://%s:%s", jsonBody.get("host"), jsonBody.get("port"));
            Node node = new Node(address);

            if(jsonBody.has("nodeName")) {
                name = jsonBody.getString("nodeName");
            } else {
                name = UUID.randomUUID().toString().replace("-", "");
            }

            nodeService.setNode(name, node);
        }

        return new ResponseEntity<>("The node has been registered successfully. The name - \"" + name + "\"", HttpStatus.OK);
    }

    @RequestMapping(value = "/clear", method = RequestMethod.GET)
    public ResponseEntity<String> deleteAllNodes() {
        nodeService.getNodes().clear();
        return new ResponseEntity<>("Information has been deleted.", HttpStatus.OK);
    }

    @RequestMapping(value = "/delete/{name}", method = RequestMethod.GET)
    public ResponseEntity<String> deleteNodes(@PathVariable String name) {
        nodeService.getNodes().remove(name);
        return new ResponseEntity<>(String.format("Information about node %s has been deleted.", name), HttpStatus.OK);
    }
}
