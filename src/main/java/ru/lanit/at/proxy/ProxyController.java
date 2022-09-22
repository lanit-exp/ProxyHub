package ru.lanit.at.proxy;

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
import ru.lanit.at.api.RequestService;
import ru.lanit.at.node.Node;
import ru.lanit.at.node.NodeService;
import ru.lanit.at.utils.CommonUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/")
public class ProxyController {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

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
    public ResponseEntity<?> sendRequest(@Context HttpServletRequest request) {
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

        HttpResponse<String> response;
        String method = request.getMethod();

        try {
            response = sendRequest(method, request, body, url);

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

        if(!response.getBody().isEmpty()) {
            return new ResponseEntity<>(response.getBody(), HttpStatus.OK);
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

                HttpResponse<String> response;
                String method = request.getMethod();

                try {
                    response = sendRequest(method, request, body, node.getAddress());
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

    public HttpResponse<String> sendRequest(String method, HttpServletRequest request, String body, String url) {
        String uri = request.getRequestURI();
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
            case "DELETE":
                response = requestService.doDelete(headers, url, uri);
                break;
            default:
                response = requestService.doGet(headers, url, uri);
                break;
        }

        return response;
    }
}
