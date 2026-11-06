import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import { getPackage } from "../services/packageService";
import PackageDetails from "./PackageDetails";

vi.mock("../services/packageService", () => ({
  getPackage: vi.fn(),
}));

const travelPackage = {
  id: 12,
  destinationId: 7,
  destinationName: "Amalfi Coast",
  title: "Coastal Escape",
  description: "Explore coastal villages and seaside trails.",
  durationDays: 6,
  basePrice: 1250,
  maxCapacity: 14,
  isActive: true,
};

function CurrentLocation() {
  const location = useLocation();
  return <p>Current route: {location.pathname}</p>;
}

function renderPackageDetails(initialEntry = "/packages/12") {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route path="/packages/:id" element={<PackageDetails />} />
        <Route path="/packages" element={<CurrentLocation />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("PackageDetails", () => {
  afterEach(() => {
    cleanup();
    vi.resetAllMocks();
  });

  it("shows loading state while package details are fetched", () => {
    getPackage.mockReturnValue(new Promise(() => {}));
    renderPackageDetails();

    expect(screen.getByRole("status")).toHaveTextContent(
      "Loading package details...",
    );
    expect(getPackage).toHaveBeenCalledWith("12");
  });

  it("displays package details, capacity, and back navigation", async () => {
    getPackage.mockResolvedValue(travelPackage);
    renderPackageDetails();

    expect(await screen.findByRole("heading", { name: "Coastal Escape" }))
      .toBeInTheDocument();
    expect(screen.getByText("Amalfi Coast")).toBeInTheDocument();
    expect(screen.getByText("Explore coastal villages and seaside trails."))
      .toBeInTheDocument();
    expect(screen.getByText("6 days")).toBeInTheDocument();
    expect(screen.getByText("$1,250.00")).toBeInTheDocument();
    expect(screen.getByText("14")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("link", { name: "Back to Packages" }));
    expect(screen.getByText("Current route: /packages")).toBeInTheDocument();
  });

  it("shows a not-found state for a missing package without retry", async () => {
    getPackage.mockRejectedValue({
      response: { status: 404, data: { message: "Package not found: 99" } },
    });
    renderPackageDetails("/packages/99");

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Package not found.",
    );
    expect(screen.queryByRole("button", { name: "Retry" }))
      .not.toBeInTheDocument();
  });

  it("shows an API error and retries the request", async () => {
    getPackage
      .mockRejectedValueOnce(new Error("Service unavailable"))
      .mockResolvedValueOnce(travelPackage);
    renderPackageDetails();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Service unavailable",
    );
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));

    expect(await screen.findByRole("heading", { name: "Coastal Escape" }))
      .toBeInTheDocument();
    expect(getPackage).toHaveBeenCalledTimes(2);
  });
});
