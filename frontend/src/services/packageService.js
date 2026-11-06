import api from "./api";

export async function getPackages(filters = {}) {
  const response = await api.get("/packages", { params: filters });

  if (!Array.isArray(response.data)) {
    throw new Error("The server returned an invalid package list.");
  }

  return response.data;
}
