import { useState } from "react";
import { Link } from "react-router-dom";
import api from "../services/api";
import { getAuthErrorMessage } from "../services/authError";
import "../styles/Auth.css";

function Register() {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [feedback, setFeedback] = useState(null);

  async function handleSubmit(event) {
    event.preventDefault();
    setIsSubmitting(true);
    setFeedback(null);

    const formData = new FormData(event.currentTarget);
    const registration = {
      firstName: formData.get("firstName").trim(),
      lastName: formData.get("lastName").trim(),
      email: formData.get("email").trim(),
      password: formData.get("password"),
      phone: formData.get("phone").trim(),
    };

    try {
      await api.post("/auth/register", registration);
      setFeedback({
        type: "success",
        message: "Your account has been created. You can now sign in.",
      });
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
        <h1>Create your account</h1>
        <p className="auth-subtitle">Start planning your next adventure.</p>

        <form onSubmit={handleSubmit} aria-busy={isSubmitting}>
          <label htmlFor="register-first-name">First Name</label>
          <input
            id="register-first-name"
            name="firstName"
            type="text"
            autoComplete="given-name"
            placeholder="Enter first name"
            required
            disabled={isSubmitting}
          />

          <label htmlFor="register-last-name">Last Name</label>
          <input
            id="register-last-name"
            name="lastName"
            type="text"
            autoComplete="family-name"
            placeholder="Enter last name"
            disabled={isSubmitting}
          />

          <label htmlFor="register-email">Email Address</label>
          <input
            id="register-email"
            name="email"
            type="email"
            autoComplete="email"
            placeholder="Enter your email"
            required
            disabled={isSubmitting}
          />

          <label htmlFor="register-phone">Phone Number</label>
          <input
            id="register-phone"
            name="phone"
            type="tel"
            autoComplete="tel"
            placeholder="Enter phone number"
            disabled={isSubmitting}
          />

          <label htmlFor="register-password">Password</label>
          <input
            id="register-password"
            name="password"
            type="password"
            autoComplete="new-password"
            placeholder="Create a password"
            minLength="8"
            required
            disabled={isSubmitting}
          />

          <button
            type="submit"
            className="auth-button"
            disabled={isSubmitting}
          >
            {isSubmitting ? "Creating Account..." : "Create Account"}
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
          Already have an account? <Link to="/login">Sign in</Link>
        </p>

        <Link to="/" className="back-home">← Back to Home</Link>
      </div>
    </div>
  );
}

export default Register;