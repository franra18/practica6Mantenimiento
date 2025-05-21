// Grupo formado por Francisco Ramírez Cañadas y Jorge Repullo Serrano.

package com.uma.example.springuma.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uma.example.springuma.integration.base.AbstractIntegration;
import com.uma.example.springuma.model.Imagen;
import com.uma.example.springuma.model.Medico;
import com.uma.example.springuma.model.Paciente;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ImagenControllerWebClientIT {

        @Autowired
        private WebTestClient webTestClient;

        @LocalServerPort
        private Integer port;

        private Medico medico;
        private Paciente paciente;
        private Imagen imagen;

        @PostConstruct
        public void init() {
                webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port)
                                .responseTimeout(Duration.ofMillis(30000)).build();

                medico = new Medico();
                medico.setDni("12345678A");
                medico.setNombre("Doctor Test");
                medico.setEspecialidad("Cardiologia");
                medico.setId(1);

                paciente = new Paciente();
                paciente.setNombre("Juan");
                paciente.setEdad(30);
                paciente.setCita("2025-06-01");
                paciente.setDni("87654321B");
                paciente.setMedico(medico);
                paciente.setId(1);

                imagen = new Imagen();
                imagen.setId(1);
                imagen.setPaciente(paciente);
        }

        @BeforeEach
        public void setUp() throws Exception {
                // Crea el médico
                this.webTestClient.post().uri("/medico")
                                .body(Mono.just(medico), Medico.class)
                                .exchange()
                                .expectStatus().isCreated()
                                .expectBody().returnResult();

                // Crea el paciente
                this.webTestClient.post().uri("/paciente")
                                .body(Mono.just(paciente), Paciente.class)
                                .exchange()
                                .expectStatus().isCreated()
                                .expectBody().returnResult();
        }

        @Test
        @DisplayName("Subir imagen de paciente correctamente debería devolver una respuesta válida que contenga el nombre del archivo")
        void subirImagen_pacienteCorrecto_devuleveRespuestaValida() throws IOException {
                // Sube la imagen
                File uploadFile = new File("./src/test/resources/healthy.png");
                
                MultipartBodyBuilder builder = new MultipartBodyBuilder();
                builder.part("image", new FileSystemResource(uploadFile));
                builder.part("paciente", paciente, MediaType.APPLICATION_JSON);
                
                
                FluxExchangeResult<String> responseBody = webTestClient.post()
                        .uri("/imagen")
                        .contentType(MediaType.MULTIPART_FORM_DATA) // Esta parte es obligatoria
                        .body(BodyInserters.fromMultipartData(builder.build()))
                        .exchange()
                        .expectStatus().is2xxSuccessful().returnResult(String.class);

                String result = responseBody.getResponseBody().blockFirst();

                assertEquals("{\"response\" : \"file uploaded successfully : healthy.png\"}", result);

                // Verifica que la imagen esté asociada al paciente
                webTestClient.get().uri("/imagen/paciente/" + paciente.getId())
                                .exchange()
                                .expectStatus().isOk()
                                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                                .expectBody()
                                .jsonPath("$[0].nombre").isEqualTo("healthy.png")
                                .jsonPath("$[0].paciente.id").isEqualTo(paciente.getId());
        }

        @Test
        @DisplayName("Subir dos imagenes para el mismo paciente debería devolver una respuesta válida que contenga el nombre de ambos archivos")
        void subirDosImagenes_mismoPaciente_devuelveRespuestaValida() throws IOException {
                // Crea un paciente
                Paciente paciente2 = new Paciente();
                paciente2.setNombre("Jorge");
                paciente2.setEdad(90);
                paciente2.setCita("2025-06-01");
                paciente2.setDni("45968724B");
                paciente2.setMedico(medico);
                paciente2.setId(2);

                this.webTestClient.post().uri("/paciente")
                                .body(Mono.just(paciente2), Paciente.class)
                                .exchange()
                                .expectStatus().isCreated()
                                .expectBody().returnResult();


                File uploadFile1 = new File("./src/test/resources/healthy.png");
                File uploadFile2 = new File("./src/test/resources/no_healthty.png");

                MultipartBodyBuilder builder1 = new MultipartBodyBuilder();
                builder1.part("image", new FileSystemResource(uploadFile1));
                builder1.part("paciente", paciente2, MediaType.APPLICATION_JSON);

                // Sube la primera imagen
                webTestClient.post()
                                .uri("/imagen")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .body(BodyInserters.fromMultipartData(builder1.build()))
                                .exchange()
                                .expectStatus().isOk();

                MultipartBodyBuilder builder2 = new MultipartBodyBuilder();
                builder2.part("image", new FileSystemResource(uploadFile2));
                builder2.part("paciente", paciente2, MediaType.APPLICATION_JSON);

                // Sube la segunda imagen
                webTestClient.post()
                                .uri("/imagen")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .body(BodyInserters.fromMultipartData(builder2.build()))
                                .exchange()
                                .expectStatus().isOk();

                // Verifica que ambas imágenes estén asociadas al paciente
                webTestClient.get().uri("/imagen/paciente/" + paciente2.getId())
                                .exchange()
                                .expectStatus().isOk()
                                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                                .expectBody()
                                .jsonPath("$.length()").isEqualTo(2)
                                .jsonPath("$[?(@.nombre == 'healthy.png')]").exists()
                                .jsonPath("$[?(@.nombre == 'no_healthty.png')]").exists();
        }

        @Test
        @DisplayName("Intentar subir una imagen sin archivo debería devolver un error 400")
        void subirImagen_sinArchivo_devuelveError() throws IOException {
                MultipartBodyBuilder builder = new MultipartBodyBuilder();
                // Sin parte de imagen
                builder.part("paciente", paciente, MediaType.APPLICATION_JSON);

                webTestClient.post()
                                .uri("/imagen")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .body(BodyInserters.fromMultipartData(builder.build()))
                                .exchange()
                                .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("Intentar subir una imagen sin datos de paciente debería devolver un error 400")
        void subirImagen_sinDatosPaciente_devuelveError() throws IOException {
                File uploadFile = new File("./src/test/resources/healthy.png");

                MultipartBodyBuilder builder = new MultipartBodyBuilder();
                builder.part("image", new FileSystemResource(uploadFile));
                // Sin parte de paciente

                webTestClient.post()
                                .uri("/imagen")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .body(BodyInserters.fromMultipartData(builder.build()))
                                .exchange()
                                .expectStatus().isBadRequest();
        }

        // Realizar una predicción de una imagen de un paciente. Se debe de devolver un resultado aleatorio, indicando si tiene cancer o no.
        @Test
        @DisplayName("Realizar una predicción de una imagen de un paciente debería devolver un resultado aleatorio")
        void getImagenPrediction() throws IOException {
                File uploadFile = new File("./src/test/resources/healthy.png");

                MultipartBodyBuilder builder = new MultipartBodyBuilder();
                builder.part("image", new FileSystemResource(uploadFile));
                builder.part("paciente", paciente, MediaType.APPLICATION_JSON);

                // Sube la imagen
                webTestClient.post()
                                .uri("/imagen")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .body(BodyInserters.fromMultipartData(builder.build()))
                                .exchange()
                                .expectStatus().isOk();

                // Realiza la predicción
                FluxExchangeResult<String> responseBody = webTestClient.get()
                                .uri("/imagen/predict/" + paciente.getId())
                                .accept(APPLICATION_JSON)
                                .exchange()
                                .expectStatus().is2xxSuccessful()
                                .returnResult(String.class);

                String result = responseBody.getResponseBody().blockFirst();

                assertEquals(true, result.contains("status"));
        }

        @Test
        @DisplayName("Eliminar una imagen debería devolver un estado 204 sin contenido")
        void deleteImagen() throws IOException {
                File uploadFile = new File("./src/test/resources/healthy.png");

                MultipartBodyBuilder builder = new MultipartBodyBuilder();
                builder.part("image", new FileSystemResource(uploadFile));
                builder.part("paciente", paciente, MediaType.APPLICATION_JSON);

                // Sube la imagen
                webTestClient.post()
                                .uri("/imagen")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .body(BodyInserters.fromMultipartData(builder.build()))
                                .exchange()
                                .expectStatus().isOk();

                // Elimina la imagen
                webTestClient.delete()
                                .uri("/imagen/" + paciente.getId())
                                .exchange()
                                .expectStatus().isNoContent();
        }
}