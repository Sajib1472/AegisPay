package com.aegispay.app.platform.ops;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;

@Component
public class RedisHealthIndicator implements HealthIndicator {

    private final boolean required;
    private final String host;
    private final int port;

    public RedisHealthIndicator(
            @Value("${aegispay.redis.required:false}") boolean required,
            @Value("${aegispay.redis.url:redis://localhost:6379}") String url
    ) {
        this.required = required;
        URI uri = URI.create(url);
        this.host = uri.getHost() == null ? "localhost" : uri.getHost();
        this.port = uri.getPort() <= 0 ? 6379 : uri.getPort();
    }

    @Override
    public Health health() {
        if (!required) {
            return Health.up().withDetail("redis", "optional").withDetail("host", host).build();
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 500);
            return Health.up().withDetail("host", host).build();
        } catch (Exception e) {
            return Health.down(e).withDetail("host", host).build();
        }
    }
}
