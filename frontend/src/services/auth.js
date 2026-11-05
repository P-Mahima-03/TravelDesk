const TOKEN_KEY = "token";

export function isValidToken(token) {
  if (typeof token !== "string") {
    return false;
  }

  const segments = token.trim().split(".");
  if (
    segments.length !== 3
    || segments.some((segment) => !/^[A-Za-z0-9_-]+$/.test(segment))
  ) {
    return false;
  }

  try {
    const header = JSON.parse(decodeJwtSegment(segments[0]));
    const payload = JSON.parse(decodeJwtSegment(segments[1]));
    return (
      typeof header.alg === "string"
      && header.alg.toLowerCase() !== "none"
      && typeof payload.exp === "number"
      && Number.isFinite(payload.exp)
      && payload.exp > Date.now() / 1000
    );
  } catch {
    return false;
  }
}

function decodeJwtSegment(segment) {
  const base64 = segment.replace(/-/g, "+").replace(/_/g, "/");
  const padded = base64.padEnd(
    base64.length + ((4 - (base64.length % 4)) % 4),
    "=",
  );
  const binary = window.atob(padded);
  const bytes = Uint8Array.from(binary, (character) =>
    character.charCodeAt(0),
  );
  return new TextDecoder().decode(bytes);
}

export function getToken() {
  try {
    const token = window.localStorage.getItem(TOKEN_KEY)?.trim();
    return isValidToken(token) ? token : null;
  } catch {
    return null;
  }
}

export function isAuthenticated() {
  return getToken() !== null;
}

export function setToken(token) {
  if (!isValidToken(token)) {
    return false;
  }

  try {
    window.localStorage.setItem(TOKEN_KEY, token.trim());
    return true;
  } catch {
    return false;
  }
}

export function clearSession() {
  try {
    window.localStorage.removeItem(TOKEN_KEY);
  } catch {
    return;
  }
}

export function logout(navigate) {
  clearSession();

  if (typeof navigate === "function") {
    navigate("/login", { replace: true });
    return;
  }

  window.location.assign("/login");
}
