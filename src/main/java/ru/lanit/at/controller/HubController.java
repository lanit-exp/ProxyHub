package ru.lanit.at.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ru.lanit.at.util.CommonUtils;

@RestController
@RequestMapping(value = "/hub")
public class HubController {
    @RequestMapping(value = "/timeout/set/{value}", method = RequestMethod.GET)
    public ResponseEntity<String> setTimeout(@PathVariable int value) {
        if(value >= 0) {
            CommonUtils.RESOURCE_TIMEOUT.set(value);
            return new ResponseEntity<>(String.format("Set timeout value %s", value), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(String.format("Abort operation. The value %s is less than 0.", value), HttpStatus.OK);
        }
    }
}
