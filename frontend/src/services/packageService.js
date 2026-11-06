import api from "./api";

export async function getPackages(filters = {}) {
  const response = await api.get("/packages", { params: filters });

  if (!Array.isArray(response.data)) {
    throw new Error("The server returned an invalid package list.");
  }

  return response.data;
}

export async function getPackage(id) {
  const response = await api.get(`/packages/${encodeURIComponent(id)}`);

  if (!response.data || typeof response.data !== "object" || Array.isArray(response.data)) {
    throw new Error("The server returned an invalid package.");
  }

  return response.data;
}
