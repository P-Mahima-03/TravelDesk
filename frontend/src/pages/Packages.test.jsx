import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { getDestinations } from "../services/destinationService";
import { getPackages } from "../services/packageService";
import Packages from "./Packages";

vi.mock("../services/destinationService", () => ({
  getDestinations: vi.fn(),
}));

vi.mock("../services/packageService", () => ({
  getPackages: vi.fn(),
}));

function renderPackages(initialEntry = "/packages") {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route path="/packages" element={<Packages />} />
      </Routes>
    </MemoryRouter>,
  );
}

const samplePackage = {
  id: 11,
  destinationId: 5,
  destinationName: "Kyoto",
  title: "Temple Walk",
  description: "A guided tour of historic temples.",
  durationDays: 5,
  basePrice: 250,
  maxCapacity: 12,
  isActive: true,
};

describe("Packages", () => {
  beforeEach(() => {
    getDestinations.mockResolvedValue([
      { id: 5, name: "Kyoto" },
      { id: 6, name: "Osaka" },
    ]);
    getPackages.mockResolvedValue([]);
  });

  afterEach(() => {
    cleanup();
    vi.resetAllMocks();
  });

  it("shows loading state while packages are being fetched", () => {
    getPackages.mockReturnValue(new Promise(() => {}));
    renderPackages();

    expect(screen.getByRole("status")).toHaveTextContent("Loading packages...");
  });

  it("loads and displays package details", async () => {
    getPackages.mockResolvedValue([samplePackage]);
    renderPackages();

    expect(await screen.findByRole("heading", { name: "Temple Walk" }))
      .toBeInTheDocument();
    expect(screen.getByText("Kyoto", { selector: ".listing-package-destination" }))
      .toBeInTheDocument();
    expect(screen.getByText("5 days")).toBeInTheDocument();
    expect(screen.getByText("$250.00")).toBeInTheDocument();
    expect(screen.getByText("A guided tour of historic temples."))
      .toBeInTheDocument();
  });

  it("shows an empty state when no packages match", async () => {
    renderPackages();

    expect(await screen.findByText("No packages match your search."))
      .toBeInTheDocument();
  });

  it("shows an error and retries the request", async () => {
    getPackages
      .mockRejectedValueOnce(new Error("Service unavailable"))
      .mockResolvedValueOnce([samplePackage]);
    renderPackages();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Service unavailable",
    );
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));

    expect(await screen.findByRole("heading", { name: "Temple Walk" }))
      .toBeInTheDocument();
    expect(getPackages).toHaveBeenCalledTimes(2);
  });

  it("loads URL filters and applies all package filters", async () => {
    renderPackages(
      "/packages?title=Temple&destinationId=5&minPrice=100&maxPrice=500&maxDurationDays=7",
    );

    await waitFor(() => {
      expect(getPackages).toHaveBeenCalledWith({
        title: "Temple",
        destinationId: "5",
        minPrice: "100",
        maxPrice: "500",
        maxDurationDays: "7",
      });
    });
    expect(screen.getByLabelText("Destination")).toHaveValue("5");

    fireEvent.change(screen.getByLabelText("Title"), {
      target: { value: "Walking" },
    });
    fireEvent.change(screen.getByLabelText("Destination"), {
      target: { value: "6" },
    });
    fireEvent.change(screen.getByLabelText("Minimum price"), {
      target: { value: "50" },
    });
    fireEvent.change(screen.getByLabelText("Maximum price"), {
      target: { value: "700" },
    });
    fireEvent.change(screen.getByLabelText("Maximum duration (days)"), {
      target: { value: "9" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Apply filters" }));

    await waitFor(() => {
      expect(getPackages).toHaveBeenLastCalledWith({
        title: "Walking",
        destinationId: "6",
        minPrice: "50",
        maxPrice: "700",
        maxDurationDays: "9",
      });
    });
  });

  it("clears package filters", async () => {
    renderPackages("/packages?title=Temple");
    await screen.findByText("No packages match your search.");

    fireEvent.click(screen.getByRole("button", { name: "Clear filters" }));

    await waitFor(() => {
      expect(getPackages).toHaveBeenLastCalledWith({});
    });
  });
});
