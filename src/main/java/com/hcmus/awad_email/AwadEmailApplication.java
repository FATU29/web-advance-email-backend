package com.hcmus.awad_email;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AwadEmailApplication {

	public static void main(String[] args) {
		SpringApplication.run(AwadEmailApplication.class, args);
	}

}
