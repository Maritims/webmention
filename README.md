# webmention

This is a Java implementation of the [Webmention specification](https://www.w3.org/TR/2017/REC-webmention-20170112/).

> Webmention is a simple way to notify any URL when you mention it on your site. From the receiver's perspective, it's a
> way to request notifications when other sites mention it.
>
> *[https://www.w3.org/TR/2017/REC-webmention-20170112/](https://www.w3.org/TR/2017/REC-webmention-20170112/)*

## Project structure

- webmention-core: Core library.
- webmention-cli: A CLI for sending webmentions while building a static website.
- webmention-javalin: A Javalin plugin for receiving webmentions.
- webmention-service: A microservice using Javalin and the webmention-javalin plugin to receive webmentions.

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

```bash
webmention-cli --uri https://example.com --dir ./dist
```

### webmention-javalin

The Javalin plugin exposes an endpoint at a configurable path which can be used to receive webmentions, and to retrieve
your received webmentions in a paginated manner. Receiving webmentions is described in [the specification](https://www.w3.org/TR/2017/REC-webmention-20170112/#receiving-webmentions).

Retrieving all webmentions is done by sending a GET request to the endpoint:

```bash
curl https://example.com/webmentions 
```

The endpoint accepts the following query parameters:

- pageNumber: The page number to retrieve. Defaults to 0.
- pageSize: The number of webmentions per page. Defaults to 10.
- orderByColumn: The column to order the webmentions by. Defaults to `id`.
- orderByDirection: The direction to order the webmentions by. Defaults to `DESC`.

### webmention-service

The service is a simple Javalin application that receives webmentions and stores them in a database. It can be run in a
container and should be deployed behind a reverse proxy which handles SSL termination and rate limiting. You don't want
to be turned into a DDoS zombie. I like [nginx](https://nginx.org/).