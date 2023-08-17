export function getFrontendServerUrl() {
    let frontendurl = "http://localhost:3000"
    // this lets me hardcode the api url if I'm working outside the container
    if (process.env.NEXT_PUBLIC_API_URL) {
        frontendurl = process.env.NEXT_PUBLIC_API_URL;
    }
    return frontendurl
}
export function getBackendServerUrl() {
    // this lets me hardcode the api url if I'm working outside the container
    let backendurl = "http://localhost:8201"
    if (process.env.OSCARS_BACKEND_URL) {
        backendurl = process.env.OSCARS_BACKEND_URL;
    }
    return backendurl;
}
export const fetcher = (...args) => fetch(...args).then(res => res.json())
