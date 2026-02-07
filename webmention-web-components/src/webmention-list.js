import {fetchWebmentions} from './utils.js';

/**
 * A component for displaying webmentions fetched from a specified endpoint.
 * @attribute hide-when-empty Whether to hide the webmentions section when there are no webmentions to display. Defaults to false.
 * @attribute endpoint The webmention endpoint to fetch from.
 * @attribute heading The heading to display above the webmentions. Defaults to "Mentions".
 * @example <webmention-list heading="Read what others are saying" endpoint="https://example.com/webmention/endpoint" hide-when-empty="false"></webmention-list>/
 */
export class WebmentionList extends HTMLElement {
    /**
     * @type {string} The webmention endpoint to fetch from.
     * @private
     */
    _endpoint;
    /**
     * The webmentions fetched from the endpoint.
     * @type {Webmention[]}
     * @private
     */
    _webmentions = [];
    /**
     * @type {boolean} Whether the last fetch was successful.
     * @private
     */
    _ok;

    constructor() {
        super();
    }

    static observedAttributes = ['endpoint', 'heading', 'hide-when-empty'];

    #escapeHtml(str) {
        if (!str) {
            return '';
        }

        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    /**
     * Sanitizes a URL to ensure it is a valid protocol relative URL.
     * @param url The URL to sanitize.
     * @return {string} The sanitized URL.
     */
    #sanitizeUrl(url) {
        try {
            const protocol = new URL(url, window.location.href).protocol;
            return (protocol === 'http:' || protocol === 'https:') ? url : '#';
        } catch (error) {
            return '#';
        }
    }

    async attributeChangedCallback(name, oldValue, newValue) {
        if (oldValue !== newValue) {
            switch (name) {
                case 'endpoint':
                    this._endpoint = newValue;
                    break;
                case 'heading':
                    this._heading = newValue;
                    break;
                case 'hide-when-empty':
                    this._hideWhenEmpty = newValue === 'true';
                    break;
                default:
                    break;
            }

            if (this.isConnected) {
                if (name === 'endpoint') {
                    await this.fetch();
                }
                this.render();
            }
        }
    }

    connectedCallback() {
        this._hideWhenEmpty = this.getAttribute('hide-when-empty') === 'true' || false;
        this._endpoint = this.getAttribute('endpoint');
        this._heading = this.getAttribute('heading') || 'Mentions';
        this.fetch().then(() => this.render());
    }

    render() {
        if (this._hideWhenEmpty && this._webmentions.length === 0) {
            this.innerHTML = '';
            return;
        }

        this.innerHTML = `
            <section class="webmention">
                <h2>${this.#escapeHtml(this._heading)}</h2>
                <ul>${this._webmentions.map(webmention => `
                    <li>
                        <a href="${this.#sanitizeUrl(webmention.target)}">${this.#escapeHtml(webmention.mentionText)}</a> (from: <a href="${this.#sanitizeUrl(webmention.source)}">${this.#escapeHtml(webmention.source)})</a>
                    </li>
                `).join('')}                
                </ul>
            </section>`;
    }

    async fetch() {
        const webmentionFetchResult =  fetchWebmentions(this._endpoint);
        const { webmentions, ok } = await webmentionFetchResult;
        this._webmentions = webmentions;
        this._ok = ok;
    }
}

if (!customElements.get('webmention-list')) {
    customElements.define('webmention-list', WebmentionList);
}