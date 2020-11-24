package ru.lanit.at.controller;

import io.swagger.annotations.*;
import kong.unirest.json.JSONObject;
import org.mapstruct.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ru.lanit.at.element.Node;
import ru.lanit.at.service.Nodes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api")
public class NodeController {
    private Nodes nodes;
    private static int timeout;

    @Autowired
    public NodeController(Nodes nodes) {
        this.nodes = nodes;
        timeout = 60;
    }

    @RequestMapping(value = "/register/node", method = RequestMethod.POST,
            headers="content-type=application/json;charset=UTF-8",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Регистрация узла через отправку JSON. При желании можно указать имя узла через параметр \"applicationName\".")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The node has been registered successfully.", response = String.class),
            @ApiResponse(code = 500, message = "Error", response = String.class),
            @ApiResponse(code = 400, message = "Host and/or port is missing.", response = String.class)
    })
    public ResponseEntity<String> registerNode(@Context HttpServletRequest request) {
        String body;

        try {
            body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            return new ResponseEntity<>("Error: " + Arrays.toString(e.getStackTrace()), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        JSONObject jsonBody = new JSONObject(body);
        String name;

        if(!jsonBody.has("host") || !jsonBody.has("port")) {
            return new ResponseEntity<>("Host and/or port is missing.", HttpStatus.BAD_REQUEST);
        } else {
            String address = String.format("http://%s:%s", jsonBody.get("host"), jsonBody.get("port"));
            Node node = new Node(address, timeout);

            if(jsonBody.has("applicationName")) {
                name = jsonBody.getString("applicationName");
            } else {
                name = UUID.randomUUID().toString().replace("-", "");
            }

            nodes.setNode(name, node);
        }

        return new ResponseEntity<>("The node has been registered successfully. The name - \"" + name + "\"", HttpStatus.OK);
    }

    @RequestMapping(value = "/delete/nodes", method = RequestMethod.GET)
    @ApiOperation(value = "Удаление информации о текущих узлах.")
    public ResponseEntity<String> deleteNodes() {
        nodes.getNodeConcurrentHashMap().clear();
        return new ResponseEntity<>("Information has been deleted.", HttpStatus.OK);
    }

    @RequestMapping(value = "/delete/node/{name}", method = RequestMethod.GET)
    @ApiOperation(value = "Удаление информации об определенном узле. В качестве параметра используется \"applicationName\" узла.")
    public ResponseEntity<String> deleteNodes(@PathVariable String name) {
        nodes.getNodeConcurrentHashMap().remove(name);
        return new ResponseEntity<>(String.format("Information about node %s has been deleted.", name), HttpStatus.OK);
    }

    @RequestMapping(value = "/set/timeout/{value}", method = RequestMethod.GET)
    @ApiOperation(value = "Изменение значения параметра таймаута.")
    public ResponseEntity<String> setTimeout(@PathVariable int value) {
        this.timeout = value;

        for(Node x : nodes.getNodeConcurrentHashMap().values()) {
            x.setTimeout(value);
        }

        return new ResponseEntity<>(String.format("Set timeout value %s", value), HttpStatus.OK);
    }

    public static int getTimeout() {
        return timeout;
    }
}
