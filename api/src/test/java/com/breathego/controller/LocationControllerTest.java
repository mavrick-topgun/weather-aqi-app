package com.breathego.controller;

import com.breathego.dto.LocationRequest;
import com.breathego.dto.LocationResponse;
import com.breathego.service.LocationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LocationController.class)
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LocationService locationService;

    @Test
    @DisplayName("GET /api/locations should return all locations")
    void getAllLocations() throws Exception {
        LocationResponse loc1 = new LocationResponse(1L, "New York",
                new BigDecimal("40.7128"), new BigDecimal("-74.0060"), "auto", OffsetDateTime.now());
        LocationResponse loc2 = new LocationResponse(2L, "Los Angeles",
                new BigDecimal("34.0522"), new BigDecimal("-118.2437"), "auto", OffsetDateTime.now());

        when(locationService.getAllLocations()).thenReturn(List.of(loc1, loc2));

        mockMvc.perform(get("/api/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("New York"))
                .andExpect(jsonPath("$[1].name").value("Los Angeles"));
    }

    @Test
    @DisplayName("GET /api/locations/{id} should return location by id")
    void getLocationById() throws Exception {
        LocationResponse location = new LocationResponse(1L, "New York",
                new BigDecimal("40.7128"), new BigDecimal("-74.0060"), "auto", OffsetDateTime.now());

        when(locationService.getLocation(1L)).thenReturn(location);

        mockMvc.perform(get("/api/locations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("New York"));
    }

    @Test
    @DisplayName("GET /api/locations/{id} should return 404 for non-existent location")
    void getLocationNotFound() throws Exception {
        when(locationService.getLocation(99L))
                .thenThrow(new LocationService.LocationNotFoundException(99L));

        mockMvc.perform(get("/api/locations/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /api/locations should create new location")
    void createLocation() throws Exception {
        LocationRequest request = new LocationRequest("New York",
                new BigDecimal("40.7128"), new BigDecimal("-74.0060"), null);

        LocationResponse response = new LocationResponse(1L, "New York",
                new BigDecimal("40.7128"), new BigDecimal("-74.0060"), "auto", OffsetDateTime.now());

        when(locationService.createLocation(any())).thenReturn(response);

        mockMvc.perform(post("/api/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("New York"));
    }

    @Test
    @DisplayName("POST /api/locations should return 400 for invalid request")
    void createLocationValidationError() throws Exception {
        LocationRequest request = new LocationRequest("",
                new BigDecimal("91"), new BigDecimal("-74.0060"), null);

        mockMvc.perform(post("/api/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("DELETE /api/locations/{id} should delete location")
    void deleteLocation() throws Exception {
        doNothing().when(locationService).deleteLocation(1L);

        mockMvc.perform(delete("/api/locations/1"))
                .andExpect(status().isNoContent());

        verify(locationService).deleteLocation(1L);
    }

    @Test
    @DisplayName("DELETE /api/locations/{id} should return 404 for non-existent location")
    void deleteLocationNotFound() throws Exception {
        doThrow(new LocationService.LocationNotFoundException(99L))
                .when(locationService).deleteLocation(99L);

        mockMvc.perform(delete("/api/locations/99"))
                .andExpect(status().isNotFound());
    }
}
