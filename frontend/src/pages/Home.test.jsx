import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import Home from "./Home";
import { clearSession, getToken, setToken } from "../services/auth";
import { getDestinations } from "../services/destinationService";

vi.mock("../services/destinationService", () => ({
  getDestinations: vi.fn(),
}));

function LoginLocation() {
  const location = useLocation();
  return <p>Current route: {location.pathname}</p>;
}

function CurrentLocation() {
  const location = useLocation();
  return <p>Current route: {location.pathname}{location.search}</p>;
}

function renderHome(initialEntries = ["/"]) {
  return render(
    <MemoryRouter initialEntries={initialEntries}>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<LoginLocation />} />
        <Route path="/destinations" element={<CurrentLocation />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("Home authentication navigation", () => {
  const validToken =
    "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0IiwiZXhwIjo0MTAyNDQ0ODAwfQ.c2lnbmF0dXJl";

  beforeEach(() => {
    clearSession();
    getDestinations.mockResolvedValue([]);
  });

  afterEach(() => {
    cleanup();
    clearSession();
    vi.resetAllMocks();
  });

  it("shows login and signup links when signed out", () => {
    renderHome();

    expect(screen.getByRole("link", { name: "Log in" })).toHaveAttribute(
      "href",
      "/login",
    );
    expect(screen.getByRole("link", { name: "Sign up" })).toHaveAttribute(
      "href",
      "/register",
    );
    expect(
      screen.queryByRole("button", { name: "Log out" }),
    ).not.toBeInTheDocument();
  });

  it("clears the session and navigates to login when logging out", () => {
    setToken(validToken);
    renderHome();

    fireEvent.click(screen.getByRole("button", { name: "Log out" }));

    expect(getToken()).toBeNull();
    expect(screen.getByText("Current route: /login")).toBeInTheDocument();
  });

  it("loads real destinations and links to the complete destination listing", async () => {
    getDestinations.mockResolvedValue([
      {
        id: 7,
        name: "Kyoto",
        city: "Kyoto",
        country: "Japan",
        description: "Historic temples",
      },
    ]);
    renderHome();

    expect(await screen.findByRole("heading", { name: "Kyoto" }))
      .toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Destinations" }))
      .toHaveAttribute("href", "/destinations");
    expect(screen.getByRole("link", { name: "Packages" }))
      .toHaveAttribute("href", "/packages");
    expect(screen.getByRole("link", { name: /view all destinations/i }))
      .toHaveAttribute("href", "/destinations");
    expect(getDestinations).toHaveBeenCalledWith();
  });

  it("navigates to destination listings with the entered search query", () => {
    renderHome();
    fireEvent.change(screen.getByLabelText("Destination"), {
      target: { value: "Kyoto" },
    });
    fireEvent.change(screen.getByLabelText("Travel date"), {
      target: { value: "2027-05-01" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Search Trips" }));

    expect(screen.getByText("Current route: /destinations?name=Kyoto"))
      .toBeInTheDocument();
  });
});
