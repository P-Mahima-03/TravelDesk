import { Link } from "react-router-dom";
import "../styles/Auth.css";

function Register() {
  return (
    <div className="auth-page">
      <div className="auth-card">
        <Link to="/" className="auth-brand">TravelDesk</Link>
        <h1>Create your account</h1>
        <p className="auth-subtitle">Start planning your next adventure.</p>

        <form onSubmit={(e) => e.preventDefault()}>
          <label>First Name</label>
          <input type="text" placeholder="Enter first name" required />

          <label>Last Name</label>
          <input type="text" placeholder="Enter last name" />

          <label>Email Address</label>
          <input type="email" placeholder="Enter your email" required />

          <label>Phone Number</label>
          <input type="tel" placeholder="Enter phone number" />

          <label>Password</label>
          <input type="password" placeholder="Create a password" minLength="8" required />

          <button type="submit" className="auth-button">Create Account</button>
        </form>

        <p className="auth-switch">
          Already have an account? <Link to="/login">Sign in</Link>
        </p>

        <Link to="/" className="back-home">← Back to Home</Link>
      </div>
    </div>
  );
}

export default Register;