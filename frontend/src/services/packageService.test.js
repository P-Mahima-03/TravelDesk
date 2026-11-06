import { afterEach, describe, expect, it, vi } from "vitest";
import api from "./api";
import { getPackages } from "./packageService";

vi.mock("./api", () => ({
  default: { get: vi.fn() },
}));

describe("packageService", () => {
  afterEach(() => {
    vi.resetAllMocks();
  });

  it("returns the bare package list and sends filters as query parameters", async () => {
    const packages = [{ id: 1, title: "Temple Walk" }];
    const filters = {
      title: "Temple",
      destinationId: "4",
      minPrice: "100",
      maxPrice: "500",
      maxDurationDays: "7",
    };
    api.get.mockResolvedValue({ data: packages });

    await expect(getPackages(filters)).resolves.toEqual(packages);

    expect(api.get).toHaveBeenCalledWith("/packages", { params: filters });
  });

  it("rejects a response that is not a bare package list", async () => {
    api.get.mockResolvedValue({ data: { packages: [] } });

    await expect(getPackages()).rejects.toThrow(
      "The server returned an invalid package list.",
    );
  });
});
