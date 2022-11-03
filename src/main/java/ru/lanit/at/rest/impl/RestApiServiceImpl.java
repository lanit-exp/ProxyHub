package ru.lanit.at.rest.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.lanit.at.rest.RestApiService;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

@Slf4j
@AllArgsConstructor
@Service
public class RestApiServiceImpl implements RestApiService {
    private final RestTemplate restTemplate;

    public String executeRequest(String method, HttpServletRequest request, String body, String url, String uri) {
        HttpHeaders headers = new HttpHeaders();
        setHeaders(request, headers);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        return getResponseBody(url, uri, method, entity, request);
    }

    public String executeRequest(String method, HttpServletRequest request, String url, String uri) {
        HttpHeaders headers = new HttpHeaders();
        setHeaders(request, headers);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        return getResponseBody(url, uri, method, entity, request);
    }

    public String executeRequest(String method, String url, String uri, String requestParam) {
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        return getResponseBody (url, uri, method, entity, requestParam);
    }

    public String getResponseBody(String url, String uri, String method, HttpEntity<String> entity, HttpServletRequest request) {
        String queryString = request.getQueryString();
        return getResponseBody(url, uri, method, entity, queryString);
    }

    public String getResponseBody(String url, String uri, String method, HttpEntity<String> entity, String requestParam) {
        log.info("Request to: {}", url + uri + (requestParam != null ? "?" + requestParam : ""));

        return restTemplate.exchange(String.format("%s%s%s", url, uri, (requestParam != null ? "?" + requestParam : "")),
                HttpMethod.valueOf(method), entity, String.class).getBody();
    }

    private void setHeaders(HttpServletRequest request, HttpHeaders headers) {
        Enumeration<String> headerNames = request.getHeaderNames();

        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                headers.set(headerName, request.getHeader(headerName));
            }
        }

        headers.remove("path");
        headers.remove("content-length");
    }
}
