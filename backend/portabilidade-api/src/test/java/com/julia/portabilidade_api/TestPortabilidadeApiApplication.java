package com.julia.portabilidade_api;

import org.springframework.boot.SpringApplication;

public class TestPortabilidadeApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(PortabilidadeApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
