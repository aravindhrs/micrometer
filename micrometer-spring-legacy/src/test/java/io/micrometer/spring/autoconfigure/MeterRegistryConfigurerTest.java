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
package io.micrometer.spring.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jon Schneider
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {
    "spring.metrics.useGlobalRegistry=false",
    "spring.metrics.atlas.enabled=false",
    "spring.metrics.prometheus.enabled=false",
    "spring.metrics.datadog.enabled=false",
    "spring.metrics.ganglia.enabled=false",
    "spring.metrics.graphite.enabled=false",
    "spring.metrics.influx.enabled=false",
    "spring.metrics.jmx.enabled=false",
    "spring.metrics.statsd.enabled=false",
    "spring.metrics.newrelic.enabled=false"
})
public class MeterRegistryConfigurerTest {

    @Autowired
    MeterRegistry registry;

    @Test
    public void commonTagsAreAppliedToAutoConfiguredBinders() {
        registry.mustFind("jvm.memory.used").tags("region", "us-east-1").gauge();
    }

    @SpringBootApplication(scanBasePackages = "isolated")
    static class MetricsApp {
        public static void main(String[] args) {
            SpringApplication.run(MetricsApp.class);
        }

        @Bean
        public MeterRegistryConfigurer registryConfigurer() {
            return registry -> registry.config().commonTags("region", "us-east-1");
        }
    }
}
