import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { StatusBadge } from "@/components/shared/status-badge";

describe("StatusBadge", () => {
  it("renders PENDING status in Vietnamese", () => {
    render(<StatusBadge status="PENDING" />);
    expect(screen.getByText("Chờ xác nhận")).toBeInTheDocument();
  });

  it("renders ACTIVE status", () => {
    render(<StatusBadge status="ACTIVE" />);
    expect(screen.getByText("Hoạt động")).toBeInTheDocument();
  });

  it("renders CANCELLED status", () => {
    render(<StatusBadge status="CANCELLED" />);
    expect(screen.getByText("Đã hủy")).toBeInTheDocument();
  });

  it("renders unknown status as-is", () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    render(<StatusBadge status={"UNKNOWN_STATUS" as any} />);
    expect(screen.getByText("UNKNOWN_STATUS")).toBeInTheDocument();
  });
});