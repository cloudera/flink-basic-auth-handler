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

import org.apache.flink.runtime.rest.handler.util.HandlerUtils;
import org.apache.flink.runtime.rest.messages.ErrorResponseBody;

import org.apache.flink.shaded.netty4.io.netty.channel.ChannelHandler;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelHandlerContext;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.flink.shaded.netty4.io.netty.handler.codec.http.HttpHeaderNames;
import org.apache.flink.shaded.netty4.io.netty.handler.codec.http.HttpHeaders;
import org.apache.flink.shaded.netty4.io.netty.handler.codec.http.HttpRequest;
import org.apache.flink.shaded.netty4.io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.flink.shaded.netty4.io.netty.util.ReferenceCountUtil;

import org.apache.commons.codec.digest.Md5Crypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/** Netty handler for basic authentication on the Server side. */
@ChannelHandler.Sharable
public class ServerBasicHttpAuthenticator extends ChannelInboundHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(ServerBasicHttpAuthenticator.class);

    private final Map<String, String> responseHeaders;

    private final Map<String, String> credentials;

    public ServerBasicHttpAuthenticator(
            Map<String, String> credentials, final Map<String, String> responseHeaders) {
        this.responseHeaders = new HashMap<>(requireNonNull(responseHeaders));
        this.responseHeaders.put(
                HttpHeaderNames.WWW_AUTHENTICATE.toString(), "Basic realm=\"flink\"");
        this.credentials = credentials;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest) {
            try {
                HttpHeaders headers = ((HttpRequest) msg).headers();

                /*
                 * look for auth token
                 */
                String auth = headers.get(HttpHeaderNames.AUTHORIZATION);
                if (auth == null) {
                    final String errorMessage = "Missing authorization header";
                    LOG.error(errorMessage);
                    HandlerUtils.sendErrorResponse(
                            ctx,
                            false,
                            new ErrorResponseBody(errorMessage),
                            HttpResponseStatus.UNAUTHORIZED,
                            responseHeaders);
                    return;
                } else {
                    LOG.debug("Authorization header found");
                }
                int sp = auth.indexOf(' ');
                if (sp < 0 || !auth.substring(0, sp).equals("Basic")) {
                    final String errorMessage = "Unknown authorization method";
                    LOG.error(errorMessage);
                    HandlerUtils.sendErrorResponse(
                            ctx,
                            false,
                            new ErrorResponseBody(errorMessage),
                            HttpResponseStatus.UNAUTHORIZED,
                            responseHeaders);
                    return;
                } else {
                    LOG.debug("Valid authorization method found");
                }
                String userPass = new String(Base64.getDecoder().decode(auth.substring(sp + 1)));
                int colon = userPass.indexOf(':');
                if (colon < 0) {
                    final String errorMessage = "No password found in basic authentication header";
                    LOG.error(errorMessage);
                    HandlerUtils.sendErrorResponse(
                            ctx,
                            false,
                            new ErrorResponseBody(errorMessage),
                            HttpResponseStatus.UNAUTHORIZED,
                            responseHeaders);
                    return;
                } else {
                    LOG.debug("Password found");
                }
                String user = userPass.substring(0, colon);
                String password = userPass.substring(colon + 1);

                if (checkCredentials(user, password)) {
                    LOG.debug("User {} authenticated successfully", user);
                    ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
                } else {
                    final String errorMessage = "Invalid credentials";
                    LOG.error(errorMessage);
                    HandlerUtils.sendErrorResponse(
                            ctx,
                            false,
                            new ErrorResponseBody(errorMessage),
                            HttpResponseStatus.UNAUTHORIZED,
                            responseHeaders);
                }
            } catch (Exception e) {
                LOG.error("Exception while authenticating user", e);
                HandlerUtils.sendErrorResponse(
                        ctx,
                        false,
                        new ErrorResponseBody("Invalid credentials"),
                        HttpResponseStatus.UNAUTHORIZED,
                        responseHeaders);
            }
        } else {
            // Only HttpRequests are authenticated
            ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
        }
    }

    /**
     * Checks credentials against the stored hashes using salted MD5 hashing as described in
     * http://httpd.apache.org/docs/2.2/misc/password_encryptions.html. This is the default format
     * produced by the htpasswd command.
     *
     * @param username Username provided in the http request
     * @param password Password provided in the http request
     * @return True if username & password match the stored credentials
     */
    private boolean checkCredentials(String username, String password) {
        String storedPasswordHash = credentials.get(username);
        if (storedPasswordHash == null) {
            LOG.error("No stored credentials found");
            return false;
        }

        String computedPwdHash =
                Md5Crypt.apr1Crypt(password.getBytes(), storedPasswordHash.substring(6, 14));
        return storedPasswordHash.equals(computedPwdHash);
    }
}
