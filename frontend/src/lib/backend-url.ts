const backendBaseUrl = import.meta.env.VITE_BACKEND_URL ?? 'http://localhost:8080'

export function backendUrl(path: string) {
  return new URL(path, backendBaseUrl).toString()
}
