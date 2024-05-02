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

package org.apache.flink.runtime.rest;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.runtime.rest.util.TestRestServerEndpoint;
import org.apache.flink.runtime.rpc.RpcUtils;
import org.apache.flink.runtime.webmonitor.RestfulGateway;
import org.apache.flink.runtime.webmonitor.TestingRestfulGateway;
import org.apache.flink.testutils.TestingUtils;
import org.apache.flink.testutils.executor.TestExecutorExtension;

import com.cloudera.flink.config.BasicAuthOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

/** This test validates basic auth for rest endpoints. */
public class RestBasicAuthTest {

    @RegisterExtension
    static final TestExecutorExtension<ScheduledExecutorService> EXECUTOR_RESOURCE =
            TestingUtils.defaultExecutorExtension();

    private static final String PASSWORD_FILE =
            requireNonNull(RestBasicAuthTest.class.getResource("/.htpasswd")).getFile();

    private static RestServerEndpoint serverEndpoint;
    private static RestServerEndpointITCase.TestVersionHandler testVersionHandler;

    @BeforeAll
    public static void init() throws Exception {
        Configuration serverConfig = getConfig();

        RestfulGateway restfulGateway = new TestingRestfulGateway.Builder().build();
        testVersionHandler =
                new RestServerEndpointITCase.TestVersionHandler(
                        () -> CompletableFuture.completedFuture(restfulGateway),
                        RpcUtils.INF_TIMEOUT);

        serverEndpoint =
                TestRestServerEndpoint.builder(serverConfig)
                        .withHandler(testVersionHandler.getMessageHeaders(), testVersionHandler)
                        .build();
        serverEndpoint.start();
    }

    @Test
    public void testAuthGoodCredentials() throws Exception {
        Configuration goodCredsConf = getConfig();
        goodCredsConf.set(BasicAuthOptions.BASIC_AUTH_CLIENT_CREDENTIALS, "testusr:testpwd");
        try (RestClient restClientWithGoodCredentials =
                new RestClient(goodCredsConf, EXECUTOR_RESOURCE.getExecutor())) {
            InetSocketAddress serverAddress = serverEndpoint.getServerAddress();
            assertNotNull(serverAddress);
            restClientWithGoodCredentials
                    .sendRequest(
                            serverAddress.getHostString(),
                            serverAddress.getPort(),
                            testVersionHandler.getMessageHeaders())
                    .get();
        }
    }

    @Test
    public void testAuthBadCredentials() throws Exception {
        Configuration badCredsConf = getConfig();
        badCredsConf.set(BasicAuthOptions.BASIC_AUTH_CLIENT_CREDENTIALS, "wrong:pwd");
        try (RestClient restClientWithBadCredentials =
                new RestClient(badCredsConf, EXECUTOR_RESOURCE.getExecutor())) {
            InetSocketAddress serverAddress = serverEndpoint.getServerAddress();
            assertNotNull(serverAddress);
            restClientWithBadCredentials
                    .sendRequest(
                            serverAddress.getHostString(),
                            serverAddress.getPort(),
                            testVersionHandler.getMessageHeaders())
                    .get();
            fail();
        } catch (ExecutionException ee) {
            assertTrue(ee.getCause().getMessage().contains("Invalid credentials"));
        }
    }

    private static Configuration getConfig() {
        final Configuration conf = new Configuration();
        conf.setString(RestOptions.BIND_PORT, "0");
        conf.setString(RestOptions.ADDRESS, "localhost");
        conf.set(BasicAuthOptions.BASIC_AUTH_ENABLED, true);
        conf.set(BasicAuthOptions.BASIC_AUTH_PWD_FILE, PASSWORD_FILE);
        return conf;
    }
}
