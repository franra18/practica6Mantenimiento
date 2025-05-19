package com.uma.example.springuma.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uma.example.springuma.integration.base.AbstractIntegration;
import com.uma.example.springuma.model.Medico;
import com.uma.example.springuma.model.Paciente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PacienteControllerMockMvcIT extends AbstractIntegration {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Medico medico;

    @BeforeEach
    void setUp() throws Exception {
        medico = crearMedico();
    }

    private Medico crearMedico() throws Exception {
        Medico medico = new Medico();
        medico.setDni("12345678A"); 
        medico.setNombre("Doctor Test");
        medico.setEspecialidad("Cardiologia");

        this.mockMvc.perform(post("/medico")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(medico)))
                .andExpect(status().isCreated());
        
        // Retrieve the created medico to get its generated ID
        String response = this.mockMvc.perform(get("/medico/dni/" + medico.getDni()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, Medico.class);
    }

    private Paciente crearPaciente(Medico medicoAsociado) throws Exception {
        Paciente paciente = new Paciente();
        paciente.setNombre("Juan");
        paciente.setEdad(30);
        paciente.setCita("2025-06-01");
        paciente.setDni("87654321B");
        paciente.setMedico(medicoAsociado);

        this.mockMvc.perform(post("/paciente")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(paciente)))
                .andExpect(status().isCreated());

        String pacientesJson = this.mockMvc.perform(get("/paciente/medico/" + medicoAsociado.getId()))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        Paciente[] pacientes = objectMapper.readValue(pacientesJson, Paciente[].class);
        
        if (pacientes.length > 0) {
            return pacientes[pacientes.length - 1];
        }
        throw new Exception("Failed to retrieve created patient");

    }

    @Test
    @DisplayName("Asociar un paciente a un médico y obtenerlo")
    void asociarPacienteAMedico_yObtenerlo() throws Exception {
        Paciente paciente = crearPaciente(medico);

        this.mockMvc.perform(get("/paciente/" + paciente.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.nombre").value(paciente.getNombre()))
                .andExpect(jsonPath("$.dni").value(paciente.getDni()))
                .andExpect(jsonPath("$.medico.id").value(medico.getId()));
    }

    @Test
    @DisplayName("Editar paciente cambiando su nombre")
    void updateCuentaPut_cambiandoNombrePaciente() throws Exception {
        Paciente paciente = crearPaciente(medico);
        paciente.setNombre("Pedro");

        this.mockMvc.perform(put("/paciente")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(paciente)))
                .andExpect(status().isNoContent());

        this.mockMvc.perform(get("/paciente/" + paciente.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Pedro"));
    }

    @Test
    @DisplayName("Cambiar médico de un paciente")
    void cambiarMedicoDePaciente() throws Exception {
        Paciente paciente = crearPaciente(medico);

        Medico nuevoMedico = new Medico();
        nuevoMedico.setDni("99999999C");
        nuevoMedico.setNombre("Otro Doctor");
        nuevoMedico.setEspecialidad("Pediatria");

        this.mockMvc.perform(post("/medico")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(nuevoMedico)))
                .andExpect(status().isCreated());
        
        String responseNuevoMedico = this.mockMvc.perform(get("/medico/dni/" + nuevoMedico.getDni()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Medico medicoNuevo = objectMapper.readValue(responseNuevoMedico, Medico.class);

        paciente.setMedico(medicoNuevo);

        this.mockMvc.perform(put("/paciente")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(paciente)))
                .andExpect(status().isNoContent());

        this.mockMvc.perform(get("/paciente/" + paciente.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medico.id").value(medicoNuevo.getId()))
                .andExpect(jsonPath("$.medico.nombre").value("Otro Doctor"));
    }
    
    @Test
    @DisplayName("Obtener todos los pacientes de un médico")
    void getPacientesDeMedico() throws Exception {
        crearPaciente(medico);
        
        Paciente paciente2 = new Paciente();
        paciente2.setNombre("Paco");
        paciente2.setEdad(40);
        paciente2.setCita("2025-07-01");
        paciente2.setDni("11223344D");
        paciente2.setMedico(medico);

        this.mockMvc.perform(post("/paciente")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(paciente2)))
                .andExpect(status().isCreated());


        this.mockMvc.perform(get("/paciente/medico/" + medico.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2)) 
                .andExpect(jsonPath("$[0].medico.id").value(medico.getId()))
                .andExpect(jsonPath("$[1].medico.id").value(medico.getId()));
    }

    @Test
    @DisplayName("Eliminar un paciente")
    void deletePaciente() throws Exception {
        Paciente paciente = crearPaciente(medico);

        this.mockMvc.perform(delete("/paciente/" + paciente.getId()))
                .andExpect(status().isOk());

        this.mockMvc.perform(get("/paciente/" + paciente.getId()))
                .andExpect(status().is5xxServerError());
    }
}
