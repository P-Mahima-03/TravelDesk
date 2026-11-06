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

export async function createPackage(packageData) {
  const response = await api.post("/packages", packageData);
  return response.data;
}

export async function updatePackage(id, packageData) {
  const response = await api.put(`/packages/${encodeURIComponent(id)}`, packageData);
  return response.data;
}

export async function deletePackage(id) {
  await api.delete(`/packages/${encodeURIComponent(id)}`);
}

export async function getCurrentUser() {
  const response = await api.get("/users/me");
  return response.data;
}
