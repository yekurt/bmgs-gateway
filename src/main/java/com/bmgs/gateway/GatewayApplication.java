package com.bmgs.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.discovery.DiscoveryClientRouteDefinitionLocator;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.cloud.gateway.filter.factory.RequestRateLimiterGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RewritePathGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;

import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.config.EnableWebFlux;

import static org.springframework.web.reactive.function.server.RequestPredicates.path;

@Configuration
@SpringBootApplication
@EnableDiscoveryClient
@EnableWebFluxSecurity
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    MapReactiveUserDetailsService authentication() {
        return new MapReactiveUserDetailsService(User.withDefaultPasswordEncoder()
                .username("user").password("pw").roles("USER").build());
    }

    @Bean
    SecurityWebFilterChain authorization(ServerHttpSecurity security)
    {
        return security
                .authorizeExchange().pathMatchers("/rl/**").authenticated()
                .anyExchange().permitAll()
                .and()
                .httpBasic()
                .and()
                .build();
    }

    @Bean
    DiscoveryClientRouteDefinitionLocator discoveryRoutes(DiscoveryClient dc, DiscoveryLocatorProperties properties) {
        return new DiscoveryClientRouteDefinitionLocator(dc, properties);
    }

    @Bean
    RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(1, 2);
    }

    @Bean
    RouteLocator gatewayRoutes(RouteLocatorBuilder rlb, RequestRateLimiterGatewayFilterFactory rl) {
        return rlb.routes()
                .route(
                        r -> r.path("/start")
                                .uri("http://start.spring.io:80/"))
                .route(
                        r -> r
                                .path("/lb/**")
                                .filters(f -> f.rewritePath("/lb/(?<segment>.*)", "/${segment}"))
                                .uri("lb://telemetry-service")
                )
                .route(
                        r -> r
                                .path("/cf1")
                                .filters((f) -> {
                                    f.setResponseHeader("Content-Type", "application/pdf")
                                            .setStatus(HttpStatus.ALREADY_REPORTED);
                                    return f;
                                })
                                .uri("lb://telemetry-service/telemetry")
                )
                .route(
                        r -> r
                                .path("/cb/**")
                                .filters((f) ->
                                        f.rewritePath("/cb/(?<segment>.*)", "/${segment}")
                                                .hystrix(c -> c.setName("cb")))
                                .uri("lb://telemetry-service")
                )
                .route(
                        r -> r
                                .path("/rl/**")
                                .filters((f) ->
                                        f.rewritePath("/rl/(?<segment>.*)", "/${segment}")
                                                .requestRateLimiter()
                                                .configure(c -> c.setRateLimiter(redisRateLimiter())))
                                .uri("lb://telemetry-service")
                )
                .build();
    }
}
