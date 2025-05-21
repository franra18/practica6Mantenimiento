// Grupo formado por Francisco Ramírez Cañadas y Jorge Repullo Serrano.

package com.uma.example.springuma.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import com.uma.example.springuma.model.Imagen;
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
        private Imagen imagen;

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

                imagen = new Imagen();
                imagen.setId(1);
                imagen.setPaciente(paciente);

                informe = new Informe();
                informe.setId(1);
                informe.setContenido("Informe de prueba");
                informe.setImagen(imagen);
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

                // Asocia la imagen al paciente y la sube 
                File uploadFile = new File("./src/test/resources/healthy.png");
                MultipartBodyBuilder builder = new MultipartBodyBuilder();
                builder.part("image", new FileSystemResource(uploadFile));
                builder.part("paciente", paciente, MediaType.APPLICATION_JSON);

                FluxExchangeResult<String> responseBody = webTestClient.post()
                                .uri("/imagen")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .body(BodyInserters.fromMultipartData(builder.build()))
                                .exchange()
                                .expectStatus().is2xxSuccessful()
                                .returnResult(String.class);
        }

        @Test
        @DisplayName("Crea un informe de un paciente correctamente")
        public void crearInforme() {
                // Crea el informe
                this.webTestClient.post()
                                .uri("/informe")
                                .body(Mono.just(this.informe), Informe.class)
                                .exchange()
                                .expectStatus().isCreated()
                                .expectBody().returnResult();

                // Comprueba que el informe se ha creado correctamente 
                FluxExchangeResult<Informe> responseBody = this.webTestClient.get()
                                .uri("/informe/1")
                                .exchange()
                                .expectStatus().is2xxSuccessful()
                                .returnResult(Informe.class);

                Informe result = responseBody.getResponseBody().blockFirst();
                Boolean condicion = result.toString().contains("Informe de prueba");
                assertEquals(imagen, result.getImagen());
                assertTrue(condicion);
        }

        @Test
        @DisplayName("Elimina un informe de un paciente correctamente")
        public void eliminarInforme() {
                // Crea el informe
                this.webTestClient.post()
                                .uri("/informe")
                                .body(Mono.just(this.informe), Informe.class)
                                .exchange()
                                .expectStatus().isCreated()
                                .expectBody().returnResult();

                // Comprueba que el informe se ha creado correctamente 
                FluxExchangeResult<Informe> responseBody = this.webTestClient.get()
                                .uri("/informe/1")
                                .exchange()
                                .expectStatus().is2xxSuccessful()
                                .returnResult(Informe.class);

                // Borra el informe
                this.webTestClient.delete()
                                .uri("/informe/1")
                                .exchange()
                                .expectStatus().isNoContent()
                                .expectBody().returnResult();

                //Comprueba que el informe se ha borrado correctamente
                FluxExchangeResult<Informe> responseBody2 = this.webTestClient.get()
                                .uri("/informe/1")
                                .exchange()
                                .expectStatus().is2xxSuccessful()
                                .returnResult(Informe.class);

                Informe result = responseBody2.getResponseBody().blockFirst();
                assertTrue(result == null);
        }

}
