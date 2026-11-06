import { createBrowserRouter } from "react-router-dom";

import Home from "../pages/Home";
import Login from "../pages/Login";
import Register from "../pages/Register";
import Destinations from "../pages/Destinations";
import Packages from "../pages/Packages";

const router = createBrowserRouter([
  {
    path: "/",
    element: <Home />,
  },
  {
    path: "/login",
    element: <Login />,
  },
  {
    path: "/register",
    element: <Register />,
  },
  {
    path: "/destinations",
    element: <Destinations />,
  },
  {
    path: "/packages",
    element: <Packages />,
  },
]);

export default router;