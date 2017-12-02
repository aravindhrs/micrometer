/**
 * Copyright 2017 Pivotal Software, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.core.instrument.binder.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.micrometer.core.Issue;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static io.micrometer.core.instrument.MockClock.clock;
import static org.assertj.core.api.Assertions.assertThat;

class LogbackMetricsTest {
    private MeterRegistry registry = new SimpleMeterRegistry(SimpleConfig.DEFAULT, new MockClock());
    private Logger logger = (Logger) LoggerFactory.getLogger("foo");

    @BeforeEach
    void bindLogbackMetrics() {
        new LogbackMetrics().bindTo(registry);
    }

    @Test
    void logbackLevelMetrics() {
        assertThat(registry.mustFind("logback.events").counter().count()).isEqualTo(0.0);

        logger.setLevel(Level.INFO);

        logger.warn("warn");
        logger.error("error");
        logger.debug("debug"); // shouldn't record a metric

        clock(registry).add(SimpleConfig.DEFAULT_STEP);
        assertThat(registry.mustFind("logback.events").tags("level", "warn").counter().count()).isEqualTo(1.0);
        assertThat(registry.mustFind("logback.events").tags("level", "debug").counter().count()).isEqualTo(0.0);
    }

    @Issue("#183")
    @Test
    void isLevelEnabledDoesntContributeToCounts() {
        logger.isErrorEnabled();

        clock(registry).add(SimpleConfig.DEFAULT_STEP);
        assertThat(registry.mustFind("logback.events").tags("level", "error").counter().count()).isEqualTo(0.0);
    }
}
