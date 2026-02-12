export class WebmentionEvent extends Event {
    /**
     * @type {'success'|'failure'} The result of the webmention submission. Either "success" or "failure".
     */
    #result
    /**
     * @type {number} The HTTP status code of the webmention submission. Only set if the result is "failure".
     */
    #httpStatusCode;
    /**
     * @type {string} The HTTP status text of the webmention submission. Only set if the result is "failure".
     */
    #httpStatusText;

    /**
     * Creates a new WebmentionEvent.
     * @param {'success'|'failure'} result
     * @param {number|undefined} httpStatusCode
     * @param {string|undefined} httpStatusText
     * @throws {Error} If the result is not "success" or "failure", or if httpStatusCode and httpStatusText are not set when the result is "failure".
     */
    constructor(result, httpStatusCode= undefined, httpStatusText= undefined) {
        super("webmention-sent", {
            bubbles: true,
            composed: true,
        });
        this.#result = result;

        if (result === 'success') {
            if (httpStatusCode !== undefined || httpStatusText !== undefined) {
                throw new Error('Cannot set httpStatusCode or httpStatusText when result is "success"');
            }
        } else if (result === 'failure') {
            this.#httpStatusCode = httpStatusCode;
            this.#httpStatusText = httpStatusText;
        } else {
            throw new Error(`Invalid result: ${result}`);
        }
    }

    get result() {
        return this.#result;
    }

    get httpStatusCode() {
        return this.#httpStatusCode;
    }

    get httpStatusText() {
        return this.#httpStatusText;
    }
}

export class WebmentionSend extends HTMLElement {
    /**
     * @type {string} The webmention endpoint to send webmentions to.
     * @private
     */
    _endpoint;
    _descriptiveText;

    constructor() {
        super();
    }

    static get observedAttributes() {
        return ['target'];
    }

    attributeChangedCallback(name, oldValue, newValue) {
        if (oldValue !== newValue) {
            switch (name) {
                case 'endpoint':
                    this._endpoint = newValue;
                    break;
                default:
                    throw new Error(`Unknown attribute: ${name}`);
            }
            this.render();
        }
    }

    connectedCallback() {
        this._endpoint = this.getAttribute('endpoint');
        this._descriptiveText = this.innerHTML;

        this.render();
    }

    render() {
        this.innerHTML = `
        <form method="POST" action="${this._endpoint}">
            ${this._descriptiveText}
            <input type="hidden" name="target" value="${window.location.href}">
            <label for="source">URL:</label>
            <input type="url" name="source" id="source" required>
            <button type="submit">Send Webmention</button>
        </form>
        `;

        this.querySelector('form').addEventListener('submit', async (event) => {
            event.preventDefault();

            event.target.querySelector('button').disabled = true;

            try {
                const response = await fetch(event.target.action, {
                    method: event.target.method,
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                    body: new URLSearchParams(new FormData(event.target)).toString(),
                });

                if (response.ok) {
                    this.dispatchEvent(new WebmentionEvent('success'));
                } else {
                    this.dispatchEvent(new WebmentionEvent('failure', response.status, response.statusText));
                }
            }
            catch (error) {
                this.dispatchEvent(new WebmentionEvent('failure', error.status, error.statusText));
            }

            event.target.querySelector('button').disabled = false;
        });
    }
}

customElements.define('webmention-send', WebmentionSend);