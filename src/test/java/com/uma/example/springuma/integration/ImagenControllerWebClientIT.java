package com.uma.example.springuma.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uma.example.springuma.integration.base.AbstractIntegration;
import com.uma.example.springuma.model.Medico;
import com.uma.example.springuma.model.Paciente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.io.IOException;

import static org.springframework.http.MediaType.APPLICATION_JSON;

public class ImagenControllerWebClientIT extends AbstractIntegration {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ObjectMapper objectMapper;

    @LocalServerPort
    private int port;

    private Medico medico;
    private Paciente paciente;

    @BeforeEach
    void setUp() throws Exception {
        // Initialize WebTestClient with base URL
        webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();

        medico = new Medico();
        medico.setDni("12345678A");
        medico.setNombre("Doctor Test");
        medico.setEspecialidad("Cardiologia");

        webTestClient.post().uri("/medico")
                .contentType(APPLICATION_JSON)
                .body(BodyInserters.fromValue(medico))
                .exchange()
                .expectStatus().isCreated();

        Medico createdMedico = webTestClient.get().uri("/medico/dni/" + medico.getDni())
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Medico.class)
                .returnResult().getResponseBody();
        if (createdMedico != null) {
            medico.setId(1);
        }


        paciente = new Paciente();
        paciente.setNombre("Juan");
        paciente.setEdad(30);
        paciente.setCita("2025-06-01");
        paciente.setDni("87654321B");
        paciente.setMedico(medico);

        webTestClient.post().uri("/paciente")
                .contentType(APPLICATION_JSON)
                .body(BodyInserters.fromValue(paciente))
                .exchange()
                .expectStatus().isCreated();
        
        Paciente[] createdPacientes = webTestClient.get().uri("/paciente/medico/" + medico.getId())
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Paciente[].class)
                .returnResult().getResponseBody();

        if (createdPacientes != null && createdPacientes.length > 0) {
            // Assuming the last patient in the list is the one just created
            paciente = createdPacientes[createdPacientes.length - 1];
        } else {
            throw new Exception("Failed to retrieve created patient");
        }
    }

    @Test
    @DisplayName("Subir imagen de paciente correctamente")
    void uploadImage_pacienteCorrecto() throws IOException {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("image", new ClassPathResource("healthy.png")).contentType(MediaType.IMAGE_PNG);
        builder.part("paciente", paciente, MediaType.APPLICATION_JSON);

        webTestClient.post().uri("/imagen")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.response").isEqualTo("file uploaded successfully : healthy.png");

        // Verify the image is associated with the patient
        webTestClient.get().uri("/imagen/paciente/" + paciente.getId())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$[0].nombre").isEqualTo("healthy.png")
                .jsonPath("$[0].paciente.id").isEqualTo(paciente.getId());
    }

    @Test
    @DisplayName("Subir dos imagenes para el mismo paciente")
    void uploadTwoImages_samePaciente() throws IOException {
        MultipartBodyBuilder builder1 = new MultipartBodyBuilder();
        builder1.part("image", new ClassPathResource("healthy.png")).contentType(MediaType.IMAGE_PNG);
        builder1.part("paciente", paciente, MediaType.APPLICATION_JSON);

        webTestClient.post().uri("/imagen")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder1.build()))
                .exchange()
                .expectStatus().isOk();

        MultipartBodyBuilder builder2 = new MultipartBodyBuilder();
        builder2.part("image", new ClassPathResource("no_healthty.png")).contentType(MediaType.IMAGE_PNG);
        builder2.part("paciente", paciente, MediaType.APPLICATION_JSON);

        webTestClient.post().uri("/imagen")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder2.build()))
                .exchange()
                .expectStatus().isOk();

        // Verify both images are associated with the patient
        webTestClient.get().uri("/imagen/paciente/" + paciente.getId())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[?(@.nombre == 'healthy.png')]").exists()
                .jsonPath("$[?(@.nombre == 'no_healthty.png')]").exists();
    }

    @Test
    @DisplayName("Intentar subir sin archivo")
    void uploadImage_sinArchivo() throws IOException {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        // No image part
        builder.part("paciente", paciente, MediaType.APPLICATION_JSON);

        webTestClient.post().uri("/imagen")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Intentar subir sin datos de paciente")
    void uploadImage_sinDatosPaciente() throws IOException {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("image", new ClassPathResource("healthy.png")).contentType(MediaType.IMAGE_PNG);
        // No paciente part

        webTestClient.post().uri("/imagen")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .exchange()
                .expectStatus().isBadRequest();
    }

}
