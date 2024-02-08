package ru.lanit.at.proxy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import ru.lanit.at.node.Node;
import ru.lanit.at.node.NodeNotFoundException;
import ru.lanit.at.node.NodeService;
import ru.lanit.at.proxy.exception.proxy.ProxyRequestHandlerException;
import ru.lanit.at.proxy.exception.proxy.ProxyRestApiResponseException;
import ru.lanit.at.proxy.exception.session.CreateSessionException;
import ru.lanit.at.proxy.exception.session.SessionNotFoundException;
import ru.lanit.at.rest.RestApiService;
import ru.lanit.at.util.JsonUtils;

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
    public synchronized ResponseEntity<?> startSession(HttpServletRequest request) throws NodeNotFoundException, CreateSessionException {
        if (nodeService.getNodes().isEmpty()) {
            throw new NodeNotFoundException("List of nodes is empty!");
        }

        Node node;

        try {
            try (BufferedReader reader = request.getReader()) {
                String body = reader.lines().collect(Collectors.joining(System.lineSeparator()));

                String nodeName = JsonUtils.getJsonNodeByKey(body, "nodeName").asText();
                String sessionId = JsonUtils.getJsonNodeByKey(body, "sessionId").asText();

                if (!nodeName.isEmpty()) {

                    if (!nodeService.getNodes().containsKey(nodeName)) {
                        throw new NodeNotFoundException(String.format("Node %s not found in list", nodeName));
                    }

                    node = nodeService.getNodeByName(nodeName);

                } else if (!sessionId.isEmpty()) {
                    Optional<Node> nodeOptional = nodeService.getNodeBySessionId(sessionId);

                    if (nodeOptional.isPresent()) {
                        node = nodeOptional.get();
                    } else {
                        throw new NodeNotFoundException("Node is not found by session id");
                    }

                } else {
                    node = nodeService.getFreeNode();
                    nodeService.releaseNode(node);
                }

                String response;
                String method = request.getMethod();

                try {
                    response = restApiService.executeRequest(method, request, body, node.getAddress(), "/session");
                } catch (Exception e) {
                    throw new ProxyRestApiResponseException(e);
                }

                node.setLastActivity(System.currentTimeMillis());
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
            throw new ProxyRequestHandlerException("Exception of body parsing");
        }

        Optional<Node> workNodeOptional = nodeService.getWorkedNode(uri);
        Node workNode;

        if (workNodeOptional.isPresent()) {
            workNode = workNodeOptional.get();
        } else {
            throw new SessionNotFoundException();
        }

        url = workNode.getAddress();

        String response;
        HttpStatus status = HttpStatus.OK;

        String method = request.getMethod();

        try {
            if (body.isEmpty()) {
                response = restApiService.executeRequest(method, request, url, uri);
            } else {
                response = restApiService.executeRequest(method, request, body, url, uri);
            }

            workNode.setLastActivity(System.currentTimeMillis());

        } catch (HttpStatusCodeException e) {

            response = e.getResponseBodyAsString();
            status = e.getStatusCode();

        } catch (Exception e) {
            log.info("url - {}", url + uri);
            log.info("body - {}", body);
            throw new ProxyRestApiResponseException(e);
        }

        if (!response.isEmpty()) {
            return ResponseEntity
                    .status(status)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
        } else {
            return new ResponseEntity<>(status);
        }
    }
}
