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
import org.apache.flink.runtime.io.network.netty.OutboundChannelHandlerFactory;
import org.apache.flink.util.ConfigurationException;

import org.apache.flink.shaded.netty4.io.netty.channel.ChannelHandler;

import com.cloudera.flink.config.BasicAuthOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class ClientBasicAuthHandlerFactory implements OutboundChannelHandlerFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ClientBasicAuthHandlerFactory.class);

    public ClientBasicAuthHandlerFactory() {}

    @Override
    public String toString() {
        return super.toString() + " priority: " + priority();
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public Optional<ChannelHandler> createHandler(Configuration configuration)
            throws ConfigurationException {
        if (!configuration.getBoolean(BasicAuthOptions.BASIC_AUTH_ENABLED)) {
            return Optional.empty();
        }

        String credentials =
                configuration
                        .getOptional(BasicAuthOptions.BASIC_AUTH_CLIENT_CREDENTIALS)
                        .orElseThrow(
                                () ->
                                        new ConfigurationException(
                                                BasicAuthOptions.BASIC_AUTH_CLIENT_CREDENTIALS.key()
                                                        + " must be configured if basic auth is enabled."));

        LOG.info("Creating basic client authentication handler");
        return Optional.of(new ClientBasicHttpAuthenticator(credentials));
    }
}
