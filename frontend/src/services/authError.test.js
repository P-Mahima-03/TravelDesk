import { describe, expect, it } from "vitest";
import { getAuthErrorMessage } from "./authError";

describe("getAuthErrorMessage", () => {
  it("explains network failures without a response", () => {
    expect(getAuthErrorMessage(new Error("Network Error"))).toContain(
      "Unable to reach the server",
    );
  });

  it("uses the authentication failure response message", () => {
    expect(
      getAuthErrorMessage({
        response: {
          status: 401,
          data: { message: "Invalid email or password" },
        },
      }),
    ).toBe("Invalid email or password");
  });

  it("shows field validation errors instead of a generic validation message", () => {
    expect(
      getAuthErrorMessage({
        response: {
          status: 400,
          data: {
            message: "Validation failed",
            errors: ["Email is required", "Password is required"],
          },
        },
      }),
    ).toBe("Email is required Password is required");
  });

  it("uses the backend error message for other HTTP errors", () => {
    expect(
      getAuthErrorMessage({
        response: {
          status: 503,
          data: { message: "Authentication service unavailable" },
        },
      }),
    ).toBe("Authentication service unavailable");
  });
});
