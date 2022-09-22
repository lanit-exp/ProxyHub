package ru.lanit.at.api;

import kong.unirest.HttpResponse;

import java.util.Map;

public interface RequestService {

    HttpResponse<String> doPost(Map<String, String> headers, String body, String url, String uri);

    HttpResponse<String> doGet(Map<String, String> headers, String url, String uri);

    HttpResponse<String> doDelete(Map<String, String> headers, String url, String uri);
}
