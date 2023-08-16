export function getServerUrl() {
    // this lets me hardcode the api url if I'm working outside the container
    if (process.env.NEXT_PUBLIC_API_URL) {
        return process.env.NEXT_PUBLIC_API_URL;
    } else {
        return "http://localhost:3000"
    }
}

export const fetcher = (...args) => fetch(...args).then(res => res.json())
