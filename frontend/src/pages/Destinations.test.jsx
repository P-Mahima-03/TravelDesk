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
import Destinations from "./Destinations";

vi.mock("../services/destinationService", () => ({
  getDestinations: vi.fn(),
}));

function renderDestinations(initialEntry = "/destinations") {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route path="/destinations" element={<Destinations />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("Destinations", () => {
  beforeEach(() => {
    getDestinations.mockResolvedValue([]);
  });

  afterEach(() => {
    cleanup();
    vi.resetAllMocks();
  });

  it("shows a loading state while the destination request is pending", () => {
    getDestinations.mockReturnValue(new Promise(() => {}));
    renderDestinations();

    expect(screen.getByRole("status")).toHaveTextContent(
      "Loading destinations...",
    );
  });

  it("renders destination details and a placeholder when no image is provided", async () => {
    getDestinations.mockResolvedValue([
      {
        id: 1,
        name: "Kyoto",
        city: "Kyoto",
        country: "Japan",
        description: "Historic temples and gardens.",
      },
      {
        id: 2,
        name: "Reykjavik",
        city: "Reykjavik",
        country: "Iceland",
        imageUrl: "https://example.com/reykjavik.jpg",
      },
    ]);
    renderDestinations();

    expect(await screen.findByRole("heading", { name: "Kyoto" })).toBeInTheDocument();
    expect(screen.getByText("Kyoto, Japan")).toBeInTheDocument();
    expect(screen.getByText("Historic temples and gardens.")).toBeInTheDocument();
    expect(screen.getByRole("img", { name: "No destination image available" }))
      .toBeInTheDocument();
    expect(screen.getByRole("img", { name: "Reykjavik" }))
      .toHaveAttribute("src", "https://example.com/reykjavik.jpg");
  });

  it("shows an empty state when the API returns no destinations", async () => {
    renderDestinations();

    expect(await screen.findByText("No destinations match your search."))
      .toBeInTheDocument();
  });

  it("shows an API error state", async () => {
    getDestinations.mockRejectedValue(new Error("Service unavailable"));
    renderDestinations();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Service unavailable",
    );
  });

  it("loads URL filters and applies changed name, city, and country filters", async () => {
    renderDestinations("/destinations?name=Kyoto&country=Japan");

    await waitFor(() => {
      expect(getDestinations).toHaveBeenCalledWith({
        name: "Kyoto",
        country: "Japan",
      });
    });
    expect(screen.getByLabelText("Name")).toHaveValue("Kyoto");

    fireEvent.change(screen.getByLabelText("Name"), {
      target: { value: "Osaka" },
    });
    fireEvent.change(screen.getByLabelText("City"), {
      target: { value: "Namba" },
    });
    fireEvent.change(screen.getByLabelText("Country"), {
      target: { value: "Japan" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Apply filters" }));

    await waitFor(() => {
      expect(getDestinations).toHaveBeenLastCalledWith({
        name: "Osaka",
        city: "Namba",
        country: "Japan",
      });
    });
  });

  it("clears filter values and reloads the full destination list", async () => {
    renderDestinations("/destinations?city=Kyoto");
    await screen.findByText("No destinations match your search.");

    fireEvent.click(screen.getByRole("button", { name: "Clear filters" }));

    await waitFor(() => {
      expect(getDestinations).toHaveBeenLastCalledWith({});
    });
    expect(screen.getByLabelText("City")).toHaveValue("");
  });
});
