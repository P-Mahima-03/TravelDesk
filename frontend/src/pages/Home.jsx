
import "../styles/Home.css";

const destinations = [
  {
    name: "Bali",
    country: "Indonesia",
    price: "$599",
    image:
      "https://images.unsplash.com/photo-1537996194471-e657df975ab4?w=700",
  },
  {
    name: "Santorini",
    country: "Greece",
    price: "$899",
    image:
      "https://images.unsplash.com/photo-1613395877344-13d4a8e0d49e?w=700",
  },
  {
    name: "Swiss Alps",
    country: "Switzerland",
    price: "$1,099",
    image:
      "https://images.unsplash.com/photo-1531366936337-7c912a4589a7?w=700",
  },
];

const packages = [
  {
    name: "Tropical Bali Escape",
    duration: "5 Days / 4 Nights",
    price: "$599",
    image:
      "https://images.unsplash.com/photo-1518548419970-58e3b4079ab2?w=700",
  },
  {
    name: "Greek Island Discovery",
    duration: "7 Days / 6 Nights",
    price: "$899",
    image:
      "https://images.unsplash.com/photo-1533105079780-92b9be482077?w=700",
  },
  {
    name: "Swiss Mountain Adventure",
    duration: "6 Days / 5 Nights",
    price: "$1,099",
    image:
      "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=700",
  },
];

function Home() {
  const handleSearch = (event) => {
    event.preventDefault();
    alert("Destination search will be connected to the TravelDesk API soon!");
  };

  return (
    <div className="travel-home">
      <header className="navbar">
        <a href="/" className="brand">
          <span className="brand-icon">✦</span>
          TravelDesk
        </a>

        <nav className="nav-links">
          <a href="#home">Home</a>
          <a href="#destinations">Destinations</a>
          <a href="#packages">Packages</a>
          <a href="#services">Services</a>
          <a href="#about">About</a>
        </nav>

        <div className="nav-actions">
          <a href="/login" className="login-link">Log in</a>
          <a href="/register" className="nav-button">Sign up</a>
        </div>
      </header>

      <main>
        <section className="hero" id="home">
          <div className="hero-content">
            <span className="eyebrow">DISCOVER THE WORLD WITH US</span>
            <h1>
              Explore beyond
              <br />
              <span>the ordinary.</span>
            </h1>
            <p>
              Discover incredible destinations, plan unforgettable
              journeys and make every trip a story worth telling.
            </p>

            <form className="search-panel" onSubmit={handleSearch}>
              <div className="search-field">
                <label htmlFor="destination">Destination</label>
                <input
                  id="destination"
                  type="text"
                  placeholder="Where do you want to go?"
                  required
                />
              </div>

              <div className="search-field">
                <label htmlFor="travel-date">Travel date</label>
                <input id="travel-date" type="date" required />
              </div>

              <div className="search-field">
                <label htmlFor="travellers">Travellers</label>
                <select id="travellers">
                  <option>1 Traveller</option>
                  <option>2 Travellers</option>
                  <option>3 Travellers</option>
                  <option>4+ Travellers</option>
                </select>
              </div>

              <button type="submit" className="search-button">
                Search Trips
              </button>
            </form>
          </div>

          <div className="hero-bottom">
            <span>YOUR JOURNEY. YOUR STORY.</span>
            <span>01 — DISCOVER · 02 — EXPLORE · 03 — EXPERIENCE</span>
          </div>
        </section>

        <section className="section destinations-section" id="destinations">
          <div className="section-heading">
            <div>
              <span className="eyebrow">HANDPICKED FOR YOU</span>
              <h2>Popular destinations</h2>
              <p>Places that deserve a place on your bucket list.</p>
            </div>
            <a href="#packages" className="text-link">Explore all trips ↗</a>
          </div>

          <div className="destination-grid">
            {destinations.map((destination) => (
              <article className="destination-card" key={destination.name}>
                <img src={destination.image} alt={destination.name} />
                <div className="destination-overlay">
                  <span>{destination.country}</span>
                  <h3>{destination.name}</h3>
                  <p>From {destination.price}</p>
                </div>
              </article>
            ))}
          </div>
        </section>

        <section className="section packages-section" id="packages">
          <div className="section-heading">
            <div>
              <span className="eyebrow">TRAVEL MADE SIMPLE</span>
              <h2>Featured travel packages</h2>
              <p>Thoughtfully planned journeys for every kind of traveller.</p>
            </div>
          </div>

          <div className="package-grid">
            {packages.map((item) => (
              <article className="package-card" key={item.name}>
                <img src={item.image} alt={item.name} />
                <div className="package-info">
                  <span className="package-duration">{item.duration}</span>
                  <h3>{item.name}</h3>
                  <div className="package-footer">
                    <span>Starting from</span>
                    <strong>{item.price}</strong>
                  </div>
                </div>
              </article>
            ))}
          </div>
        </section>

        <section className="services-section" id="services">
          <div className="section">
            <span className="eyebrow">WHY TRAVELDESK</span>
            <h2>Every detail, taken care of.</h2>

            <div className="service-grid">
              <article>
                <span>01</span>
                <h3>Curated journeys</h3>
                <p>Travel experiences thoughtfully selected around your interests.</p>
              </article>
              <article>
                <span>02</span>
                <h3>Easy bookings</h3>
                <p>Plan your travel and manage your bookings in one place.</p>
              </article>
              <article>
                <span>03</span>
                <h3>Travel support</h3>
                <p>Get assistance when you need help with your journey.</p>
              </article>
            </div>
          </div>
        </section>

        <section className="about-section" id="about">
          <span className="eyebrow">LET'S MAKE MEMORIES</span>
          <h2>The world is waiting. Where will you go?</h2>
          <p>Start planning your next unforgettable journey with TravelDesk.</p>
          <a href="#home" className="nav-button">Start exploring ↗</a>
        </section>
      </main>

      <footer className="footer">
        <a href="/" className="brand">
          <span className="brand-icon">✦</span>
          TravelDesk
        </a>
        <p>Explore. Discover. Experience.</p>
        <span>© 2026 TravelDesk. All rights reserved.</span>
      </footer>
    </div>
  );
}

export default Home;
