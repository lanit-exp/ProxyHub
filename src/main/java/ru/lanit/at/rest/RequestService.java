package ru.lanit.at.rest;

import javax.servlet.http.HttpServletRequest;

public interface RequestService {

    String executeRequest(String method, HttpServletRequest request, String body, String url, String uri);

    String executeRequest(String method, HttpServletRequest request, String url, String uri);
}
