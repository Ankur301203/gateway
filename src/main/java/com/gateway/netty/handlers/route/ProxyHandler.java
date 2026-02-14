package com.gateway.netty.handlers.proxy;

import com.gateway.http.RequestContext;
import com.gateway.http.ResponseBuilder;
import com.gateway.netty.handlers.BaseHandler;
import com.gateway.service.ProxyService;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyHandler extends BaseHandler {
    private static final Logger logger = LoggerFactory.getLogger(ProxyHandler.class);
    private final ProxyService proxyService = new ProxyService();

    @Override
    public FullHttpResponse handle(RequestContext ctx) throws Exception {
        // Extract gateway ID from path: /gateway/{gatewayId}/rest/of/path
        String path = ctx.getPath();
        String[] parts = path.split("/", 4); // ["", "gateway", "{id}", "rest"]

        if (parts.length < 3) {
            logger.warn("Invalid gateway URL format: {}", path);
            return ResponseBuilder.json(HttpResponseStatus.BAD_REQUEST)
                    .body("{\"error\": \"Invalid gateway URL format. Expected: /gateway/{id}/path\"}")
                    .build();
        }

        String gatewayId = parts[2];
        String targetPath = "/" + (parts.length > 3 ? parts[3] : "");

        logger.info("Proxying request: gateway={}, path={}, method={}",
                gatewayId, targetPath, ctx.getMethod());

        try {
            // Validate gateway ID format
            try {
                java.util.UUID.fromString(gatewayId);
            } catch (IllegalArgumentException e) {
                return ResponseBuilder.json(HttpResponseStatus.BAD_REQUEST)
                        .body("{\"error\": \"Invalid gateway ID format\"}")
                        .build();
            }

            // Forward request through ProxyService
            ProxyService.ProxyResult result = proxyService.forward(
                    gatewayId,
                    targetPath,
                    ctx.getMethod(),
                    ctx.getHeaders(),
                    ctx.getBody()
            );

            // Build response with status from backend
            HttpResponseStatus status = HttpResponseStatus.valueOf(result.statusCode);
            ResponseBuilder builder = ResponseBuilder.status(status);

            // Copy response headers from backend
            result.headers.forEach(builder::header);

            // Add custom proxy headers
            builder.header("X-Gateway-Proxy", "GatewayaaS/1.0");

            return builder.body(result.body).build();

        } catch (IllegalArgumentException e) {
            logger.warn("Bad request for gateway {}: {}", gatewayId, e.getMessage());
            return ResponseBuilder.json(HttpResponseStatus.BAD_REQUEST)
                    .body("{\"error\": \"" + e.getMessage().replace("\"", "\\\"") + "\"}")
                    .build();

        } catch (java.net.http.HttpTimeoutException e) {
            logger.error("Timeout forwarding request to gateway {}", gatewayId, e);
            return ResponseBuilder.json(HttpResponseStatus.GATEWAY_TIMEOUT)
                    .body("{\"error\": \"Gateway timeout - backend service did not respond\"}")
                    .build();

        } catch (java.net.ConnectException e) {
            logger.error("Connection failed for gateway {}", gatewayId, e);
            return ResponseBuilder.json(HttpResponseStatus.BAD_GATEWAY)
                    .body("{\"error\": \"Bad gateway - could not connect to backend service\"}")
                    .build();

        } catch (Exception e) {
            logger.error("Proxy error for gateway {}", gatewayId, e);
            return ResponseBuilder.json(HttpResponseStatus.BAD_GATEWAY)
                    .body("{\"error\": \"Bad gateway - " + e.getMessage().replace("\"", "\\\"") + "\"}")
                    .build();
        }
    }
}