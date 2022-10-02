package ru.lanit.at.rest.impl;

import kong.unirest.GetRequest;
import kong.unirest.HttpRequestWithBody;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.lanit.at.rest.RequestService;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
@Service
public class RequestServiceImpl implements RequestService {
    private final RestTemplate restTemplate;

    public HttpResponse<String> doPost(Map<String, String> headers, String body, String url, String uri) {
        HttpRequestWithBody request = Unirest.request("post",url + uri);

        headers.remove("path");
        headers.remove("content-length");


        request.headers(headers);

        return request.body(body).asString();
    }

    public HttpResponse<String> doGet(Map<String, String> headers, String url, String uri) {
        GetRequest request = Unirest.get(url + uri);
        headers.remove("path");
        headers.remove("content-length");

        for(Map.Entry<String, String> x : headers.entrySet()) {
            request.header(x.getKey(), x.getValue());
        }

        return request.asString();
    }

    public HttpResponse<String> doDelete(Map<String, String> headers, String url, String uri) {
        HttpRequestWithBody request = Unirest.delete(url + uri);
        headers.remove("path");
        headers.remove("content-length");

        for(Map.Entry<String, String> x : headers.entrySet()) {
            request.header(x.getKey(), x.getValue());
        }

        return request.asString();
    }

    public String executeRequest(String method, HttpServletRequest request, String body, String url, String uri) {
        HttpHeaders headers = new HttpHeaders();
        setHeaders(request, headers);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        return restTemplate.exchange(url + uri, HttpMethod.valueOf(method), entity, String.class).getBody();
    }

    public String executeRequest(String method, HttpServletRequest request, String url, String uri) {
        HttpHeaders headers = new HttpHeaders();
        setHeaders(request, headers);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        return restTemplate.exchange(url + uri, HttpMethod.valueOf(method), entity, String.class).getBody();
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
