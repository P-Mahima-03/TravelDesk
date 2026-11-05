import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter } from "react-router-dom";
import api from "../services/api";
import Register from "./Register";

vi.mock("../services/api", () => ({
  default: { post: vi.fn() },
}));

function renderRegister() {
  return render(
    <MemoryRouter>
      <Register />
    </MemoryRouter>,
  );
}

function fillRegistrationForm() {
  fireEvent.change(screen.getByLabelText(/first name/i), {
    target: { value: "  Alex " },
  });
  fireEvent.change(screen.getByLabelText(/last name/i), {
    target: { value: "  Traveler " },
  });
  fireEvent.change(screen.getByLabelText(/email address/i), {
    target: { value: "  alex@example.com " },
  });
  fireEvent.change(screen.getByLabelText(/phone number/i), {
    target: { value: "  555-0100 " },
  });
  fireEvent.change(screen.getByLabelText(/^password$/i), {
    target: { value: "travel-password" },
  });
}

describe("Register", () => {
  afterEach(() => {
    cleanup();
    vi.resetAllMocks();
  });

  it("submits the backend registration fields and confirms creation", async () => {
    api.post.mockResolvedValue({
      data: { id: 1, email: "alex@example.com", role: "CUSTOMER" },
    });
    renderRegister();
    fillRegistrationForm();

    fireEvent.click(screen.getByRole("button", { name: "Create Account" }));

    expect(api.post).toHaveBeenCalledWith("/auth/register", {
      firstName: "Alex",
      lastName: "Traveler",
      email: "alex@example.com",
      password: "travel-password",
      phone: "555-0100",
    });
    expect(await screen.findByRole("status")).toHaveTextContent(
      "Your account has been created. You can now sign in.",
    );
  });

  it("shows a server-provided registration error", async () => {
    api.post.mockRejectedValue({
      response: {
        status: 409,
        data: { message: "An account with this email already exists" },
      },
    });
    renderRegister();
    fillRegistrationForm();

    fireEvent.click(screen.getByRole("button", { name: "Create Account" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "An account with this email already exists",
    );
    expect(
      screen.getByRole("button", { name: "Create Account" }),
    ).toBeEnabled();
  });

  it("disables the form while registration is pending", async () => {
    let resolveRequest;
    api.post.mockReturnValue(
      new Promise((resolve) => {
        resolveRequest = resolve;
      }),
    );
    renderRegister();
    fillRegistrationForm();

    fireEvent.click(screen.getByRole("button", { name: "Create Account" }));

    expect(
      screen.getByRole("button", { name: "Creating Account..." }),
    ).toBeDisabled();
    resolveRequest({
      data: { id: 1, email: "alex@example.com", role: "CUSTOMER" },
    });
    await waitFor(() => {
      expect(screen.getByRole("status")).toHaveTextContent(
        "Your account has been created. You can now sign in.",
      );
    });
  });

  it("shows a useful message when the server cannot be reached", async () => {
    api.post.mockRejectedValue(new Error("Network Error"));
    renderRegister();
    fillRegistrationForm();

    fireEvent.click(screen.getByRole("button", { name: "Create Account" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Unable to reach the server",
    );
  });
});
