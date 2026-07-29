package com.batuhan.heurislink;

import com.batuhan.heurislink.config.RateLimitProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RateLimitProperties.class)
public class HeurislinkApplication {

	public static void main(String[] args) {
		SpringApplication.run(
				HeurislinkApplication.class,
				args
		);
	}
}