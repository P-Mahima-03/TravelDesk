# TravelDesk Frontend

The TravelDesk frontend is a React-based web application built with Vite. It provides the user interface for the TravelDesk travel agency platform.

## Technology Stack

- React.js
- Vite
- JavaScript (ES6+)
- React Router DOM
- Axios
- CSS3
- ESLint

## Project Structure

```text
frontend/
├── public/
├── src/
│   ├── assets/
│   ├── components/
│   ├── context/
│   ├── hooks/
│   ├── layouts/
│   ├── pages/
│   ├── routes/
│   │   └── AppRouter.jsx
│   ├── services/
│   │   └── api.js
│   ├── styles/
│   ├── utils/
│   ├── App.jsx
│   ├── index.css
│   └── main.jsx
├── .env.example
├── index.html
├── package.json
└── vite.config.js
```

## Prerequisites

- Node.js 18 or later
- npm

## Installation

```bash
npm install
```

## Environment Configuration

Create a `.env` file using `.env.example` as a reference.

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

## Development Server

```bash
npm run dev
```

Open `http://localhost:5173` in your browser.

## Production Build

```bash
npm run build
```

## Code Quality

```bash
npm run lint
```

## Project Status

Frontend foundation setup completed. Application modules and user-facing features will be introduced through subsequent development tasks.