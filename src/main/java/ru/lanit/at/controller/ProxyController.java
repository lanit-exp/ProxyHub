package ru.lanit.at.controller;

import io.swagger.annotations.ApiOperation;
import kong.unirest.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ru.lanit.at.rest.RequestService;
import ru.lanit.at.node.Node;
import ru.lanit.at.node.NodeService;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping(value = "/")
public class ProxyController {

    private final NodeService nodeService;
    private final RequestService requestService;

    @Autowired
    public ProxyController(NodeService nodeService, RequestService requestService) {
        this.nodeService = nodeService;
        this.requestService = requestService;
    }

    @RequestMapping(value = "/session", method = RequestMethod.POST)
    @ApiOperation(value = "Старт сессии.")
    public ResponseEntity<?> startSession(HttpServletRequest request) {
        if(nodeService.getNodes().isEmpty()) {
            return new ResponseEntity<>("List of nodes is empty!",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return createSession(request);
    }

    @RequestMapping(value = {"/**"},
            method = {RequestMethod.POST, RequestMethod.GET, RequestMethod.DELETE})
    @ApiOperation(value = "Отправка запроса в proxy driver.")
    public ResponseEntity<?> proxyRequest(@Context HttpServletRequest request) {
        String url;
        String uri = request.getRequestURI();
        String body = "";

        try {
            if ("POST".equalsIgnoreCase(request.getMethod())) {
                try (BufferedReader reader = request.getReader()) {
                    body = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error of proxy driver: \n" + e.toString(), HttpStatus.BAD_REQUEST);
        }

        Optional<Node> workNodeOptional = nodeService.getWorkedNode(uri);
        Node workNode;

        if(workNodeOptional.isPresent()) {
            workNode = workNodeOptional.get();
        } else {
            return new ResponseEntity<>("The session isn't exist.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        url = workNode.getAddress();

        String response;
        String method = request.getMethod();

        try {
            if (body.isEmpty()) {
                response = requestService.executeRequest(method, request, url, uri);
            } else {
                response = requestService.executeRequest(method, request, body, url, uri);
            }

            if ("DELETE".equalsIgnoreCase(method)) {
                nodeService.releaseNode(workNode);
            } else {
                workNode.increaseTimeout(10);
            }
        } catch (Exception e) {
            nodeService.releaseNode(workNode);

            e.printStackTrace();
            return new ResponseEntity<>("Error of proxy driver: \n" + e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if(!response.isEmpty()) {
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.OK);
        }
    }

    public ResponseEntity<?> createSession(HttpServletRequest request) {
        Node node;

        try {
            try (BufferedReader reader = request.getReader()) {
                String body = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                JSONObject desiredCapabilities = new JSONObject(body).getJSONObject("desiredCapabilities");

                if(desiredCapabilities.has("nodeName")) {
                    String nodeName = desiredCapabilities.getString("nodeName");

                    if (!nodeService.getNodes().containsKey(nodeName)) {
                        return new ResponseEntity<>(String.format("Node %s not found in list", desiredCapabilities.get("nodeName")),
                                HttpStatus.INTERNAL_SERVER_ERROR);
                    }

                    node = nodeService.getNodeByName(desiredCapabilities.getString("nodeName"));
                } else {
                    node = nodeService.getFreeNode();
                }

                String response;
                String method = request.getMethod();

                try {
                    response = requestService.executeRequest(method, request, body, node.getAddress(), "/session");
                } catch (Exception e) {
                    node.setFree(true);
                    return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
                }

                return new ResponseEntity<>(nodeService.takeNode(response, node), HttpStatus.OK);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error of proxy driver: \n" + e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
