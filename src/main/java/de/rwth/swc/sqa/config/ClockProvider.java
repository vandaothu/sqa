package de.rwth.swc.sqa.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

public class ClockProvider {

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    Clock provideClock(){
        return Clock.systemDefaultZone();
    }

}
