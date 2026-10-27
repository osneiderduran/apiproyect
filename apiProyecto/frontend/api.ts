// Cliente para usar la API desde otra app (React, etc.)
const BASE = "http://localhost:8080/api";

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(BASE + path, { headers: { "Content-Type": "application/json" }, ...options });
  if (!res.ok) throw new Error((await res.json().catch(() => ({}))).message || res.statusText);
  return res.json();
}

export const api = {
  register: (nombre: string, email: string, password: string) =>
    request("/users/register", { method: "POST", body: JSON.stringify({ nombre, email, password }) }),
  login: (email: string, password: string) =>
    request("/users/login", { method: "POST", body: JSON.stringify({ email, password }) }),
  getUser: (id: string) => request(`/users/${id}`),
  getRetos: (categoria?: string) => request(`/retos${categoria ? "?categoria=" + encodeURIComponent(categoria) : ""}`),
  completarReto: (userId: string, retoId: string) =>
    request(`/users/${userId}/completar/${retoId}`, { method: "POST" }),
};
