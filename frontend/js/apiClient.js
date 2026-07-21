const tokenKey = "calculationTool.jwt";
const baseUrlKey = "calculationTool.apiBaseUrl";

export function getStoredToken() {
    return localStorage.getItem(tokenKey);
}

export function setStoredToken(token) {
    localStorage.setItem(tokenKey, token);
}

export function clearStoredToken() {
    localStorage.removeItem(tokenKey);
}

export function getApiBaseUrl() {
    return localStorage.getItem(baseUrlKey) || window.location.origin;
}

export function setApiBaseUrl(baseUrl) {
    localStorage.setItem(baseUrlKey, baseUrl.replace(/\/$/, ""));
}

export async function apiRequest(path, options = {}) {
    const headers = new Headers(options.headers || {});
    headers.set("Accept", "application/json");

    if (options.body && !headers.has("Content-Type")) {
        headers.set("Content-Type", "application/json");
    }

    const token = getStoredToken();
    if (token) {
        headers.set("Authorization", `Bearer ${token}`);
    }

    let response;
    try {
        response = await fetch(`${getApiBaseUrl()}${path}`, {
            ...options,
            headers
        });
    } catch (error) {
        throw new Error("Could not reach the API. Check that the backend is running.");
    }

    if (!response.ok) {
        const error = await response.json().catch(() => ({ message: response.statusText }));
        if (response.status === 401) {
            throw new Error("Your session has expired. Please log in again.");
        }
        if (response.status === 403) {
            throw new Error(error.message || "You do not have access to perform this action.");
        }
        throw new Error(error.message || `Request failed with status ${response.status}`);
    }

    if (response.status === 204) {
        return null;
    }

    return response.json();
}
