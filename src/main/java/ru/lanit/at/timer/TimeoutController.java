package ru.lanit.at.timer;

import io.swagger.annotations.ApiOperation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ru.lanit.at.utils.CommonUtils;

@RestController
@RequestMapping(value = "/timeout")
public class TimeoutController {
    @RequestMapping(value = "/set/{value}", method = RequestMethod.GET)
    @ApiOperation(value = "Изменение значения параметра таймаута.")
    public ResponseEntity<String> setTimeout(@PathVariable int value) {
        if(value >= 0) {
            CommonUtils.RESOURCE_TIMEOUT.set(value);
            return new ResponseEntity<>(String.format("Set timeout value %s", value), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(String.format("Abort operation. The value %s is less than 0.", value), HttpStatus.OK);
        }
    }
}
