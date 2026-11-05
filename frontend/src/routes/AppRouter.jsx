
import { createBrowserRouter } from "react-router-dom";

const router = createBrowserRouter([
  {
    path: "/",
    element: (
      <div>
        <h1>TravelDesk</h1>
        <p>Your journey starts here.</p>
      </div>
    ),
  },
]);

export default router;
