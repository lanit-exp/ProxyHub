package ru.lanit.at.controller;

import io.swagger.annotations.*;
import kong.unirest.json.JSONObject;
import org.mapstruct.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ru.lanit.at.element.Node;
import ru.lanit.at.service.Nodes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api")
public class NodeController {
    private Nodes nodes;

    @Autowired
    public NodeController(Nodes nodes) {
        this.nodes = nodes;
    }

    @RequestMapping(value = "/register/node", method = RequestMethod.POST,
            headers="content-type=application/json;charset=UTF-8",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Регистрация узла через отправку JSON. При желании можно указать имя узла через параметр \"applicationName\".")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Узел успешно зарегистрирован.", response = String.class),
            @ApiResponse(code = 500, message = "Error", response = String.class),
            @ApiResponse(code = 400, message = "Отсутствует хост и/или порт.", response = String.class)
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
            return new ResponseEntity<>("Отсутствует хост и/или порт.", HttpStatus.BAD_REQUEST);
        } else {
            String address = String.format("http://%s:%s", jsonBody.get("host"), jsonBody.get("port"));
            Node node = new Node(address);

            if(jsonBody.has("applicationName")) {
                name = jsonBody.getString("applicationName");
            } else {
                name = UUID.randomUUID().toString().replace("-", "");
            }

            nodes.setNode(name, node);
        }

        return new ResponseEntity<>("Узел успешно зарегистрирован. Имя узла - \"" + name + "\"", HttpStatus.OK);
    }

    @RequestMapping(value = "/get/nodes", method = RequestMethod.GET)
    @ApiOperation(value = "Получение информации о текущих узлах.")
    public ResponseEntity<String> getNodes() {
        return new ResponseEntity<>(nodes.toString(), HttpStatus.OK);
    }
}
