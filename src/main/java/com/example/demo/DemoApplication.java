package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class DemoApplication implements CommandLineRunner {

	@Value("${server.port:8833}")
	private String serverPort;

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Override
	public void run(String... strings) throws Exception {
		log.info("Application is success, Index >> http://127.0.0.1:{}", serverPort);
		log.info("Application is success, User List >> http://127.0.0.1:{}/api/user/list", serverPort);
	}
}
