/*
 * Licensed to Cloudera, Inc. under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloudera.flink.config;

import org.apache.flink.annotation.docs.Documentation;
import org.apache.flink.configuration.ConfigOption;

import static org.apache.flink.configuration.ConfigOptions.key;

/** The set of configuration options relating to basic authentication. */
public class BasicAuthOptions {

    /** Basic authentication enabled. */
    @Documentation.Section(Documentation.Sections.EXPERT_REST)
    public static final ConfigOption<Boolean> BASIC_AUTH_ENABLED =
            key("security.basic.auth.enabled")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Turns on/off basic authentication.");

    /** Basic authentication password file. */
    @Documentation.Section(Documentation.Sections.EXPERT_REST)
    public static final ConfigOption<String> BASIC_AUTH_PWD_FILE =
            key("security.basic.auth.password.file")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Basic authentication password file.");

    /** Basic authentication client credentials user:pwd. */
    @Documentation.Section(Documentation.Sections.EXPERT_REST)
    public static final ConfigOption<String> BASIC_AUTH_CLIENT_CREDENTIALS =
            key("security.basic.auth.client.credentials")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Basic authentication client credentials user:pwd.");
}
