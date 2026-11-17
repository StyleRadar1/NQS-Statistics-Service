package edu.dosw.styleradar.estadisticas.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeSeriesClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
