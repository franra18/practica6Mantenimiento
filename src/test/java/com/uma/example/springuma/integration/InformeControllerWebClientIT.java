package com.uma.example.springuma.integration;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.uma.example.springuma.model.Informe;
import com.uma.example.springuma.model.Medico;
import com.uma.example.springuma.model.Paciente;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

/*
 * Test de integración para el controlador InformeController utilizando WebClient.
 * El informe tiene un atributo String prediccion y un atributo String contenido, además del id.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class InformeControllerWebClientIT {
    
    @LocalServerPort
    private int port;

    @Autowired
    private WebTestClient webTestClient;

    private Paciente paciente;
    private Medico medico;
    private Informe informe;

    @PostConstruct
    public void init() {
        this.webTestClient = webTestClient.mutate()
                .baseUrl("http://localhost:" + port)
                .build();

        paciente = new Paciente();
        paciente.setId(1);
        paciente.setNombre("Juan");
        paciente.setEdad(30);
        paciente.setCita("2025-06-01");
        paciente.setDni("87654321B");

        medico = new Medico();
        medico.setId(1);
        medico.setDni("12345678A");
        medico.setNombre("Doctor Test");
        medico.setEspecialidad("Cardiologia");

        informe = new Informe();
        informe.setId(1);
        informe.setContenido("Informe de prueba");
        informe.setPrediccion("Predicción de prueba");
    }

    @BeforeEach
    public void setUp() {
        // Crea el médico
        this.webTestClient.post()
                .uri("/medico")
                .body(Mono.just(medico), Medico.class)
                .exchange()
                .expectStatus().isCreated()
                .expectBody().returnResult();

        // Crea el paciente
        this.webTestClient.post()
                .uri("/paciente")
                .body(Mono.just(paciente), Paciente.class)
                .exchange()
                .expectStatus().isCreated()
                .expectBody().returnResult();
            
        // Crea el informe
        this.webTestClient.post()
                .uri("/informe")
                .body(Mono.just(informe), Informe.class)
                .exchange()
                .expectStatus().isCreated()
                .expectBody().returnResult();        
    }
    
}
