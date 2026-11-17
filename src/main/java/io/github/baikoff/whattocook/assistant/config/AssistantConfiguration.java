package io.github.baikoff.whattocook.assistant.config;

import java.util.Random;
import java.util.random.RandomGenerator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AssistantConfiguration {

	@Bean
	RandomGenerator recommendationRandomGenerator() {
		return new Random();
	}
}
