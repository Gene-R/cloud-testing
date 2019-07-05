package com.example.hadooptest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
public class HadoopTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(HadoopTestApplication.class, args);
	}


	@RestController
	@RequestMapping("/")
	class query{


		@RequestMapping(method = RequestMethod.GET)
		public String help() {
			return "HELP";
		}

		@RequestMapping(method = RequestMethod.GET, path = "/hadoop/ls/{fileName}")
		public String dfsLs(@PathVariable String fileName) {
             return "FILE: " + fileName;
		}

	}

}
