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

package org.apache.flink.runtime.webmonitor.history;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.HistoryServerOptions;

import com.cloudera.flink.config.BasicAuthOptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.Base64;

import static java.util.Objects.requireNonNull;

/** Test for the HistoryServer Basic authentication integration. */
class HistoryServerBasicAuthTest {

    private static final String PASSWORD_FILE =
            requireNonNull(HistoryServerBasicAuthTest.class.getResource("/.htpasswd")).getFile();

    private static final String USER_CREDENTIALS = "testusr:testpwd";

    @TempDir private Path tempDir;

    @Test
    void testAuthGoodCredentials() throws Exception {
        Configuration serverConfig = getServerConfig();
        HistoryServer hs = new HistoryServer(serverConfig);

        try {
            hs.start();
            String baseUrl = "http://localhost:" + hs.getWebPort();
            String authHeader =
                    "Basic " + new String(Base64.getEncoder().encode(USER_CREDENTIALS.getBytes()));
            Assertions.assertEquals(200, getHTTPResponseCode(baseUrl, authHeader));
        } finally {
            hs.stop();
        }
    }

    @Test
    void testAuthBadCredentials() throws Exception {
        Configuration serverConfig = getServerConfig();
        HistoryServer hs = new HistoryServer(serverConfig);

        try {
            hs.start();
            String baseUrl = "http://localhost:" + hs.getWebPort();
            Assertions.assertEquals(401, getHTTPResponseCode(baseUrl, null));
        } finally {
            hs.stop();
        }
    }

    private Configuration getServerConfig() {
        Configuration config = new Configuration();
        config.setString(
                HistoryServerOptions.HISTORY_SERVER_ARCHIVE_DIRS,
                tempDir.resolve("jm").toFile().toURI().toString());
        config.set(BasicAuthOptions.BASIC_AUTH_PWD_FILE, PASSWORD_FILE);
        config.set(BasicAuthOptions.BASIC_AUTH_ENABLED, true);
        config.set(BasicAuthOptions.BASIC_AUTH_CLIENT_CREDENTIALS, USER_CREDENTIALS);
        return config;
    }

    private int getHTTPResponseCode(String url, String authHeader) throws Exception {
        URL u = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) u.openConnection();
        connection.setConnectTimeout(10000);
        if (authHeader != null) {
            connection.setRequestProperty("Authorization", authHeader);
        }
        connection.connect();
        return connection.getResponseCode();
    }
}
