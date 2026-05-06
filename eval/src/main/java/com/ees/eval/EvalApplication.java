package com.ees.eval;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class EvalApplication {

	public static void main(String[] args) {
		SpringApplication.run(EvalApplication.class, args);
	}

}
