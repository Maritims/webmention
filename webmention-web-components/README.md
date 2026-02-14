# webmention-web-components

This is a collection of web components for webmentions written in vanilla JavaScript.

> Webmention is a simple way to notify any URL when you mention it on your site. From the receiver's perspective, it's a
> way to request notifications when other sites mention it.
>
> *[https://www.w3.org/TR/2017/REC-webmention-20170112/](https://www.w3.org/TR/2017/REC-webmention-20170112/)*


## Components

The following components are available:

### webmention-list

A simple webmention list component. It fetches webmentions from a given URL and displays them in a list.

The following attributes are available:

- `endpoint`: The URL of the webmention endpoint to fetch webmentions from.
- `heading`: The heading to display above the webmentions list.
- `hide-when-empty`: If set to `true`, the webmention list will be hidden when there are no webmentions to display.

#### Example

```html
<webmention-list endpoint="https://my-website.com/api/webmention" heading="See what others are saying" hide-when-empty="true"></webmention-list>
```

### webmention-send

A simple webmention send component. It allows you to send a webmention informing the receiver that you've mentioned one of their pages (the target URL) somewhere on your website (the source URL).

The following attributes are available:

- `endpoint`: The URL of the webmention endpoint to send webmentions to. This would be your own webmention endpoint.

#### Example

```html
<webmention-send endpoint="https://my-website.com/api/webmention"></webmention-send>
```

## Installation

### Using npm

Install the webmention-web-components package:

```bash
npm install webmention-web-components
```

Import the webmention-web-components module in your JavaScript file:

```js
import 'webmention-web-components';
```


### Not using npm

```html
<script src="path/to/your/webmention-web-components.js" type="module"></script>
```

Refer to the [Components](#components) section for more information about each component and usage examples.