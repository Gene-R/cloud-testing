package com.example.reservationclient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@EnableZuulProxy
@EnableDiscoveryClient
@SpringBootApplication
public class ReservationClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReservationClientApplication.class, args);
	}
}

@RestController
@RequestMapping("/reservations")
class ReservationApiGatewayRestController {

	@Bean // make sure to define this bean before using Autowired for this bean below in
			// the code
	// public RestTemplate restTemplate() {
	// return new RestTemplate();
	// }
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		// Do any additional configuration here
		return builder.build();
	}

	@Autowired
	RestTemplate restTempalte;

	@RequestMapping(method = RequestMethod.GET, value = "/names")
	public Collection<String> names() {
		String serviceID = "reservation-service"; // ATTENTION: this is service ID - not hostname in DNS !!!!
		// for durect access you would need to use http://localhost:8085/reservations
		// for accsing it from registry service use:
		// http://registration-service/reservations
		ParameterizedTypeReference<Resources<Reservation>> ptr = new ParameterizedTypeReference<Resources<Reservation>>() {
		};
		ResponseEntity<Resources<Reservation>> entiry = this.restTempalte
				.exchange("http://" + serviceID + "/reservations", HttpMethod.GET, null, ptr);

		return entiry.getBody() // get teh body from rest request
				.getContent() // get content of the body
				.stream() // get the streat of Resources<Reservation>
				.map(Reservation::getReservationName) // map Reservation object to string - reservation name
				.collect(Collectors.toList()); // return the result back as the collection of the strings
		// Note: you can do other things here such as non-blocking RxJava async calls to
		// other services, then zip() and process
		// ATTENTION: For large list to return it will be better to return Observable
	}
}

class Reservation {
	private String reservationName;

	public String getReservationName() {
		return reservationName;
	}

}
