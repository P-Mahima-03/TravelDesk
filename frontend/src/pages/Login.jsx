import { Link } from "react-router-dom";
import "../styles/Auth.css";

function Login() {
  return (
    <div className="auth-page">
      <div className="auth-card">
        <Link to="/" className="auth-brand">TravelDesk</Link>
        <h1>Welcome back!</h1>
        <p className="auth-subtitle">Sign in to continue your journey.</p>

        <form onSubmit={(e) => e.preventDefault()}>
          <label>Email Address</label>
          <input type="email" placeholder="Enter your email" required />

          <label>Password</label>
          <input type="password" placeholder="Enter your password" required />

          <button type="submit" className="auth-button">Sign In</button>
        </form>

        <p className="auth-switch">
          Don't have an account? <Link to="/register">Create account</Link>
        </p>

        <Link to="/" className="back-home">← Back to Home</Link>
      </div>
    </div>
  );
}

export default Login;