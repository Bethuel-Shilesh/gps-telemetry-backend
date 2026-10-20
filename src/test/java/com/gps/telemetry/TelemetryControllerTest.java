package com.gps.telemetry;

import com.gps.telemetry.controller.TelemetryController;
import com.gps.telemetry.model.Telemetry;
import com.gps.telemetry.service.TelemetryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TelemetryController.class)
class TelemetryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    

    @MockitoBean
    private TelemetryService telemetryService;

    private String validRequest() {
        return """
            {
              "vehicleId": "TEST_VEHICLE_001",
              "timestamp": "2026-10-20T10:00:00Z",
              "latitude": 19.033,
              "longitude": 73.0297,
              "speed": 40.0,
              "harshBrake": false,
              "harshAcceleration": false,
              "ignition": true,
              "batteryVoltage": 12.4,
              "crashEvent": false
            }
            """;
    }

    @Test
    void validTelemetryReturnsSuccess() throws Exception {
        when(telemetryService.saveTelemetry(any(Telemetry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleId")
                        .value("TEST_VEHICLE_001"))
                .andExpect(jsonPath("$.latitude").value(19.033))
                .andExpect(jsonPath("$.longitude").value(73.0297));
    }

    @Test
    void latitudeAboveAllowedRangeIsRejected() throws Exception {
        String request = validRequest()
                .replace("\"latitude\": 19.033", "\"latitude\": 95.0");

        mockMvc.perform(post("/api/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void longitudeAboveAllowedRangeIsRejected() throws Exception {
        String request = validRequest()
                .replace("\"longitude\": 73.0297", "\"longitude\": 200.0");

        mockMvc.perform(post("/api/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void negativeSpeedIsRejected() throws Exception {
        String request = validRequest()
                .replace("\"speed\": 40.0", "\"speed\": -10.0");

        mockMvc.perform(post("/api/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blankVehicleIdIsRejected() throws Exception {
        String request = validRequest()
                .replace("\"vehicleId\": \"TEST_VEHICLE_001\"",
                         "\"vehicleId\": \"   \"");

        mockMvc.perform(post("/api/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }
}
