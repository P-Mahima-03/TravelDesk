import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { getDestinations } from "../services/destinationService";
import {
  createPackage,
  deletePackage,
  getCurrentUser,
  getPackages,
  updatePackage,
} from "../services/packageService";
import PackageManagement from "./PackageManagement";

vi.mock("../services/destinationService", () => ({
  getDestinations: vi.fn(),
}));

vi.mock("../services/packageService", () => ({
  createPackage: vi.fn(),
  deletePackage: vi.fn(),
  getCurrentUser: vi.fn(),
  getPackages: vi.fn(),
  updatePackage: vi.fn(),
}));

const existingPackage = {
  id: 21,
  destinationId: 5,
  destinationName: "Kyoto",
  title: "Temple Walk",
  description: "Guided tour",
  durationDays: 5,
  basePrice: 250,
  maxCapacity: 12,
};

function renderManagement() {
  return render(
    <MemoryRouter>
      <PackageManagement />
    </MemoryRouter>,
  );
}

describe("PackageManagement", () => {
  afterEach(() => {
    cleanup();
    vi.resetAllMocks();
  });

  function mockManager(role = "ADMIN") {
    getCurrentUser.mockResolvedValue({ role });
    getDestinations.mockResolvedValue([{ id: 5, name: "Kyoto" }]);
    getPackages.mockResolvedValue([existingPackage]);
  }

  it("prevents customers from accessing package management actions", async () => {
    getCurrentUser.mockResolvedValue({ role: "CUSTOMER" });
    renderManagement();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Package management is available to administrators and travel agents only.",
    );
    expect(screen.queryByRole("button", { name: "Create package" }))
      .not.toBeInTheDocument();
    expect(getPackages).not.toHaveBeenCalled();
  });

  it("allows an administrator to create a package with a selected destination", async () => {
    mockManager();
    createPackage.mockResolvedValue({ id: 22 });
    getPackages
      .mockResolvedValueOnce([existingPackage])
      .mockResolvedValueOnce([existingPackage, { ...existingPackage, id: 22 }]);
    renderManagement();
    await screen.findByRole("heading", { name: "Existing packages" });

    fireEvent.change(screen.getByLabelText("Destination"), {
      target: { value: "5" },
    });
    fireEvent.change(screen.getByLabelText("Title"), {
      target: { value: "  Garden Visit " },
    });
    fireEvent.change(screen.getByLabelText("Duration (days)"), {
      target: { value: "4" },
    });
    fireEvent.change(screen.getByLabelText("Base price"), {
      target: { value: "99.50" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Create package" }));

    await waitFor(() => {
      expect(createPackage).toHaveBeenCalledWith({
        destinationId: 5,
        title: "Garden Visit",
        description: null,
        durationDays: 4,
        basePrice: 99.5,
        maxCapacity: null,
      });
    });
    expect(await screen.findByRole("status")).toHaveTextContent(
      "Package created successfully.",
    );
  });

  it("allows a travel agent to edit a package", async () => {
    mockManager("TRAVEL_AGENT");
    updatePackage.mockResolvedValue({ ...existingPackage, title: "Revised Walk" });
    getPackages
      .mockResolvedValueOnce([existingPackage])
      .mockResolvedValueOnce([{ ...existingPackage, title: "Revised Walk" }]);
    renderManagement();
    await screen.findByRole("heading", { name: "Existing packages" });

    fireEvent.click(screen.getByRole("button", { name: "Edit" }));
    fireEvent.change(screen.getByLabelText("Title"), {
      target: { value: "Revised Walk" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Save changes" }));

    await waitFor(() => {
      expect(updatePackage).toHaveBeenCalledWith(21, {
        destinationId: 5,
        title: "Revised Walk",
        description: "Guided tour",
        durationDays: 5,
        basePrice: 250,
        maxCapacity: 12,
      });
    });
  });

  it("soft-deletes a package and refreshes the list", async () => {
    mockManager();
    deletePackage.mockResolvedValue(undefined);
    getPackages
      .mockResolvedValueOnce([existingPackage])
      .mockResolvedValueOnce([]);
    renderManagement();
    await screen.findByRole("heading", { name: "Existing packages" });

    fireEvent.click(screen.getByRole("button", { name: "Deactivate" }));

    await waitFor(() => {
      expect(deletePackage).toHaveBeenCalledWith(21);
      expect(getPackages).toHaveBeenCalledTimes(2);
    });
    expect(await screen.findByText("No packages are available to manage."))
      .toBeInTheDocument();
  });

  it("shows API errors when package creation fails", async () => {
    mockManager();
    createPackage.mockRejectedValue({
      response: { data: { message: "Destination is no longer active" } },
    });
    renderManagement();
    await screen.findByRole("heading", { name: "Existing packages" });

    fireEvent.change(screen.getByLabelText("Destination"), {
      target: { value: "5" },
    });
    fireEvent.change(screen.getByLabelText("Title"), {
      target: { value: "A valid title" },
    });
    fireEvent.change(screen.getByLabelText("Duration (days)"), {
      target: { value: "3" },
    });
    fireEvent.change(screen.getByLabelText("Base price"), {
      target: { value: "30" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Create package" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Destination is no longer active",
    );
  });

  it("rejects a whitespace-only title before sending a request", async () => {
    mockManager();
    renderManagement();
    await screen.findByRole("heading", { name: "Existing packages" });

    fireEvent.change(screen.getByLabelText("Destination"), {
      target: { value: "5" },
    });
    fireEvent.change(screen.getByLabelText("Title"), {
      target: { value: "   " },
    });
    fireEvent.change(screen.getByLabelText("Duration (days)"), {
      target: { value: "3" },
    });
    fireEvent.change(screen.getByLabelText("Base price"), {
      target: { value: "30" },
    });
    fireEvent.submit(document.getElementById("package-management-form"));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Package title is required.",
    );
    expect(createPackage).not.toHaveBeenCalled();
  });
});
