package com.bmgs.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.discovery.DiscoveryClientRouteDefinitionLocator;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.cloud.gateway.filter.factory.RewritePathGatewayFilterFactory;
import org.springframework.cloud.gateway.route.RouteLocator;

import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.springframework.web.reactive.function.server.RequestPredicates.path;

@Configuration
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

	@Bean
	DiscoveryClientRouteDefinitionLocator discoveryRoutes(DiscoveryClient dc, DiscoveryLocatorProperties properties)
	{
		return new DiscoveryClientRouteDefinitionLocator(dc,properties);
	}

	@Bean
	RouteLocator gatewayRoutes(RouteLocatorBuilder rlb)
	{
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
				.build();
	}

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}
}
