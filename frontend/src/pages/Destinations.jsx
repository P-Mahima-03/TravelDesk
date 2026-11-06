import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { getDestinations } from "../services/destinationService";
import "../styles/Destinations.css";

function readFilters(search) {
  const params = new URLSearchParams(search);
  return {
    name: params.get("name") ?? "",
    city: params.get("city") ?? "",
    country: params.get("country") ?? "",
  };
}

function getErrorMessage(error) {
  const serverMessage = error?.response?.data?.message;
  if (typeof serverMessage === "string" && serverMessage.trim()) {
    return serverMessage.trim();
  }

  return error?.message || "Unable to load destinations. Please try again.";
}

function DestinationCard({ destination }) {
  return (
    <article className="listing-destination-card">
      {destination.imageUrl ? (
        <img
          className="listing-destination-image"
          src={destination.imageUrl}
          alt={destination.name}
        />
      ) : (
        <div
          className="listing-destination-placeholder"
          aria-label="No destination image available"
          role="img"
        >
          <span>TravelDesk</span>
        </div>
      )}
      <div className="listing-destination-details">
        <p className="listing-destination-location">
          {[destination.city, destination.country].filter(Boolean).join(", ")}
        </p>
        <h2>{destination.name}</h2>
        {destination.description && <p>{destination.description}</p>}
      </div>
    </article>
  );
}

function DestinationFilters({ initialFilters, onSubmit, onClear }) {
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
    <form className="destination-filters" onSubmit={handleSubmit}>
      <label>
        Name
        <input
          name="name"
          type="search"
          value={filters.name}
          onChange={handleChange}
          placeholder="Search destinations"
        />
      </label>
      <label>
        City
        <input
          name="city"
          type="search"
          value={filters.city}
          onChange={handleChange}
          placeholder="Enter a city"
        />
      </label>
      <label>
        Country
        <input
          name="country"
          type="search"
          value={filters.country}
          onChange={handleChange}
          placeholder="Enter a country"
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

function Destinations() {
  const [searchParams, setSearchParams] = useSearchParams();
  const search = searchParams.toString();
  const [retryCount, setRetryCount] = useState(0);
  const [destinations, setDestinations] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const requestKey = `${search}:${retryCount}`;

  useEffect(() => {
    const activeFilters = readFilters(search);
    let isCurrent = true;

    async function loadDestinations() {
      setIsLoading(true);
      setError("");

      try {
        const results = await getDestinations(
          Object.fromEntries(
            Object.entries(activeFilters).filter(([, value]) => value.trim()),
          ),
        );
        if (isCurrent) {
          setDestinations(results);
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

    loadDestinations();

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
    if (!search) {
      return;
    }

    setSearchParams({});
  }

  return (
    <main className="destinations-page">
      <header className="destinations-header">
        <Link to="/" className="destinations-brand">
          TravelDesk
        </Link>
        <Link to="/" className="destinations-home-link">
          Back to home
        </Link>
      </header>

      <section className="destinations-intro">
        <span className="eyebrow">FIND YOUR NEXT ADVENTURE</span>
        <h1>Explore destinations</h1>
        <p>Discover places to visit and start planning your next journey.</p>
      </section>

      <DestinationFilters
        key={search}
        initialFilters={readFilters(search)}
        onSubmit={handleSubmit}
        onClear={handleClear}
      />

      <section className="destination-results" aria-live="polite">
        {isLoading ? (
          <p className="destination-state" role="status">
            Loading destinations...
          </p>
        ) : error ? (
          <p className="destination-state destination-state--error" role="alert">
            {error}
          </p>
        ) : destinations.length === 0 ? (
          <p className="destination-state">No destinations match your search.</p>
        ) : (
          <div className="destination-listing-grid">
            {destinations.map((destination) => (
              <DestinationCard key={destination.id} destination={destination} />
            ))}
          </div>
        )}
      </section>
    </main>
  );
}

export default Destinations;
