import api from "./api";

export async function getDestinations(filters = {}) {
  const response = await api.get("/destinations", { params: filters });

  if (!Array.isArray(response.data)) {
    throw new Error("The server returned an invalid destination list.");
  }

  return response.data;
}
