import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import api from "../services/api";
import ProtectedRoute from "../components/ProtectedRoute";
import Login from "./Login";

const validToken =
  "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0IiwiZXhwIjo0MTAyNDQ0ODAwfQ.c2lnbmF0dXJl";

vi.mock("../services/api", () => ({
  default: { post: vi.fn() },
}));

function renderLogin() {
  return render(
    <MemoryRouter initialEntries={["/login"]}>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/" element={<p>Home page</p>} />
      </Routes>
    </MemoryRouter>,
  );
}

function fillLoginForm() {
  fireEvent.change(screen.getByLabelText(/email address/i), {
    target: { value: "traveler@example.com" },
  });
  fireEvent.change(screen.getByLabelText(/password/i), {
    target: { value: "correct-horse-battery" },
  });
}

function TripLocation() {
  const location = useLocation();

  return <p>Upcoming trips{location.search}{location.hash}</p>;
}

describe("Login", () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  afterEach(() => {
    cleanup();
    vi.resetAllMocks();
  });

  it("submits credentials, stores a returned token, and navigates home", async () => {
    api.post.mockResolvedValue({
      data: { message: "Login successful", token: validToken },
    });
    renderLogin();
    fillLoginForm();

    fireEvent.click(screen.getByRole("button", { name: "Sign In" }));

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith("/auth/login", {
        email: "traveler@example.com",
        password: "correct-horse-battery",
      });
    });
    expect(await screen.findByText("Home page")).toBeInTheDocument();
    expect(window.localStorage.getItem("token")).toBe(
      validToken,
    );
  });

  it("trims a valid token before storing it", async () => {
    api.post.mockResolvedValue({
      data: {
        message: "Login successful",
        token: `  ${validToken}  `,
      },
    });
    renderLogin();
    fillLoginForm();

    fireEvent.click(screen.getByRole("button", { name: "Sign In" }));

    expect(await screen.findByText("Home page")).toBeInTheDocument();
    expect(window.localStorage.getItem("token")).toBe(
      validToken,
    );
  });

  it.each([null, "", "   ", "not-a-jwt", 42, {}])(
    "does not store an invalid token (%j)",
    async (token) => {
      api.post.mockResolvedValue({
        data: { message: "Login successful", token },
      });
      renderLogin();
      fillLoginForm();

      fireEvent.click(screen.getByRole("button", { name: "Sign In" }));

      expect(await screen.findByRole("alert")).toHaveTextContent(
        "did not return a valid authentication token",
      );
      expect(window.localStorage.getItem("token")).toBeNull();
    },
  );

  it("reports when browser storage is unavailable", async () => {
    api.post.mockResolvedValue({
      data: {
        message: "Login successful",
        token: validToken,
      },
    });
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new Error("Storage is unavailable");
    });
    renderLogin();
    fillLoginForm();

    fireEvent.click(screen.getByRole("button", { name: "Sign In" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "could not save your session",
    );
  });

  it("shows a server error and restores the submit button", async () => {
    api.post.mockRejectedValue({
      response: { status: 401, data: { message: "Invalid email or password" } },
    });
    renderLogin();
    fillLoginForm();

    fireEvent.click(screen.getByRole("button", { name: "Sign In" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Invalid email or password",
    );
    expect(screen.getByRole("button", { name: "Sign In" })).toBeEnabled();
    expect(window.localStorage.getItem("token")).toBeNull();
  });

  it("shows backend field errors returned by the login API", async () => {
    api.post.mockRejectedValue({
      response: {
        status: 400,
        data: {
          message: "Validation failed",
          errors: ["Email is required", "Password is required"],
        },
      },
    });
    renderLogin();
    fillLoginForm();

    fireEvent.click(screen.getByRole("button", { name: "Sign In" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Email is required Password is required",
    );
  });

  it("shows a clear message when the login API cannot be reached", async () => {
    api.post.mockRejectedValue(new Error("Network Error"));
    renderLogin();
    fillLoginForm();

    fireEvent.click(screen.getByRole("button", { name: "Sign In" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Unable to reach the server",
    );
  });

  it("disables the form while the request is pending", async () => {
    let resolveRequest;
    api.post.mockReturnValue(
      new Promise((resolve) => {
        resolveRequest = resolve;
      }),
    );
    renderLogin();
    fillLoginForm();

    fireEvent.click(screen.getByRole("button", { name: "Sign In" }));

    expect(
      screen.getByRole("button", { name: "Signing In..." }),
    ).toBeDisabled();
    resolveRequest({ data: { token: validToken } });
    await screen.findByText("Home page");
  });

  it("returns to the originally requested protected route after login", async () => {
    api.post.mockResolvedValue({
      data: { message: "Login successful", token: validToken },
    });

    render(
      <MemoryRouter initialEntries={["/trips?tab=upcoming#list"]}>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route
            path="/trips"
            element={
              <ProtectedRoute>
                <TripLocation />
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>,
    );

    fillLoginForm();
    fireEvent.click(screen.getByRole("button", { name: "Sign In" }));

    expect(
      await screen.findByText("Upcoming trips?tab=upcoming#list"),
    ).toBeInTheDocument();
  });
});
