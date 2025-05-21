// Grupo formado por Francisco Ramírez Cañadas y Jorge Repullo Serrano.

package com.uma.example.springuma.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uma.example.springuma.integration.base.AbstractIntegration;
import com.uma.example.springuma.model.Medico;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MedicoControllerMockMvcIT extends AbstractIntegration {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Medico crearMedico() throws Exception {
        Medico medico = new Medico();
        medico.setId(1);
        medico.setDni("12345678A");
        medico.setNombre("Juan");
        medico.setEspecialidad("Cardiología");

        // Crea el médico
        this.mockMvc.perform(post("/medico")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(medico)))
                .andExpect(status().isCreated())
                .andExpect(status().is2xxSuccessful());

        // Verifica que el médico se haya creado correctamente
        this.mockMvc.perform(get("/medico/" + medico.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.dni").value("12345678A"))
                .andExpect(jsonPath("$.nombre").value("Juan"))
                .andExpect(jsonPath("$.especialidad").value("Cardiología"));

        return medico;
    }

    @Test
    @DisplayName("Crea un medico y lo obtiene correctamente")
    void testCreateMedico() throws Exception {
        Medico medico = crearMedico();
    }

    @Test
    @DisplayName("Busca un medico por DNI y lo obtiene correctamente")
    void getMedicoByDni() throws Exception {
        Medico medico = crearMedico();

        // Busca el médico por DNI
        this.mockMvc.perform(get("/medico/dni/" + medico.getDni()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.id").value(medico.getId()))
                .andExpect(jsonPath("$.dni").value(medico.getDni()))
                .andExpect(jsonPath("$.nombre").value(medico.getNombre()))
                .andExpect(jsonPath("$.especialidad").value(medico.getEspecialidad()));
    }

    @Test
    @DisplayName("Busca un medico por ID y lo obtiene correctamente")
    void getMedicoById() throws Exception {
        Medico medico = crearMedico();

        // Busca el médico por ID
        this.mockMvc.perform(get("/medico/" + medico.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.id").value(medico.getId()))
                .andExpect(jsonPath("$.dni").value(medico.getDni()))
                .andExpect(jsonPath("$.nombre").value(medico.getNombre()))
                .andExpect(jsonPath("$.especialidad").value(medico.getEspecialidad()));
    }

    @Test
    @DisplayName("Actualiza un medico y lo obtiene correctamente")
    void updateMedico() throws Exception {
        Medico medico = crearMedico();

        // Actualiza los datos del médico
        medico.setNombre("Pedro");
        medico.setDni("123456789B");

        this.mockMvc.perform(put("/medico")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(medico)))
                .andExpect(status().is2xxSuccessful());

        // Verifica que los datos se hayan actualizado correctamente
        this.mockMvc.perform(get("/medico/" + medico.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.nombre").value("Pedro"));
    }

    @Test
    @DisplayName("Comprueba que el medico se ha eliminado correctamente")
    void deleteMedico() throws Exception {
        Medico medico = crearMedico();

        // Elimina el médico
        this.mockMvc.perform(delete("/medico/" + medico.getId()));

        // Verifica que el médico ya no existe
        this.mockMvc.perform(get("/medico/" + medico.getId()))
                .andExpect(status().is5xxServerError());
    }
}