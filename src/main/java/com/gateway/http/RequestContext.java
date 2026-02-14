package com.gateway.http;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestContext {
    private final String method;
    private final String path;
    private final Map<String, String> headers;
    private final Map<String, String> queryParams;
    private final String body;
    private final Map<String, String> pathParams;

    private RequestContext(String method, String path, Map<String, String> headers,
                           Map<String, String> queryParams, String body) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.queryParams = queryParams;
        this.body = body;
        this.pathParams = new HashMap<>();
    }

    public static RequestContext from(FullHttpRequest request) {
        String method = request.method().name();
        String uri = request.uri();

        // Parse path and query using Netty's decoder
        QueryStringDecoder decoder = new QueryStringDecoder(uri);
        String path = decoder.path();

        // Parse query parameters
        Map<String, String> queryParams = new HashMap<>();
        decoder.parameters().forEach((key, values) -> {
            if (!values.isEmpty()) {
                queryParams.put(key, values.get(0));
            }
        });

        // Parse headers
        Map<String, String> headers = new HashMap<>();
        request.headers().forEach(entry ->
                headers.put(entry.getKey().toLowerCase(), entry.getValue())
        );

        // Read body
        String body = "";
        ByteBuf content = request.content();
        if (content.isReadable()) {
            body = content.toString(StandardCharsets.UTF_8);
        }

        return new RequestContext(method, path, headers, queryParams, body);
    }

    public void addPathParam(String key, String value) {
        pathParams.put(key, value);
    }

    public String extractPathVariable(String pattern, String variableName) {
        String[] patternParts = pattern.split("/");
        String[] pathParts = path.split("/");

        if (patternParts.length != pathParts.length) {
            return null;
        }

        for (int i = 0; i < patternParts.length; i++) {
            if (patternParts[i].equals("{" + variableName + "}")) {
                return pathParts[i];
            }
        }

        return null;
    }

    // Getters
    public String getMethod() { return method; }
    public String getPath() { return path; }
    public String getHeader(String name) { return headers.get(name.toLowerCase()); }
    public String getQueryParam(String name) { return queryParams.get(name); }
    public String getPathParam(String name) { return pathParams.get(name); }
    public String getBody() { return body; }
    public Map<String, String> getHeaders() { return headers; }
    public Map<String, String> getQueryParams() { return queryParams; }

    @Override
    public String toString() {
        return method + " " + path;
    }
}