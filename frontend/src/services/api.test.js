import axios from "axios";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import api from "./api";
import { clearSession, getToken, setToken, logout } from "./auth";

vi.mock("./auth", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    logout: vi.fn((navigate) => {
      actual.clearSession();
      if (navigate) {
        navigate("/login", { replace: true });
      }
    }),
  };
});

const validToken =
  "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0IiwiZXhwIjo0MTAyNDQ0ODAwfQ.c2lnbmF0dXJl";

function responseAdapter(status = 200) {
  return async (config) => {
    const response = {
      data: {},
      status,
      statusText: status === 401 ? "Unauthorized" : "OK",
      headers: {},
      config,
    };

    if (status >= 400) {
      throw new axios.AxiosError(
        "Request failed",
        "ERR_BAD_REQUEST",
        config,
        undefined,
        response,
      );
    }

    return response;
  };
}

describe("Axios authentication interceptors", () => {
  beforeEach(() => {
    window.localStorage.clear();
    logout.mockClear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("adds a Bearer token when one is stored", async () => {
    setToken(validToken);
    let requestConfig;

    await api.get("/private", {
      adapter: async (config) => {
        requestConfig = config;
        return responseAdapter()(config);
      },
    });

    expect(requestConfig.headers.get("Authorization")).toBe(
      `Bearer ${validToken}`,
    );
  });

  it("does not add an Authorization header when no token exists", async () => {
    let requestConfig;

    await api.get("/public", {
      adapter: async (config) => {
        requestConfig = config;
        return responseAdapter()(config);
      },
    });

    expect(requestConfig.headers.has("Authorization")).toBe(false);
  });

  it("preserves an explicitly supplied Authorization header", async () => {
    setToken(validToken);
    let requestConfig;

    await api.get("/private", {
      headers: { Authorization: "Bearer caller-token" },
      adapter: async (config) => {
        requestConfig = config;
        return responseAdapter()(config);
      },
    });

    expect(requestConfig.headers.get("Authorization")).toBe(
      "Bearer caller-token",
    );
  });

  it("clears the session and logs out on a 401 for a protected request", async () => {
    setToken(validToken);

    await expect(
      api.get("/private", { adapter: responseAdapter(401) }),
    ).rejects.toMatchObject({ response: { status: 401 } });

    expect(logout).toHaveBeenCalledOnce();
    expect(getToken()).toBeNull();
  });

  it("clears an invalid session but avoids redirecting auth endpoint failures", async () => {
    setToken(validToken);

    await expect(
      api.post("/auth/login", {}, { adapter: responseAdapter(401) }),
    ).rejects.toMatchObject({ response: { status: 401 } });

    expect(logout).not.toHaveBeenCalled();
    expect(getToken()).toBeNull();
  });

  it("does not clear the session or redirect for other errors", async () => {
    setToken(validToken);

    await expect(
      api.get("/private", { adapter: responseAdapter(500) }),
    ).rejects.toMatchObject({ response: { status: 500 } });

    expect(logout).not.toHaveBeenCalled();
    expect(getToken()).toBe(validToken);
  });

  it("does not redirect for network errors", async () => {
    setToken(validToken);
    const adapter = async () => {
      throw new axios.AxiosError("Network Error", "ERR_NETWORK");
    };

    await expect(api.get("/private", { adapter })).rejects.toMatchObject({
      code: "ERR_NETWORK",
    });

    expect(logout).not.toHaveBeenCalled();
    expect(getToken()).toBe(validToken);
    clearSession();
  });
});
