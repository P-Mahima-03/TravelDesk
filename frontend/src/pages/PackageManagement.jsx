import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getDestinations } from "../services/destinationService";
import {
  createPackage,
  deletePackage,
  getCurrentUser,
  getPackages,
  updatePackage,
} from "../services/packageService";
import "../styles/Destinations.css";
import "../styles/PackageListing.css";
import "../styles/PackageManagement.css";

const emptyForm = {
  destinationId: "",
  title: "",
  description: "",
  durationDays: "",
  basePrice: "",
  maxCapacity: "",
};

function getErrorMessage(error) {
  const serverMessage = error?.response?.data?.message;
  if (typeof serverMessage === "string" && serverMessage.trim()) {
    return serverMessage.trim();
  }

  return error?.message || "The request could not be completed. Please try again.";
}

function PackageManagement() {
  const [retryCount, setRetryCount] = useState(0);
  const [packages, setPackages] = useState([]);
  const [destinations, setDestinations] = useState([]);
  const [role, setRole] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [formError, setFormError] = useState("");
  const [feedback, setFeedback] = useState("");
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [isSaving, setIsSaving] = useState(false);
  const [deletingId, setDeletingId] = useState(null);
  const canManage = role === "ADMIN" || role === "TRAVEL_AGENT";

  useEffect(() => {
    let isCurrent = true;

    getCurrentUser()
      .then((currentUser) => {
        if (!isCurrent) {
          return null;
        }

        setRole(currentUser.role);
        if (currentUser.role !== "ADMIN" && currentUser.role !== "TRAVEL_AGENT") {
          return null;
        }

        return Promise.all([getDestinations(), getPackages()]);
      })
      .then((results) => {
        if (isCurrent && results) {
          setDestinations(results[0]);
          setPackages(results[1]);
        }
      })
      .catch((error) => {
        if (isCurrent) {
          setLoadError(getErrorMessage(error));
        }
      })
      .finally(() => {
        if (isCurrent) {
          setIsLoading(false);
        }
      });

    return () => {
      isCurrent = false;
    };
  }, [retryCount]);

  function handleChange(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  function resetForm() {
    setForm(emptyForm);
    setEditingId(null);
    setFormError("");
  }

  function handleEdit(travelPackage) {
    setForm({
      destinationId: String(travelPackage.destinationId),
      title: travelPackage.title,
      description: travelPackage.description ?? "",
      durationDays: String(travelPackage.durationDays),
      basePrice: String(travelPackage.basePrice),
      maxCapacity: travelPackage.maxCapacity == null
        ? ""
        : String(travelPackage.maxCapacity),
    });
    setEditingId(travelPackage.id);
    setFormError("");
    setFeedback("");
    const formElement = document.getElementById("package-management-form");
    if (typeof formElement?.scrollIntoView === "function") {
      formElement.scrollIntoView({ behavior: "smooth", block: "start" });
    }
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setFormError("");
    setFeedback("");

    const destinationId = Number(form.destinationId);
    const durationDays = Number(form.durationDays);
    const basePrice = Number(form.basePrice);
    const maxCapacity = form.maxCapacity ? Number(form.maxCapacity) : null;
    if (!form.title.trim()) {
      setFormError("Package title is required.");
      return;
    }
    if (!destinations.some((destination) => destination.id === destinationId)) {
      setFormError("Select an active destination.");
      return;
    }
    if (!Number.isInteger(durationDays) || durationDays < 1) {
      setFormError("Duration must be a positive whole number of days.");
      return;
    }
    if (!Number.isFinite(basePrice) || basePrice < 0) {
      setFormError("Base price must be zero or greater.");
      return;
    }
    if (maxCapacity !== null && (!Number.isInteger(maxCapacity) || maxCapacity < 1)) {
      setFormError("Maximum capacity must be a positive whole number.");
      return;
    }

    setIsSaving(true);

    const packageData = {
      destinationId,
      title: form.title.trim(),
      description: form.description.trim() || null,
      durationDays,
      basePrice,
      maxCapacity,
    };

    try {
      if (editingId === null) {
        await createPackage(packageData);
        setFeedback("Package created successfully.");
      } else {
        await updatePackage(editingId, packageData);
        setFeedback("Package updated successfully.");
      }
      resetForm();
      setPackages(await getPackages());
    } catch (error) {
      setFormError(getErrorMessage(error));
    } finally {
      setIsSaving(false);
    }
  }

  async function handleDelete(id) {
    setFormError("");
    setFeedback("");
    setDeletingId(id);

    try {
      await deletePackage(id);
      setPackages(await getPackages());
      if (editingId === id) {
        resetForm();
      }
      setFeedback("Package deactivated successfully.");
    } catch (error) {
      setFormError(getErrorMessage(error));
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <main className="destinations-page package-list-page">
      <header className="destinations-header">
        <Link to="/" className="destinations-brand">TravelDesk</Link>
        <nav className="package-page-nav">
          <Link to="/packages" className="destinations-home-link">
            View packages
          </Link>
          <Link to="/" className="destinations-home-link">
            Back to home
          </Link>
        </nav>
      </header>

      <section className="destinations-intro">
        <span className="eyebrow">PACKAGE OPERATIONS</span>
        <h1>Manage travel packages</h1>
        <p>Create, update, and deactivate packages for active destinations.</p>
      </section>

      {isLoading ? (
        <section className="destination-results">
          <p className="destination-state" role="status">
            Loading package management...
          </p>
        </section>
      ) : loadError ? (
        <section className="destination-results">
          <div className="destination-state destination-state--error">
            <p role="alert">{loadError}</p>
            <button
              type="button"
              className="package-retry-button"
              onClick={() => {
                setIsLoading(true);
                setLoadError("");
                setRetryCount((current) => current + 1);
              }}
            >
              Retry
            </button>
          </div>
        </section>
      ) : !canManage ? (
        <section className="destination-results">
          <p className="destination-state destination-state--error" role="alert">
            Package management is available to administrators and travel agents only.
          </p>
        </section>
      ) : (
        <>
          {feedback && <p className="package-management-feedback" role="status">{feedback}</p>}
          <section className="package-management-form-section">
            <h2>{editingId === null ? "Create a package" : "Edit package"}</h2>
            <form
              id="package-management-form"
              className="package-management-form"
              onSubmit={handleSubmit}
            >
              <label>
                Destination
                <select
                  name="destinationId"
                  value={form.destinationId}
                  onChange={handleChange}
                  required
                >
                  <option value="">Select a destination</option>
                  {destinations.map((destination) => (
                    <option key={destination.id} value={destination.id}>
                      {destination.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Title
                <input
                  name="title"
                  value={form.title}
                  onChange={handleChange}
                  maxLength="200"
                  required
                />
              </label>
              <label className="package-management-description">
                Description
                <textarea
                  name="description"
                  value={form.description}
                  onChange={handleChange}
                  rows="4"
                />
              </label>
              <label>
                Duration (days)
                <input
                  name="durationDays"
                  type="number"
                  min="1"
                  step="1"
                  value={form.durationDays}
                  onChange={handleChange}
                  required
                />
              </label>
              <label>
                Base price
                <input
                  name="basePrice"
                  type="number"
                  min="0"
                  step="0.01"
                  value={form.basePrice}
                  onChange={handleChange}
                  required
                />
              </label>
              <label>
                Maximum capacity
                <input
                  name="maxCapacity"
                  type="number"
                  min="1"
                  step="1"
                  value={form.maxCapacity}
                  onChange={handleChange}
                />
              </label>
              {formError && (
                <p className="package-management-error" role="alert">
                  {formError}
                </p>
              )}
              <div className="package-management-form-actions">
                <button type="submit" disabled={isSaving || destinations.length === 0}>
                  {isSaving
                    ? "Saving..."
                    : editingId === null
                      ? "Create package"
                      : "Save changes"}
                </button>
                {editingId !== null && (
                  <button
                    type="button"
                    className="package-management-cancel"
                    onClick={resetForm}
                    disabled={isSaving}
                  >
                    Cancel edit
                  </button>
                )}
              </div>
              {destinations.length === 0 && (
                <p className="package-management-empty-destinations">
                  No active destinations are available. Add an active destination
                  before creating a package.
                </p>
              )}
            </form>
          </section>

          <section className="package-management-list">
            <h2>Existing packages</h2>
            {packages.length === 0 ? (
              <p className="destination-state">No packages are available to manage.</p>
            ) : (
              <div className="package-management-items">
                {packages.map((travelPackage) => (
                  <article className="package-management-item" key={travelPackage.id}>
                    <div>
                      <p className="listing-package-destination">
                        {travelPackage.destinationName}
                      </p>
                      <h3>{travelPackage.title}</h3>
                      <p>
                        {travelPackage.durationDays} days ·{" "}
                        {new Intl.NumberFormat(undefined, {
                          style: "currency",
                          currency: "USD",
                        }).format(travelPackage.basePrice)}
                      </p>
                    </div>
                    <div className="package-management-item-actions">
                      <button
                        type="button"
                        onClick={() => handleEdit(travelPackage)}
                      >
                        Edit
                      </button>
                      <button
                        type="button"
                        className="package-management-delete"
                        disabled={deletingId === travelPackage.id}
                        onClick={() => handleDelete(travelPackage.id)}
                      >
                        {deletingId === travelPackage.id ? "Deleting..." : "Deactivate"}
                      </button>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </section>
        </>
      )}
    </main>
  );
}

export default PackageManagement;
