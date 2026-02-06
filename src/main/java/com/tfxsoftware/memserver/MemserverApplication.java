package com.tfxsoftware.memserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
@EnableScheduling
public class MemserverApplication {

	public static void main(String[] args) {
		// Load `.env` and expose values as system properties so Spring Boot can resolve
		Dotenv dotenv = Dotenv.configure().filename(".env").ignoreIfMalformed().ignoreIfMissing().load();
		dotenv.entries().forEach(entry -> {
			String key = entry.getKey();
			String value = entry.getValue();
			if (System.getProperty(key) == null && System.getenv(key) == null) {
				System.setProperty(key, value);
			}
		});

		SpringApplication.run(MemserverApplication.class, args);
	}

}
