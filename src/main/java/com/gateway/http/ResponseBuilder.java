package com.gateway.http;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class ResponseBuilder {
    private final HttpResponseStatus status;
    private final DefaultFullHttpResponse response;

    private ResponseBuilder(HttpResponseStatus status) {
        this.status = status;
        this.response = new DefaultFullHttpResponse(HTTP_1_1, status);
    }

    public static ResponseBuilder status(HttpResponseStatus status) {
        return new ResponseBuilder(status);
    }

    public static ResponseBuilder ok() {
        return new ResponseBuilder(HttpResponseStatus.OK);
    }

    public static ResponseBuilder created() {
        return new ResponseBuilder(HttpResponseStatus.CREATED);
    }

    public static ResponseBuilder badRequest() {
        return new ResponseBuilder(HttpResponseStatus.BAD_REQUEST);
    }

    public static ResponseBuilder unauthorized() {
        return new ResponseBuilder(HttpResponseStatus.UNAUTHORIZED);
    }

    public static ResponseBuilder notFound() {
        return new ResponseBuilder(HttpResponseStatus.NOT_FOUND);
    }

    public static ResponseBuilder internalError() {
        return new ResponseBuilder(HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }

    public static ResponseBuilder serviceUnavailable() {
        return new ResponseBuilder(HttpResponseStatus.SERVICE_UNAVAILABLE);
    }

    public static ResponseBuilder json(HttpResponseStatus status) {
        ResponseBuilder builder = new ResponseBuilder(status);
        builder.response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        return builder;
    }

    public ResponseBuilder contentType(String type) {
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, type);
        return this;
    }

    public ResponseBuilder jsonContent() {
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        return this;
    }

    public ResponseBuilder body(String content) {
        if (content != null) {
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            response.content().writeBytes(bytes);
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
        } else {
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
        }
        return this;
    }

    public ResponseBuilder header(String name, String value) {
        response.headers().set(name, value);
        return this;
    }

    public ResponseBuilder headers(Map<String, String> headers) {
        headers.forEach((key, value) -> response.headers().set(key, value));
        return this;
    }

    public FullHttpResponse build() {
        // Set default content-type if not set
        if (!response.headers().contains(HttpHeaderNames.CONTENT_TYPE)) {
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        }

        // Ensure content-length is set
        if (!response.headers().contains(HttpHeaderNames.CONTENT_LENGTH)) {
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH,
                    response.content().readableBytes());
        }

        return response;
    }
}