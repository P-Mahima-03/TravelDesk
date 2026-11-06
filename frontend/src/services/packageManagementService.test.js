import { afterEach, describe, expect, it, vi } from "vitest";
import api from "./api";
import {
  createPackage,
  deletePackage,
  getCurrentUser,
  updatePackage,
} from "./packageService";

vi.mock("./api", () => ({
  default: {
    delete: vi.fn(),
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}));

describe("package management service methods", () => {
  afterEach(() => {
    vi.resetAllMocks();
  });

  it("creates a package through the shared API client", async () => {
    const body = { title: "Coastal Escape" };
    api.post.mockResolvedValue({ data: { id: 8, ...body } });

    await expect(createPackage(body)).resolves.toEqual({ id: 8, ...body });
    expect(api.post).toHaveBeenCalledWith("/packages", body);
  });

  it("updates a package through the shared API client", async () => {
    const body = { title: "Updated Escape" };
    api.put.mockResolvedValue({ data: { id: 8, ...body } });

    await expect(updatePackage(8, body)).resolves.toEqual({ id: 8, ...body });
    expect(api.put).toHaveBeenCalledWith("/packages/8", body);
  });

  it("deletes a package through the shared API client", async () => {
    api.delete.mockResolvedValue({});

    await expect(deletePackage(8)).resolves.toBeUndefined();
    expect(api.delete).toHaveBeenCalledWith("/packages/8");
  });

  it("fetches the signed-in user for role authorization", async () => {
    api.get.mockResolvedValue({ data: { role: "TRAVEL_AGENT" } });

    await expect(getCurrentUser()).resolves.toEqual({ role: "TRAVEL_AGENT" });
    expect(api.get).toHaveBeenCalledWith("/users/me");
  });
});
