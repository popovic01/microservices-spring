package utility.microservices.apigateway;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiGatewayConfiguration {
    
    @Bean
	public RouteLocator gatewayRouter(RouteLocatorBuilder builder) {

		return builder
				.routes()
				.route(p -> p.path("/@Depaiva1997").uri("http://youtube.com"))
				.route(p -> p.path("/currency-exchange/**").uri("lb://currency-exchange"))
				.route(p -> p.path("/bank-account/**").uri("lb://bank-account"))
				.route(p -> p.path("/crypto-wallet/**").uri("lb://crypto-wallet"))
				.route(p -> p.path("/crypto-exchange/**").uri("lb://crypto-exchange"))
				.route(p -> p.path("/crypto-conversion/**").uri("lb://crypto-conversion"))
				.route(p -> p.path("/transfer-service/**").uri("lb://transfer-service"))
				.route(p -> p.path("/trade-service/**").uri("lb://trade-service"))
                .route(p -> p.path("/currency-conversion-feign").uri("lb://currency-conversion"))
				.route(p -> p.path("/currency-conversion")
						.filters(f -> f.rewritePath("/currency-conversion", "/currency-conversion-feign")) //rerouting
						.uri("lb://currency-conversion"))
                .route(p -> p.path("/users-service/**").uri("lb://users-service"))
				.build();
	}

	//http://localhost:8765/currency-exchange/from/USD/to/RSD
}
