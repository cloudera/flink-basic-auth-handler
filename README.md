# Flink basic authentication handler

It implements custom netty HTTP request inbound/outbound handlers to add basic
authentication possibility to Flink.
Please see [FLIP-181](https://cwiki.apache.org/confluence/x/CAUBCw) for further details.

## How to build
In order to build the project one needs Maven and Java.

The authentication handler requires Flink version 1.16 or higher. To test with a specific Flink version, set the
property `flink.version`. For example, to test with Flink 1.17.2, run:
```
./mvnw -Dflink.version=1.17.2 clean verify
```

To build the authentication handler, run:
```
./mvnw clean package
```

## How to install
In order to install in one just needs to do the following:
* Make sure the following provided dependencies are available on the cluster:
  * `flink-runtime`
  * `commons-codec`
* Add the following jar to the classpath:
```
target/flink-basic-auth-handler-<VERSION>.jar
```
As described in the mentioned implementation proposal Flink loads all
inbound/outbound handlers with service loader automatically.

## How to configure

The following configuration properties are supported:

| Property                               | Type    | Default | Description                                      |
|----------------------------------------|---------|---------|--------------------------------------------------|
| security.basic.auth.enabled            | boolean | false   | Turns on/off basic authentication                |
| security.basic.auth.password.file      | string  | (none)  | Basic authentication password file               |
| security.basic.auth.client.credentials | string  | (none)  | Basic authentication client credentials user:pwd |

## License
This is licensed under Apache License Version 2.0.
You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
