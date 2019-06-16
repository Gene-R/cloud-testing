package com.example.reservationservice;

import java.util.Collection;
import java.util.stream.Stream;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import brave.sampler.Sampler;

@IntegrationComponentScan
@EnableBinding(Sink.class)
@EnableDiscoveryClient
@SpringBootApplication
public class ReservationServiceApplication {
	// Sender sender = OkHttpSender.create("http://localhost:9411/api/v1/spans");
    // Reporter reporter = AsyncReporter.builder(sender).build();

    // // Now, create a tracer with the service name you want to see in Zipkin.
    // Tracer tracer = Tracer.newBuilder()
    //         .localServiceName("my-service")
    //         .reporter(reporter)
    //         .build();
    // Span twoPhase = tracer.newTrace().name("twoPhase").start();
    // try {
    //     Span prepare = tracer.newChild(twoPhase.context()).name("prepare").start();
    //     try {
    //         System.out.print("prepare");
    //     } finally {
    //         prepare.finish();
    //     }
    //     Span commit = tracer.newChild(twoPhase.context()).name("commit").start();
    //     try {
    //         System.out.print("commit");
    //     } finally {
    //         commit.finish();
    //     }
    // } finally {
    //     twoPhase.finish();
    // }




	@Bean
	Sampler sampler() {
		return new Sampler() {
			@Override
			public boolean isSampled(long traceId) {
				return true;
			}
		};
	}

	@Bean
	CommandLineRunner commandLineRunner(ReservationRepository reservationRepository) {
		return names -> {
			Stream.of("Mike", "Bob", "Joshua", "Max")
					.forEach(name -> reservationRepository.save(new Reservation(name)));
		};
	}

	public static void main(String[] args) {

		SpringApplication.run(ReservationServiceApplication.class, args);
	}

}

@RepositoryRestResource
interface ReservationRepository extends JpaRepository<Reservation, Long> {
	@RestResource(path = "by-name")
	Collection<Reservation> findByReservationName(String rn);
}

@MessageEndpoint
class ReservationProcessor {
	@Autowired
	private ReservationRepository reservationRepository;

	@ServiceActivator(inputChannel = Sink.INPUT)
	public void acceptNewReservation(Message<String> msg) {
		this.reservationRepository.save(new Reservation(msg.getPayload()));
	}
}

@Entity
class Reservation {
	@Id
	@GeneratedValue
	private long id;
	private String reservationName;

	public Reservation(String reservationName) {
		this.reservationName = reservationName;
	}

	public Reservation() { // default constructor is needed for JPA
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getReservationName() {
		return reservationName;
	}

	public void setReservationName(String reservationName) {
		this.reservationName = reservationName;
	}

	@Override
	public String toString() {
		return "Reservation [id=" + id + ", reservationName=" + reservationName + "]";
	}
}

@RefreshScope
@RestController
class MessageRestController {
	@Value("${message}") // the value will be taken from config server:
	private String msg;

	@RequestMapping("/message")
	String message() {
		return this.msg;
	}

}