package ru.lanit.at.rest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ru.lanit.at.node.NodeNotFoundException;
import ru.lanit.at.proxy.exception.proxy.ProxyRequestHandlerException;
import ru.lanit.at.proxy.exception.proxy.ProxyRestApiResponseException;
import ru.lanit.at.proxy.exception.session.CreateSessionException;
import ru.lanit.at.proxy.exception.session.SessionNotFoundException;

@ControllerAdvice
public class RestApiResponseExceptionHandler extends ResponseEntityExceptionHandler {
    public RestApiResponseExceptionHandler() {
        super();
    }

    @ExceptionHandler({NodeNotFoundException.class})
    public ResponseEntity<?> handleNodeNotFoundException(NodeNotFoundException e, final WebRequest request) {
        e.printStackTrace();
        String message = e.getMessage() != null ? e.getMessage() : "Node is not found";
        return handleExceptionInternal(e, message, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler({ProxyRestApiResponseException.class})
    public ResponseEntity<?> handleProxyRestApiResponseException(ProxyRestApiResponseException e, final WebRequest request) {
        e.printStackTrace();
        String message = e.getMessage() != null ? e.getMessage() : "Exception while proxy request";
        return handleExceptionInternal(e, message, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler({ProxyRequestHandlerException.class})
    public ResponseEntity<?> handleProxyRequestHandlerException(ProxyRequestHandlerException e, final WebRequest request) {
        e.printStackTrace();
        String message = e.getMessage() != null ? e.getMessage() : "Exception while while preparing to proxy request";
        return handleExceptionInternal(e, message, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler({CreateSessionException.class})
    public ResponseEntity<?> handleCreateSessionException(CreateSessionException e, final WebRequest request) {
        e.printStackTrace();
        String message = e.getMessage() != null ? e.getMessage() : "Exception while while creating session";
        return handleExceptionInternal(e, message, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler({SessionNotFoundException.class})
    public ResponseEntity<?> handleSessionNotFoundException(SessionNotFoundException e, final WebRequest request) {
        e.printStackTrace();
        String message = e.getMessage() != null ? e.getMessage() : "Session is not found";
        return handleExceptionInternal(e, message, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
}
