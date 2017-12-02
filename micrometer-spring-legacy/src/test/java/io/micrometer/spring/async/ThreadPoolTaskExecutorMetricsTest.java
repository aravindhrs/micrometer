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
package io.micrometer.spring.async;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Before;
import org.junit.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.micrometer.core.instrument.MockClock.clock;
import static io.micrometer.core.instrument.Statistic.Count;
import static io.micrometer.core.instrument.Statistic.Value;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ThreadPoolTaskExecutorMetricsTest {
    private MeterRegistry registry;
    private Iterable<Tag> userTags = Tags.zip("userTagKey", "userTagValue");

    @Before
    public void before() {
        registry = new SimpleMeterRegistry(SimpleConfig.DEFAULT, new MockClock());
    }

    @Test
    public void executor() throws InterruptedException {
        CountDownLatch lock = new CountDownLatch(1);
        ThreadPoolTaskExecutor pool = ThreadPoolTaskExecutorMetrics.monitor(registry, "exec", userTags);
        pool.setAwaitTerminationSeconds(1);
        pool.initialize();
        pool.execute(() -> {
            System.out.println("hello");
            lock.countDown();
        });
        lock.await();
        pool.shutdown();

        clock(registry).add(SimpleConfig.DEFAULT_STEP);
        assertThat(registry.mustFind("exec").tags(userTags).timer().count()).isEqualTo(1L);
        registry.mustFind("exec.completed").tags(userTags).functionCounter();
        registry.mustFind("exec.queued").tags(userTags).gauge();
        registry.mustFind("exec.active").tags(userTags).gauge();
        registry.mustFind("exec.pool").tags(userTags).gauge();
    }

    @Test
    public void monitorExecutorService() throws InterruptedException {
        CountDownLatch taskStart = new CountDownLatch(1);
        CountDownLatch taskComplete = new CountDownLatch(1);

        ThreadPoolTaskExecutor pool = ThreadPoolTaskExecutorMetrics.monitor(registry, "beep.pool", userTags);
        pool.setMaxPoolSize(1);
        pool.setAwaitTerminationSeconds(1);
        pool.setWaitForTasksToCompleteOnShutdown(true);
        pool.initialize();
        pool.submit(() -> {
            taskStart.countDown();
            taskComplete.await(1, TimeUnit.SECONDS);
            System.out.println("beep");
            return 0;
        });
        pool.submit(() -> System.out.println("boop"));

        taskStart.await(1, TimeUnit.SECONDS);
        assertThat(registry.mustFind("beep.pool.queued").tags(userTags).gauge().value()).isEqualTo(1.0);

        taskComplete.countDown();
        pool.shutdown();

        clock(registry).add(SimpleConfig.DEFAULT_STEP);
        assertThat(registry.mustFind("beep.pool").tags(userTags).timer().count()).isEqualTo(2L);
        assertThat(registry.mustFind("beep.pool.queued").tags(userTags).gauge().value()).isEqualTo(0.0);
    }
}
