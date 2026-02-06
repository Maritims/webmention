# webmention

This is a Java implementation of the [Webmention specification](https://www.w3.org/TR/2017/REC-webmention-20170112/).

> Webmention is a simple way to notify any URL when you mention it on your site. From the receiver's perspective, it's a
> way to request notifications when other sites mention it.
>
> *[https://www.w3.org/TR/2017/REC-webmention-20170112/](https://www.w3.org/TR/2017/REC-webmention-20170112/)*

## Project structure

- webmention-core: Core library.
- [webmention-cli](#webmention-cli): A CLI for sending webmentions while building a static website.
- [webmention-javalin](#webmention-javalin): A Javalin plugin for receiving webmentions.
- [webmention-service](#webmention-service): A microservice using Javalin and the webmention-javalin plugin to receive
  webmentions.

## Security measures

Be responsible when implementing and using the webmention specification. Read about these security measures before you
continue.

The nature of the specification could turn a server into a DDoS zombie if we're not diligent with our implementation. To
counter-act being turned into a DDoS zombie, the webmention-core module implements some security measures:

- **Verification delay**: All incoming requests will receive HTTP 202 Accepted. Any further processing is queued in a
  single-threaded queue with a maximum size. The queue is polled for one single element every five seconds. This
  prevents overloading the JVM. The numbers are chosen based on the expected number of incoming requests and their
  frequency.
- **Server-side request forgery protection**: Any incoming request perceived as intended for a local address is dropped.
- **Response body limiting**: We never trust that the Content-Length header does not lie about the actual size of the
  response body. Any reasonable HTML document is never more than a few kilobytes. Any data exceeding the configured
  limit is dropped.
- **Webmention request verification** according to
  the [specification](https://www.w3.org/TR/2017/REC-webmention-20170112/#request-verification).

## Usage

### webmention-cli

The CLI is built as a native executable using GraalVM. You'd typically run it in a post-publish hook after you've built
and published your website. In the example below, the CLI will recursively walk the `./dist` directory and send
webmentions to any URLs found in the HTML files it encounters.

To install, download the latest release from the [releases page](https://github.com/Maritims/webmention/releases).

```bash
webmention-cli --uri https://example.com --dir ./dist
```

The CLI accepts the following options:

- `-u`, `--uri`: The base URI of the website. Used to construct the source URLs for the webmention.
- `-d`, `--dir`: The directory to recursively walk for HTML files.
- `-dr`, `--dry-run`: Don't send any webmentions, but print the URLs that would be sent.
- `-v`, `--version`: Print the version number.
- `-h`, `--help`: Print usage information.

### webmention-core

To include the webmention-core module in your project, add the following dependency:

```xml
<dependency>
    <groupId>no.clueless</groupId>
    <artifactId>webmention-core</artifactId>
    <version>0.0.1-alpha.1</version>
</dependency>
```

### webmention-javalin

The Javalin plugin exposes an endpoint at a configurable path which can be used to receive webmentions and to retrieve
your received webmentions in a paginated manner. Receiving webmentions is described
in [the specification](https://www.w3.org/TR/2017/REC-webmention-20170112/#receiving-webmentions).

Retrieving all webmentions is done by sending a GET request to the endpoint:

```bash
curl https://example.com/webmentions 
```

The endpoint accepts the following query parameters:

- `pageNumber`: The page number to retrieve. Defaults to 0.
- `pageSize`: The number of webmentions per page. Defaults to 10.
- `orderByColumn`: The column to order the webmentions by. Defaults to `id`.
- `orderByDirection`: The direction to order the webmentions by. Defaults to `DESC`.

The plugin is registered in the following way, but for the full example have a look at the [webmention-service](#webmention-service) project, specifically the [Application](/webmention-service/src/main/java/no/clueless/webmention/service/Application.java) class:

```java
var javalin = Javalin.create(config -> {
    config.jsonMapper(new JavalinJackson(new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false), true
    ));

    config.registerPlugin(new WebmentionPlugin(plugin -> {
        plugin.setEndpoint(webmentionEndpoint);
        plugin.setProcessor(webmentionProcessor);
        plugin.setSender(webmentionSender);
        plugin.setWebmentionRepository(webmentionRepository);
        plugin.setTestMode(testMode);
    }));
});
```

### webmention-service

The service is a simple Javalin application that receives webmentions and stores them in a database. It can be run in a
container and should be deployed behind a reverse proxy which handles SSL termination and rate limiting. You don't want
to be turned into a DDoS zombie. I like [nginx](https://nginx.org/).

For the easiest deployment, use [the provided Dockerfile](/webmention-service/Dockerfile) to build an image and run it
with Docker:

The service accepts the following environment variables:

- `WEBMENTION_SERVER_PORT`: The port to listen on. Required. Defaults to 8080.
- `WEBMENTION_DB_CONNECTION_STRING`: The JDBC connection string for the database. Required. Defaults to a SQLite
  database in the
  current directory named `webmentions.db`.
- `WEBMENTION_ENDPOINT`: The endpoint to listen on for webmentions. Required. Defaults to `/webmention`.
- `WEBMENTION_SUPPORTED_DOMAINS`: A comma-separated list of domains for which webmentions are accepted. Required.
- `WEBMENTION_TEST_MODE`: If set to `true`, the service will expose two test endpoints for receiving and sending
  webmentions. Defaults to `false`.
- `WEBMENTION_CONNECTION_TIMEOUT_IN_MILLISECONDS`: The timeout for establishing a connection to target URLs and
  webmention endpoints. Defaults to 5000 milliseconds.