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

package com.cloudera.flink.netty;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.io.network.netty.InboundChannelHandlerFactory;
import org.apache.flink.util.ConfigurationException;

import org.apache.flink.shaded.netty4.io.netty.channel.ChannelHandler;

import com.cloudera.flink.config.BasicAuthOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class ServerBasicAuthHandlerFactory implements InboundChannelHandlerFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ServerBasicAuthHandlerFactory.class);

    public ServerBasicAuthHandlerFactory() {}

    @Override
    public String toString() {
        return super.toString() + " priority: " + priority();
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public Optional<ChannelHandler> createHandler(
            Configuration configuration, Map<String, String> responseHeaders)
            throws ConfigurationException {
        if (!configuration.getBoolean(BasicAuthOptions.BASIC_AUTH_ENABLED)) {
            return Optional.empty();
        }

        String pwdFile =
                configuration
                        .getOptional(BasicAuthOptions.BASIC_AUTH_PWD_FILE)
                        .orElseThrow(
                                () ->
                                        new ConfigurationException(
                                                BasicAuthOptions.BASIC_AUTH_PWD_FILE.key()
                                                        + " must be configured if basic auth is enabled."));

        Map<String, String> credentials = new HashMap<>();
        try (Stream<String> stream = Files.lines(Paths.get(pwdFile))) {
            stream.forEach(
                    line -> {
                        if (!line.isEmpty()) {
                            String[] split = line.split(":", 2);
                            credentials.put(split[0], split[1]);
                        }
                    });
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }

        LOG.info("Creating basic server authentication handler");
        return Optional.of(new ServerBasicHttpAuthenticator(credentials, responseHeaders));
    }
}
