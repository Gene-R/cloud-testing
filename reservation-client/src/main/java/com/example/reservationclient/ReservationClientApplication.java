package com.example.reservationclient;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import brave.sampler.Sampler;

@EnableBinding(Source.class) // for RabbitMQ messaging
@EnableCircuitBreaker
@EnableZuulProxy
@EnableDiscoveryClient
@SpringBootApplication

public class ReservationClientApplication {

	@Bean
	Sampler sampler() {
		return new Sampler() {
			@Override
			public boolean isSampled(long traceId) {
				return true;
			}
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(ReservationClientApplication.class, args);
	}
}

@RestController
@RequestMapping("/reservations")
class ReservationApiGatewayRestController {
	@LoadBalanced // this Balanced annotation was important, without this it was giving "I/O error
					// on GET request..."(java.net.UnknownHostException)
	@Bean // without this annotation, it won't even compile the project with Autowired
			// defined below
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		// Do any additional configuration here
		return builder.build();
	}

	@Autowired
	RestTemplate restTempalte;

	@Autowired
	private Source source;

	// this is our fallback function that will do something useful when our
	// reservation-serice is down.
	public Collection<String> getReservationNamesFallback() {
		System.out.println("AAA: fallback");
		return Arrays.asList("The service is curently unreachable. Please try again later.");
	}

	@HystrixCommand(fallbackMethod = "getReservationNamesFallback")
	@RequestMapping(method = RequestMethod.GET, value = "/names")
	public Collection<String> names() {
		// ATTENTION: "reservation-service" in exchange URL is a service ID - not a
		// hostname in DNS !!!!
		// for direct access you would normally use http://localhost:8085/reservations
		// for accsing it from registry service use:
		// http://registration-service/reservations
		ParameterizedTypeReference<Resources<Reservation>> ptr = new ParameterizedTypeReference<Resources<Reservation>>() {
		};
		ResponseEntity<Resources<Reservation>> entiry = this.restTempalte
				.exchange("http://reservation-service/reservations", HttpMethod.GET, null, ptr);

		return entiry.getBody() // get teh body from rest request
				.getContent() // get content of the body
				.stream() // get the streat of Resources<Reservation>
				.map(Reservation::getReservationName) // map Reservation object to string - reservation name
				.collect(Collectors.toList()); // return the result back as the collection of the strings
		// Note: you can do other things here such as non-blocking RxJava async calls to
		// other services, then zip() and process
		// ATTENTION: For large list to return it will be better to return Observable
	}

	@RequestMapping(method = RequestMethod.POST)
	// public void writeReservation(@RequestParam(required = false) Reservation
	// reservation){
	public void writeReservation(@RequestBody(required = false) Reservation reservation) {
		System.out.println("BBB: writeReservation()");
		Message<String> msg = MessageBuilder.withPayload(reservation.getReservationName()).build();
		this.source.output().send(msg);
	}

}

class Reservation {
	private String reservationName;

	public String getReservationName() {
		return reservationName;
	}

}
