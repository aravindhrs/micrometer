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
package io.micrometer.cloudwatch;

import io.micrometer.core.instrument.step.StepRegistryConfig;

/**
 * Configuration for CloudWatch exporting.
 *
 * @author Dawid Kublik
 */
public interface CloudWatchConfig extends StepRegistryConfig {

    int MAX_BATCH_SIZE = 20;

    /**
     * Accept configuration defaults
     */
    CloudWatchConfig DEFAULT = k -> null;

    @Override
    default String prefix() {
        return "cloudwatch";
    }

    default String namespace() {
        String v = get(prefix() + ".namespace");
        if(v == null)
            throw new IllegalStateException(prefix() + ".namespace must be set to report metrics to CloudWatch");
        return v;
    }

    @Override
    default int batchSize() {
        String v = get(prefix() + ".batchSize");
        int vInt = v == null ? MAX_BATCH_SIZE : Integer.parseInt(v);
        if(vInt > MAX_BATCH_SIZE)
            throw new IllegalStateException(prefix() + ".batchSize must be <= " + MAX_BATCH_SIZE);

        return vInt;
    }

}
