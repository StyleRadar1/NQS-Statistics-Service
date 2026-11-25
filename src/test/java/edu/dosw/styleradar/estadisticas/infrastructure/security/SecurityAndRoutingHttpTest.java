package edu.dosw.styleradar.estadisticas.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAndRoutingHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void statisticsRootRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/statistics"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .andExpect(header().exists(CorrelationIdFilter.REQUEST_ID_HEADER));
    }

    @Test
    void unknownStatisticsRouteWithAuthenticationReturns404InsteadOf500() throws Exception {
        mockMvc.perform(get("/statistics")
                        .header("X-User-Role", "ADMIN_STYLERADAR"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROUTE_NOT_FOUND"));
    }
}
