/**
 * @typedef {object} Webmention
 * @property {string} target The target URL of the webmention.
 * @property {string} source The source URL of the webmention.
 * @property {string} mentionText The text of the webmention.
 */

/**
 * @typedef {object} WebmentionFetchResult
 * @property {Webmention[]} webmentions The webmentions fetched from the endpoint.
 * @property {boolean} ok Whether the last fetch was successful.
 */

/**
 * Fetches webmentions from the specified endpoint.
 * @param url The URL to fetch webmentions from.
 * @return {Promise<WebmentionFetchResult>}
 */
export const fetchWebmentions = async (url) => {
    /** @type {WebmentionFetchResult} */
    let webmentionFetchResult = {webmentions: [], ok: false};

    if (url) {
        try {
            const response = await fetch(url);
            if (response.ok) {
                const webmentions = await response.json();
                webmentionFetchResult = {webmentions, ok: true};
            } else {
                console.error(`An error occurred while attempting to fetch webmentions from ${url}. Status code: ${response.status}.`);
            }
        } catch (error) {
            console.error(`An error occurred while attempting to fetch webmentions from ${url}.`, error);
        }
    } else {
        console.error('No endpoint specified Ensure that the endpoint attribute is set with a value pointing to the appropriate endpoint for fetching webmention entries.');
    }

    return webmentionFetchResult;
}