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
package io.micrometer.spring.integration;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.Statistic;
import io.micrometer.core.instrument.simple.SimpleConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.support.Transformers;
import org.springframework.integration.ws.SimpleWebServiceOutboundGateway;
import org.springframework.integration.ws.WebServiceHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {
    "spring.metrics.useGlobalRegistry=false",
    "spring.metrics.atlas.enabled=false",
    "spring.metrics.datadog.enabled=false",
    "spring.metrics.ganglia.enabled=false",
    "spring.metrics.influx.enabled=false",
    "spring.metrics.jmx.enabled=false",
    "spring.metrics.statsd.enabled=false",
    "spring.metrics.newrelic.enabled=false"
})
public class SpringIntegrationMetricsTest {
    @Autowired
    TestSpringIntegrationApplication.TempConverter converter;

    @Autowired
    MeterRegistry registry;

    @Autowired
    MockClock clock;

    @Test
    public void springIntegrationMetrics() {
        converter.fahrenheitToCelcius(68.0f);

        clock.add(SimpleConfig.DEFAULT_STEP);
        assertThat(registry.mustFind("spring.integration.channel.sends")
            .tags("channel", "convert.input").functionCounter().count()).isEqualTo(1.0);
        assertThat(registry.mustFind("spring.integration.handler.duration.min").meter()).isPresent();
        assertThat(registry.mustFind("spring.integration.sourceNames").meter()).isPresent();
    }

    @SpringBootApplication
    @IntegrationComponentScan
    public static class TestSpringIntegrationApplication {
        @Bean
        MockClock clock() {
            return new MockClock();
        }

        @MessagingGateway
        public interface TempConverter {
            @Gateway(requestChannel = "convert.input")
            float fahrenheitToCelcius(float fahren);
        }

        @Bean
        public IntegrationFlow convert() {
            return f -> f
                .transform(payload ->
                    "<FahrenheitToCelsius xmlns=\"https://www.w3schools.com/xml/\">"
                        + "<Fahrenheit>" + payload + "</Fahrenheit>"
                        + "</FahrenheitToCelsius>", e -> e.id("toXml"))
                .enrichHeaders(h -> h
                    .header(WebServiceHeaders.SOAP_ACTION,
                        "https://www.w3schools.com/xml/FahrenheitToCelsius"))
                .handle(new SimpleWebServiceOutboundGateway(
                    "https://www.w3schools.com/xml/tempconvert.asmx"), e -> e.id("w3schools"))
                .transform(Transformers.xpath("/*[local-name()=\"FahrenheitToCelsiusResponse\"]"
                    + "/*[local-name()=\"FahrenheitToCelsiusResult\"]"), e -> e.id("toResponse"));
        }
    }
}
