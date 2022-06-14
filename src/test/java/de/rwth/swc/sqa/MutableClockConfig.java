package de.rwth.swc.sqa;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.threeten.extra.MutableClock;

import java.time.Instant;
import java.time.ZoneId;

@TestConfiguration
public class MutableClockConfig {

    @Primary
    @Bean
    MutableClock provideMutableClock() {
        return MutableClock.of(Instant.EPOCH, ZoneId.systemDefault());
    }
}
