package com.example.trails.web;

import com.example.trails.dto.TrackResponse;
import com.example.trails.service.TrackService;
import com.example.trails.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(TourPageController.class)
@WithMockUser
class TourPageControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private TrackService trackService;
    @MockBean private UserService userService;

    @Test
    void rendersExistingTour() throws Exception {
        UUID id = UUID.randomUUID();
        TrackResponse tour = new TrackResponse();
        tour.setId(id);
        tour.setName("Alpine loop");
        var user = mock(com.example.trails.model.User.class);
        when(userService.getUserByEmail("user")).thenReturn(user);
        when(trackService.requireReadable(id, user)).thenReturn(null);
        when(trackService.findSummaryById(id)).thenReturn(Optional.of(tour));

        mockMvc.perform(get("/tour/{trackId}", id))
                .andExpect(status().isOk())
                .andExpect(view().name("tour"))
                .andExpect(model().attribute("tour", tour));
            verify(trackService).requireReadable(id, user);
    }

    @Test
    void returnsNotFoundForUnknownTour() throws Exception {
        UUID id = UUID.randomUUID();
        var user = mock(com.example.trails.model.User.class);
        when(userService.getUserByEmail("user")).thenReturn(user);
        when(trackService.requireReadable(id, user)).thenReturn(null);
        when(trackService.findSummaryById(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/tour/{trackId}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsForbiddenForUnreadableTour() throws Exception {
        UUID id = UUID.randomUUID();
        var user = mock(com.example.trails.model.User.class);
        when(userService.getUserByEmail("user")).thenReturn(user);
        when(trackService.requireReadable(id, user))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN));

        mockMvc.perform(get("/tour/{trackId}", id))
                .andExpect(status().isForbidden());
    }
}
