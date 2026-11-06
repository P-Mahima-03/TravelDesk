import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { getDestinations } from "../services/destinationService";
import { getPackages } from "../services/packageService";
import "../styles/Destinations.css";
import "../styles/PackageListing.css";

function readFilters(search) {
  const params = new URLSearchParams(search);
  return {
    title: params.get("title") ?? "",
    destinationId: params.get("destinationId") ?? "",
    minPrice: params.get("minPrice") ?? "",
    maxPrice: params.get("maxPrice") ?? "",
    maxDurationDays: params.get("maxDurationDays") ?? "",
  };
}

function getErrorMessage(error) {
  const serverMessage = error?.response?.data?.message;
  if (typeof serverMessage === "string" && serverMessage.trim()) {
    return serverMessage.trim();
  }

  return error?.message || "Unable to load packages. Please try again.";
}

function formatPrice(price) {
  return new Intl.NumberFormat(undefined, {
    style: "currency",
    currency: "USD",
  }).format(price);
}

function PackageCard({ travelPackage }) {
  return (
    <article className="listing-package-card">
      <div className="listing-package-details">
        <p className="listing-package-destination">
          {travelPackage.destinationName}
        </p>
        <h2>{travelPackage.title}</h2>
        <p className="listing-package-duration">
          {travelPackage.durationDays}{" "}
          {travelPackage.durationDays === 1 ? "day" : "days"}
        </p>
        {travelPackage.description && (
          <p className="listing-package-description">
            {travelPackage.description}
          </p>
        )}
        <p className="listing-package-price">{formatPrice(travelPackage.basePrice)}</p>
        <Link
          className="package-details-link"
          to={`/packages/${travelPackage.id}`}
        >
          View details
        </Link>
      </div>
    </article>
  );
}

function PackageFilters({ initialFilters, destinations, onSubmit, onClear }) {
  const [filters, setFilters] = useState(initialFilters);

  function handleChange(event) {
    const { name, value } = event.target;
    setFilters((current) => ({ ...current, [name]: value }));
  }

  function handleSubmit(event) {
    event.preventDefault();
    onSubmit(filters);
  }

  return (
    <form className="destination-filters package-filters" onSubmit={handleSubmit}>
      <label>
        Title
        <input
          name="title"
          type="search"
          value={filters.title}
          onChange={handleChange}
          placeholder="Search package titles"
        />
      </label>
      <label>
        Destination
        <select
          name="destinationId"
          value={filters.destinationId}
          onChange={handleChange}
        >
          <option value="">All destinations</option>
          {destinations.map((destination) => (
            <option key={destination.id} value={destination.id}>
              {destination.name}
            </option>
          ))}
        </select>
      </label>
      <label>
        Minimum price
        <input
          name="minPrice"
          type="number"
          min="0"
          step="0.01"
          value={filters.minPrice}
          onChange={handleChange}
          placeholder="0.00"
        />
      </label>
      <label>
        Maximum price
        <input
          name="maxPrice"
          type="number"
          min="0"
          step="0.01"
          value={filters.maxPrice}
          onChange={handleChange}
          placeholder="Any"
        />
      </label>
      <label>
        Maximum duration (days)
        <input
          name="maxDurationDays"
          type="number"
          min="1"
          step="1"
          value={filters.maxDurationDays}
          onChange={handleChange}
          placeholder="Any"
        />
      </label>
      <div className="destination-filter-actions">
        <button type="submit">Apply filters</button>
        <button
          type="button"
          className="destination-clear-button"
          onClick={onClear}
        >
          Clear filters
        </button>
      </div>
    </form>
  );
}

function Packages() {
  const [searchParams, setSearchParams] = useSearchParams();
  const search = searchParams.toString();
  const [retryCount, setRetryCount] = useState(0);
  const [packages, setPackages] = useState([]);
  const [destinations, setDestinations] = useState([]);
  const [destinationError, setDestinationError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const requestKey = `${search}:${retryCount}`;

  useEffect(() => {
    let isCurrent = true;

    getDestinations()
      .then((results) => {
        if (isCurrent) {
          setDestinations(results);
          setDestinationError("");
        }
      })
      .catch((requestError) => {
        if (isCurrent) {
          setDestinationError(getErrorMessage(requestError));
        }
      });

    return () => {
      isCurrent = false;
    };
  }, []);

  useEffect(() => {
    const activeFilters = readFilters(search);
    const apiFilters = Object.fromEntries(
      Object.entries(activeFilters).filter(([, value]) => value.trim()),
    );
    let isCurrent = true;

    async function loadPackages() {
      setIsLoading(true);
      setError("");

      try {
        const results = await getPackages(apiFilters);
        if (isCurrent) {
          setPackages(results);
        }
      } catch (requestError) {
        if (isCurrent) {
          setError(getErrorMessage(requestError));
        }
      } finally {
        if (isCurrent) {
          setIsLoading(false);
        }
      }
    }

    loadPackages();

    return () => {
      isCurrent = false;
    };
  }, [requestKey, search]);

  function handleSubmit(filters) {
    const nextParams = Object.fromEntries(
      Object.entries(filters)
        .map(([key, value]) => [key, value.trim()])
        .filter(([, value]) => value),
    );
    if (new URLSearchParams(nextParams).toString() === search) {
      if (error) {
        setRetryCount((current) => current + 1);
      }
      return;
    }

    setSearchParams(nextParams);
  }

  function handleClear() {
    if (search) {
      setSearchParams({});
    }
  }

  return (
    <main className="destinations-page package-list-page">
      <header className="destinations-header">
        <Link to="/" className="destinations-brand">
          TravelDesk
        </Link>
        <Link to="/" className="destinations-home-link">
          Back to home
        </Link>
      </header>

      <section className="destinations-intro">
        <span className="eyebrow">PLAN YOUR NEXT JOURNEY</span>
        <h1>Explore travel packages</h1>
        <p>Find a trip that fits your destination, budget, and schedule.</p>
      </section>

      {destinationError && (
        <p className="package-destination-warning" role="status">
          Destination options could not be loaded. {destinationError}
        </p>
      )}

      <PackageFilters
        key={search}
        initialFilters={readFilters(search)}
        destinations={destinations}
        onSubmit={handleSubmit}
        onClear={handleClear}
      />

      <section className="destination-results" aria-live="polite">
        {isLoading ? (
          <p className="destination-state" role="status">
            Loading packages...
          </p>
        ) : error ? (
          <div className="destination-state destination-state--error">
            <p role="alert">{error}</p>
            <button
              type="button"
              className="package-retry-button"
              onClick={() => setRetryCount((current) => current + 1)}
            >
              Retry
            </button>
          </div>
        ) : packages.length === 0 ? (
          <p className="destination-state">No packages match your search.</p>
        ) : (
          <div className="package-listing-grid">
            {packages.map((travelPackage) => (
              <PackageCard key={travelPackage.id} travelPackage={travelPackage} />
            ))}
          </div>
        )}
      </section>
    </main>
  );
}

export default Packages;
