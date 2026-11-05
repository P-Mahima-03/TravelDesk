import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  clearSession,
  getToken,
  isAuthenticated,
  isValidToken,
  logout,
  setToken,
} from "./auth";

const validToken =
  "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0IiwiZXhwIjo0MTAyNDQ0ODAwfQ.c2lnbmF0dXJl";

describe("authentication utilities", () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("accepts only structurally valid compact JWT values", () => {
    expect(isValidToken(validToken)).toBe(true);
    expect(isValidToken(`  ${validToken}  `)).toBe(true);
    expect(isValidToken("not-a-jwt")).toBe(false);
    expect(isValidToken("header..signature")).toBe(false);
    expect(
      isValidToken(
        "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0IiwiZXhwIjoxfQ.c2lnbmF0dXJl",
      ),
    ).toBe(false);
    expect(isValidToken("eyJhbGciOiJub25lIn0.eyJleHAiOjQxMDI0NDQ4MDB9.c2lnbmF0dXJl")).toBe(false);
    expect(isValidToken(null)).toBe(false);
  });

  it("retrieves a stored token and reports authentication status", () => {
    expect(getToken()).toBeNull();
    expect(isAuthenticated()).toBe(false);

    expect(setToken(validToken)).toBe(true);
    expect(getToken()).toBe(validToken);
    expect(isAuthenticated()).toBe(true);
  });

  it("does not store malformed tokens", () => {
    expect(setToken("malformed-token")).toBe(false);
    expect(getToken()).toBeNull();
  });

  it("clears the session", () => {
    setToken(validToken);

    clearSession();

    expect(getToken()).toBeNull();
    expect(isAuthenticated()).toBe(false);
  });

  it("handles unavailable localStorage without throwing", () => {
    vi.spyOn(Storage.prototype, "getItem").mockImplementation(() => {
      throw new Error("Storage is unavailable");
    });
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new Error("Storage is unavailable");
    });
    vi.spyOn(Storage.prototype, "removeItem").mockImplementation(() => {
      throw new Error("Storage is unavailable");
    });

    expect(getToken()).toBeNull();
    expect(isAuthenticated()).toBe(false);
    expect(setToken(validToken)).toBe(false);
    expect(() => clearSession()).not.toThrow();
  });

  it("logs out by clearing the token and navigating to login", () => {
    const navigate = vi.fn();
    setToken(validToken);

    logout(navigate);

    expect(getToken()).toBeNull();
    expect(navigate).toHaveBeenCalledWith("/login", { replace: true });
  });
});
