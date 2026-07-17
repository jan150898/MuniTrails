package com.example.trails.web;

import com.example.trails.dto.TrackResponse;
import com.example.trails.service.TrackService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(TourPageController.class)
@WithMockUser
class TourPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private TrackService trackService;

    @Test
    void rendersExistingTour() throws Exception {
        UUID id = UUID.randomUUID();
        TrackResponse tour = new TrackResponse();
        tour.setId(id);
        tour.setName("Alpine loop");
        when(trackService.findSummaryById(id)).thenReturn(Optional.of(tour));

        mockMvc.perform(get("/tour/{trackId}", id))
                .andExpect(status().isOk())
                .andExpect(view().name("tour"))
                .andExpect(model().attribute("tour", tour));
    }

    @Test
    void returnsNotFoundForUnknownTour() throws Exception {
        UUID id = UUID.randomUUID();
        when(trackService.findSummaryById(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/tour/{trackId}", id))
                .andExpect(status().isNotFound());
    }
}
