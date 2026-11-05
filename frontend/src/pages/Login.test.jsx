import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter } from "react-router-dom";
import api from "../services/api";
import Login from "./Login";

vi.mock("../services/api", () => ({
  default: { post: vi.fn() },
}));

function renderLogin() {
  return render(
    <MemoryRouter>
      <Login />
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

describe("Login", () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  afterEach(() => {
    cleanup();
    vi.resetAllMocks();
  });

  it("submits credentials, stores a returned token, and shows success", async () => {
    api.post.mockResolvedValue({
      data: { message: "Login successful", token: "jwt-token" },
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
    expect(await screen.findByRole("status")).toHaveTextContent(
      "Signed in successfully.",
    );
    expect(window.localStorage.getItem("token")).toBe("jwt-token");
  });

  it("trims a valid token before storing it", async () => {
    api.post.mockResolvedValue({
      data: { message: "Login successful", token: "  jwt-token  " },
    });
    renderLogin();
    fillLoginForm();

    fireEvent.click(screen.getByRole("button", { name: "Sign In" }));

    expect(await screen.findByRole("status")).toHaveTextContent(
      "Signed in successfully.",
    );
    expect(window.localStorage.getItem("token")).toBe("jwt-token");
  });

  it.each([null, "", "   ", 42, {}])(
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
      data: { message: "Login successful", token: "jwt-token" },
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
    resolveRequest({ data: { token: "jwt-token" } });
    await screen.findByRole("status");
  });
});
