# oauth2-javalin

This plugin provides a lightweight OAuth 2.0 Authorization Server implementation for Javalin.

## Installation

Add the dependency to your `pom.xml`:

> Ensure that your project also has `javalin` and `slf4j-api` as dependencies since they are required by this library.

```xml

<dependency>
    <groupId>com.auth0</groupId>
    <artifactId>javalin-oauth2-server</artifactId>
    <version>REPLACE_THIS_WITH_THE_PROPER_VERSION</version>
</dependency>
```

## Usage

Register the plugin in your Javalin application. You must provide an implementation of `ClientStore` to manage your
clients.

```java
import com.auth0.jwt.algorithms.Algorithm;
import no.clueless.oauth2.core.DefaultTokenManager;

var app = Javalin.create(config -> {
    var clientStore = new SomeClientStoreImplementation(); // Replace this with your implementation.
    var jwtManager  = new DefaultTokenManager(Algorithm.HMAC256("your-secret-key-here"), "your-issuer-url-here", 3600);
    config.registerPlugin(new OAuthServerPlugin(clientStore, jwtManager));
    config.registerPlugin(new OAuthResourceServerPlugin<>(jwtManager, jwtManager));
}).start(8080);
```