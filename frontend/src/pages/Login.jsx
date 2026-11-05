import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import api from "../services/api";
import { isValidToken, setToken } from "../services/auth";
import { getAuthErrorMessage } from "../services/authError";
import "../styles/Auth.css";

function Login() {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [feedback, setFeedback] = useState(null);
  const location = useLocation();
  const navigate = useNavigate();

  async function handleSubmit(event) {
    event.preventDefault();
    setIsSubmitting(true);
    setFeedback(null);

    const formData = new FormData(event.currentTarget);
    const credentials = {
      email: formData.get("email").trim(),
      password: formData.get("password"),
    };

    try {
      const response = await api.post("/auth/login", credentials);
      const token = response?.data?.token;

      if (!isValidToken(token)) {
        setFeedback({
          type: "error",
          message:
            "The server did not return a valid authentication token. Please try again later.",
        });
        return;
      }

      if (!setToken(token)) {
        setFeedback({
          type: "error",
          message:
            "You were signed in, but this browser could not save your session. Please enable browser storage and try again.",
        });
        return;
      }

      setFeedback({ type: "success", message: "Signed in successfully." });
      const requestedLocation = location.state?.from;
      const destination =
        typeof requestedLocation?.pathname === "string"
          && requestedLocation.pathname.startsWith("/")
          && !requestedLocation.pathname.startsWith("//")
          ? {
              pathname: requestedLocation.pathname,
              search: requestedLocation.search ?? "",
              hash: requestedLocation.hash ?? "",
            }
          : "/";
      navigate(destination, { replace: true });
    } catch (error) {
      setFeedback({ type: "error", message: getAuthErrorMessage(error) });
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <Link to="/" className="auth-brand">TravelDesk</Link>
        <h1>Welcome back!</h1>
        <p className="auth-subtitle">Sign in to continue your journey.</p>

        <form onSubmit={handleSubmit} aria-busy={isSubmitting}>
          <label htmlFor="login-email">Email Address</label>
          <input
            id="login-email"
            name="email"
            type="email"
            autoComplete="email"
            placeholder="Enter your email"
            required
            disabled={isSubmitting}
          />

          <label htmlFor="login-password">Password</label>
          <input
            id="login-password"
            name="password"
            type="password"
            autoComplete="current-password"
            placeholder="Enter your password"
            required
            disabled={isSubmitting}
          />

          <button
            type="submit"
            className="auth-button"
            disabled={isSubmitting}
          >
            {isSubmitting ? "Signing In..." : "Sign In"}
          </button>
        </form>

        {feedback && (
          <p
            className={`auth-feedback auth-feedback--${feedback.type}`}
            role={feedback.type === "error" ? "alert" : "status"}
          >
            {feedback.message}
          </p>
        )}

        <p className="auth-switch">
          Don't have an account? <Link to="/register">Create account</Link>
        </p>

        <Link to="/" className="back-home">← Back to Home</Link>
      </div>
    </div>
  );
}

export default Login;