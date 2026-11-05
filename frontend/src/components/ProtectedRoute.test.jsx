import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import ProtectedRoute from "./ProtectedRoute";
import { clearSession, setToken } from "../services/auth";

function LoginLocation() {
  const location = useLocation();

  return (
    <p>
      Login destination: {location.state.from.pathname}
      {location.state.from.search}
      {location.state.from.hash}
    </p>
  );
}

function renderProtectedRoute() {
  return render(
    <MemoryRouter initialEntries={["/trips?tab=upcoming#list"]}>
      <Routes>
        <Route path="/login" element={<LoginLocation />} />
        <Route
          path="/trips"
          element={
            <ProtectedRoute>
              <p>Protected trips</p>
            </ProtectedRoute>
          }
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe("ProtectedRoute", () => {
  const validToken =
    "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0IiwiZXhwIjo0MTAyNDQ0ODAwfQ.c2lnbmF0dXJl";

  beforeEach(() => {
    clearSession();
  });

  afterEach(() => {
    cleanup();
    clearSession();
  });

  it("redirects unauthenticated users and preserves the requested location", () => {
    renderProtectedRoute();

    expect(
      screen.getByText("Login destination: /trips?tab=upcoming#list"),
    ).toBeInTheDocument();
  });

  it("renders protected content for an authenticated user", () => {
    setToken(validToken);

    renderProtectedRoute();

    expect(screen.getByText("Protected trips")).toBeInTheDocument();
  });
});
