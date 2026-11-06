import { afterEach, describe, expect, it, vi } from "vitest";
import api from "./api";
import { getDestinations } from "./destinationService";

vi.mock("./api", () => ({
  default: { get: vi.fn() },
}));

describe("destinationService", () => {
  afterEach(() => {
    vi.resetAllMocks();
  });

  it("returns the bare destination list and sends optional filters as query params", async () => {
    const destinations = [{ id: 1, name: "Kyoto", city: "Kyoto", country: "Japan" }];
    api.get.mockResolvedValue({ data: destinations });

    await expect(getDestinations({ name: "Kyoto", country: "Japan" }))
      .resolves.toEqual(destinations);

    expect(api.get).toHaveBeenCalledWith("/destinations", {
      params: { name: "Kyoto", country: "Japan" },
    });
  });

  it("rejects a response that is not a bare destination list", async () => {
    api.get.mockResolvedValue({ data: { destinations: [] } });

    await expect(getDestinations()).rejects.toThrow(
      "The server returned an invalid destination list.",
    );
  });
});
