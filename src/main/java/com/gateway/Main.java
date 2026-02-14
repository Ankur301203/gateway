package com.gateway;

import com.gateway.server.GatewayServer;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Gateway is starting...");
        new GatewayServer(8080).start();
    }
}
