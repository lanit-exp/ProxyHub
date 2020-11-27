package ru.lanit.at.controller;

import io.swagger.annotations.ApiOperation;
import kong.unirest.HttpResponse;
import kong.unirest.json.JSONObject;
import org.mapstruct.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ru.lanit.at.element.Node;
import ru.lanit.at.service.Nodes;
import ru.lanit.at.service.RequestService;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/")
public class HubController {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private Nodes nodes;
    private RequestService requestService;

    @Autowired
    public HubController(Nodes nodes, RequestService requestService) {
        this.nodes = nodes;
        this.requestService = requestService;
    }

    @RequestMapping(value = {"/**"},
            method = {RequestMethod.POST, RequestMethod.GET, RequestMethod.DELETE})
    @ApiOperation(value = "Отправка запроса в proxy driver.")
    public ResponseEntity<?> sendRequest(@Context HttpServletRequest request) {
        return resultRequest(request);
    }

    public ResponseEntity<?> resultRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();

        if(uri.equals("/session")) {
            return createSession(request);
        }

        String body = "";
        String url;

        try {
            if ("POST".equalsIgnoreCase(request.getMethod())) {
                body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error of proxy driver: \n" + e.toString(), HttpStatus.BAD_REQUEST);
        }

        Optional<Node> workNode = getWorkNode(uri);

        if(workNode.isPresent()) {
            url = workNode.get().getAddress();
        } else {
            return new ResponseEntity<>("The node isn't listed.", HttpStatus.BAD_REQUEST);
        }

        HttpResponse<String> response;
        String method = request.getMethod();

        try {
            response = getResponse(method, request, body, url, uri);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error of proxy driver: \n" + e.toString(), HttpStatus.BAD_REQUEST);
        }

        if ("DELETE".equalsIgnoreCase(method)) {
            workNode.get().setFree(true);
            workNode.get().setIdSession(" ");
            workNode.get().getTimer().cancel();
        }

        workNode.get().setTimeout(NodeController.getTimeout());

        if(!response.getBody().isEmpty()) {
            return new ResponseEntity<>(response.getBody(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.OK);
        }
    }

    public ResponseEntity<?> createSession(HttpServletRequest request) {
        Optional<Node> node;
        String body;
        String uri = request.getRequestURI();

        if(nodes.getNodeConcurrentHashMap().isEmpty()) {
            return new ResponseEntity<>("List of nodes is empty!",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        try {
            body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));

            JSONObject jsonObject = new JSONObject(body);
            JSONObject desiredCapabilities = (JSONObject) jsonObject.get("desiredCapabilities");

            if(desiredCapabilities.has("nodeName")) {
                node = getNamedNode(desiredCapabilities.getString("nodeName"));

                if (!node.isPresent()) {
                    return new ResponseEntity<>(String.format("Node %s not found in list", desiredCapabilities.get("nodeName")),
                            HttpStatus.INTERNAL_SERVER_ERROR);
                }
            } else {
                node = Optional.ofNullable(nodes.getNode(getRandomNameNode()));
            }

            HttpResponse<String> response;
            String method = request.getMethod();

            if(node.isPresent()) {
                try {
                    response = getResponse(method, request, body, node.get().getAddress(), uri);
                } catch (Exception e) {
                    node.get().getTimer().cancel();
                    node.get().setFree(true);
                    return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
                }
            } else {
                return new ResponseEntity<>("List is empty or all nodes are busy.", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            return new ResponseEntity<>(startNode(response, node.get()), HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error of proxy driver: \n" + e.toString(), HttpStatus.BAD_REQUEST);
        }
    }

    public String startNode(HttpResponse<String> response, Node node) {
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

            logger.info(String.format("Start session with sessionId = %s and parameters: %s",
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

    public HttpResponse<String> getResponse(String method, HttpServletRequest request, String body, String url, String uri) {
        HttpResponse<String> response;

        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();

        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                headers.put(headerName, request.getHeader(headerName));
            }
        }

        switch (method) {
            case "POST":
                response = requestService.doPost(headers, body, url, uri);
                break;
            case "GET":
                response = requestService.doGet(headers, url, uri);
                break;
            default:
                response = requestService.doDelete(headers, url, uri);
                break;
        }

        return response;
    }

    private synchronized String getRandomNameNode() {
        Optional<String> nameFreeNode = Optional.empty();

        while (true) {
            for(Map.Entry<String, Node> element : nodes.getNodeConcurrentHashMap().entrySet()) {
                if(element.getValue().isFree()) {
                    nameFreeNode = Optional.ofNullable(element.getKey());
                    takeNode(element.getValue());
                    break;
                }
            }

            if(nameFreeNode.isPresent()) {
                break;
            } else {
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        return nameFreeNode.get();
    }

    private Optional<Node> getWorkNode(String uri) {
        return nodes.getNodeConcurrentHashMap()
                .values()
                .stream()
                .filter(element -> uri.contains(element.getIdSession()))
                .findFirst();
    }

    private synchronized Optional<Node> getNamedNode(String name) throws InterruptedException {
        Optional<Node> node = Optional.ofNullable(nodes.getNode(name));

        if(node.isPresent()) {
            while (true) {
                if(node.get().isFree()) {
                    node.get().setFree(false);
                    break;
                } else {
                    TimeUnit.SECONDS.sleep(5);
                }
            }
        }

        return node;
    }

    private void takeNode(Node node) {
        node.setFree(false);
    }

    @RequestMapping(value = "/status", method = RequestMethod.GET)
    @ApiOperation(value = "Получение информации о текущих узлах.")
    public ResponseEntity<List<Map<String, String>>> getNodes() {
        return new ResponseEntity<>(getAllNodes(), HttpStatus.OK);
    }

    public List<Map<String, String>> getAllNodes() {
        List<Map<String, String>> elements = new ArrayList<>();

        for (Map.Entry<String, Node> node : nodes.getNodeConcurrentHashMap().entrySet()) {
            Map<String, String> element = new HashMap<>();
            element.put("nodeName", node.getKey());
            element.put("address", node.getValue().getAddress());
            element.put("timeout", String.valueOf(NodeController.getTimeout()));

            if(node.getValue().isFree()) {
                element.put("isFree", "Yes");
            } else {
                element.put("isFree", "No");
            }

            element.put("idSession", node.getValue().getIdSession());

            elements.add(element);
        }
        return elements;
    }
}
