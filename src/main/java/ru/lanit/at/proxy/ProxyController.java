package ru.lanit.at.proxy;

import kong.unirest.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ru.lanit.at.node.Node;
import ru.lanit.at.node.NodeNotFoundException;
import ru.lanit.at.node.NodeService;
import ru.lanit.at.proxy.exception.proxy.ProxyRequestHandlerException;
import ru.lanit.at.proxy.exception.proxy.ProxyRestApiResponseException;
import ru.lanit.at.proxy.exception.session.CreateSessionException;
import ru.lanit.at.proxy.exception.session.SessionNotFoundException;
import ru.lanit.at.rest.RestApiService;
import ru.lanit.at.util.CommonUtils;

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
    private final RestApiService restApiService;

    @Autowired
    public ProxyController(NodeService nodeService, RestApiService restApiService) {
        this.nodeService = nodeService;
        this.restApiService = restApiService;
    }

    @RequestMapping(value = "/session", method = RequestMethod.POST)
    public ResponseEntity<?> startSession(HttpServletRequest request) throws NodeNotFoundException, CreateSessionException {
        if(nodeService.getNodes().isEmpty()) {
            throw new NodeNotFoundException("List of nodes is empty!");
        }

        Node node;

        try {
            try (BufferedReader reader = request.getReader()) {
                String body = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                JSONObject desiredCapabilities = new JSONObject(body).getJSONObject("desiredCapabilities");

                if(desiredCapabilities.has("nodeName")) {
                    String nodeName = desiredCapabilities.getString("nodeName");

                    if (!nodeService.getNodes().containsKey(nodeName)) {
                        throw new NodeNotFoundException(String.format("Node %s not found in list", desiredCapabilities.get("nodeName")));
                    }

                    node = nodeService.getNodeByName(desiredCapabilities.getString("nodeName"));
                } else if (desiredCapabilities.has("sessionId")) {
                    Optional<Node> nodeOptional = nodeService.getNodeBySessionId(desiredCapabilities.getString("sessionId"));

                    if (nodeOptional.isPresent()) {
                        node = nodeOptional.get();
                    } else {
                        throw new NodeNotFoundException("Node is not found by session id");
                    }
                } else {
                    node = nodeService.getFreeNode();
                }

                String response;
                String method = request.getMethod();

                try {
                    response = restApiService.executeRequest(method, request, body, node.getAddress(), "/session");
                } catch (Exception e) {
                    node.setFree(true);
                    throw new ProxyRestApiResponseException(e);
                }

                return new ResponseEntity<>(nodeService.takeNode(response, node), HttpStatus.OK);
            }
        } catch (Exception e) {
            throw new CreateSessionException(e);
        }
    }

    @RequestMapping(value = {"/**"},
            method = {RequestMethod.POST, RequestMethod.GET, RequestMethod.DELETE})
    public ResponseEntity<?> proxyRequest(HttpServletRequest request) throws ProxyRequestHandlerException, ProxyRestApiResponseException, SessionNotFoundException {
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
            throw new ProxyRequestHandlerException();
        }

        Optional<Node> workNodeOptional = nodeService.getWorkedNode(uri);
        Node workNode;

        if(workNodeOptional.isPresent()) {
            workNode = workNodeOptional.get();
        } else {
            throw new SessionNotFoundException();
        }

        url = workNode.getAddress();

        String response;
        String method = request.getMethod();

        try {
            if (body.isEmpty()) {
                response = restApiService.executeRequest(method, request, url, uri);
            } else {
                response = restApiService.executeRequest(method, request, body, url, uri);
            }

            if ("DELETE".equalsIgnoreCase(method)) {
                nodeService.releaseNode(workNode);
            } else {
                workNode.setTimeout(CommonUtils.RESOURCE_TIMEOUT.get());
            }
        } catch (Exception e) {
            nodeService.releaseNode(workNode);

            throw new ProxyRestApiResponseException(e);
        }

        if(!response.isEmpty()) {
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.OK);
        }
    }
}
