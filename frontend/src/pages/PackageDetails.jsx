import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getPackage } from "../services/packageService";
import "../styles/Destinations.css";
import "../styles/PackageListing.css";

function getErrorMessage(error) {
  const serverMessage = error?.response?.data?.message;
  if (typeof serverMessage === "string" && serverMessage.trim()) {
    return serverMessage.trim();
  }

  return error?.message || "Unable to load this package. Please try again.";
}

function formatPrice(price) {
  return new Intl.NumberFormat(undefined, {
    style: "currency",
    currency: "USD",
  }).format(price);
}

function PackageDetails() {
  const { id } = useParams();
  const [retryCount, setRetryCount] = useState(0);
  const [travelPackage, setTravelPackage] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [isNotFound, setIsNotFound] = useState(false);

  useEffect(() => {
    let isCurrent = true;

    async function loadPackage() {
      setIsLoading(true);
      setError("");
      setIsNotFound(false);

      try {
        const result = await getPackage(id);
        if (isCurrent) {
          setTravelPackage(result);
        }
      } catch (requestError) {
        if (isCurrent) {
          setError(getErrorMessage(requestError));
          setIsNotFound(requestError?.response?.status === 404);
          setTravelPackage(null);
        }
      } finally {
        if (isCurrent) {
          setIsLoading(false);
        }
      }
    }

    loadPackage();

    return () => {
      isCurrent = false;
    };
  }, [id, retryCount]);

  return (
    <main className="destinations-page package-list-page">
      <header className="destinations-header">
        <Link to="/" className="destinations-brand">
          TravelDesk
        </Link>
        <Link to="/packages" className="destinations-home-link">
          Back to Packages
        </Link>
      </header>

      <section className="destinations-intro">
        <span className="eyebrow">YOUR NEXT JOURNEY</span>
        <h1>Package details</h1>
      </section>

      <section className="destination-results package-detail-results" aria-live="polite">
        {isLoading ? (
          <p className="destination-state" role="status">
            Loading package details...
          </p>
        ) : error ? (
          <div className="destination-state destination-state--error">
            <p role="alert">
              {isNotFound ? "Package not found." : error}
            </p>
            {!isNotFound && (
              <button
                type="button"
                className="package-retry-button"
                onClick={() => setRetryCount((current) => current + 1)}
              >
                Retry
              </button>
            )}
          </div>
        ) : travelPackage ? (
          <article className="package-detail-card">
            <p className="listing-package-destination">
              {travelPackage.destinationName}
            </p>
            <h2>{travelPackage.title}</h2>
            <p className="package-detail-description">
              {travelPackage.description || "No description is available for this package."}
            </p>
            <dl className="package-detail-facts">
              <div>
                <dt>Duration</dt>
                <dd>
                  {travelPackage.durationDays}{" "}
                  {travelPackage.durationDays === 1 ? "day" : "days"}
                </dd>
              </div>
              <div>
                <dt>Base price</dt>
                <dd>{formatPrice(travelPackage.basePrice)}</dd>
              </div>
              <div>
                <dt>Maximum capacity</dt>
                <dd>
                  {travelPackage.maxCapacity ?? "Not specified"}
                </dd>
              </div>
            </dl>
          </article>
        ) : null}
      </section>
    </main>
  );
}

export default PackageDetails;
