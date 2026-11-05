import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import Home from "./Home";
import { clearSession, getToken, setToken } from "../services/auth";

function LoginLocation() {
  const location = useLocation();
  return <p>Current route: {location.pathname}</p>;
}

function renderHome() {
  return render(
    <MemoryRouter initialEntries={["/"]}>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<LoginLocation />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("Home authentication navigation", () => {
  const validToken =
    "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0IiwiZXhwIjo0MTAyNDQ0ODAwfQ.c2lnbmF0dXJl";

  beforeEach(() => {
    clearSession();
  });

  afterEach(() => {
    cleanup();
    clearSession();
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
});
