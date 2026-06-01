import { describe, it, expect } from "vitest";
import {
  cn,
  formatCurrency,
  formatPrice,
  formatNumber,
  calcDiscountPercent,
  relativeTime,
  formatDate,
  formatDateTime,
  getInitials,
  slugify,
  truncate,
} from "@/lib/utils";

describe("formatCurrency / formatPrice", () => {
  it("formats VND correctly", () => {
    expect(formatCurrency(100000)).toContain("100.000");
    expect(formatCurrency(100000)).toContain("₫");
    expect(formatPrice(100000)).toContain("100.000");
    expect(formatPrice(100000)).toContain("₫");
  });

  it("handles zero", () => {
    expect(formatPrice(0)).toContain("0");
  });

  it("formats millions", () => {
    expect(formatPrice(1500000)).toContain("1.500.000");
  });
});

describe("formatNumber", () => {
  it("formats standard numbers with dot separator", () => {
    expect(formatNumber(1234567)).toBe("1.234.567");
  });
});

describe("calcDiscountPercent", () => {
  it("calculates 50% discount", () => {
    expect(calcDiscountPercent(100000, 50000)).toBe(50);
  });

  it("calculates 20% discount", () => {
    expect(calcDiscountPercent(100000, 80000)).toBe(20);
  });

  it("rounds down", () => {
    expect(calcDiscountPercent(30000, 20000)).toBe(33);
  });

  it("handles zero original price", () => {
    expect(calcDiscountPercent(0, 5000)).toBe(0);
  });
});

describe("relativeTime", () => {
  it("returns relative time for valid dates", () => {
    const pastDate = new Date();
    pastDate.setMinutes(pastDate.getMinutes() - 5);
    expect(relativeTime(pastDate.toISOString())).toContain("trước");
  });

  it("falls back to raw string on error", () => {
    expect(relativeTime("invalid-date")).toBe("invalid-date");
  });
});

describe("formatDate", () => {
  it("formats date with default local options", () => {
    const result = formatDate("2024-01-15T00:00:00");
    expect(result).toMatch(/\d{2}\/\d{2}\/\d{4}/);
  });

  it("formats date with string pattern", () => {
    const result = formatDate("2024-01-15T00:00:00", "dd-MM-yyyy");
    expect(result).toBe("15-01-2024");
  });
});

describe("formatDateTime", () => {
  it("formats date and time in HH:mm - dd/MM/yyyy format", () => {
    const result = formatDateTime("2024-01-15T14:30:00");
    expect(result).toBe("14:30 - 15/01/2024");
  });

  it("falls back to raw string on error", () => {
    expect(formatDateTime("invalid-date")).toBe("invalid-date");
  });
});

describe("getInitials", () => {
  it("returns two uppercase initials for multiple words", () => {
    expect(getInitials("Nguyen Van A")).toBe("NA");
    expect(getInitials("Admin User")).toBe("AU");
  });

  it("returns two uppercase initials for single name", () => {
    expect(getInitials("Admin")).toBe("AD");
  });

  it("handles empty string", () => {
    expect(getInitials("")).toBe("");
  });
});

describe("slugify", () => {
  it("converts to lowercase slug", () => {
    expect(slugify("Hello World")).toBe("hello-world");
  });

  it("removes special characters", () => {
    expect(slugify("Rau cải xanh!")).toBe("rau-cai-xanh");
  });

  it("collapses multiple dashes", () => {
    expect(slugify("a  b")).toBe("a-b");
  });
});

describe("truncate", () => {
  it("leaves short strings unchanged", () => {
    expect(truncate("hello", 10)).toBe("hello");
  });

  it("truncates long strings", () => {
    expect(truncate("hello world", 5)).toBe("hello...");
  });

  it("handles exact length", () => {
    expect(truncate("hello", 5)).toBe("hello");
  });
});

describe("cn", () => {
  it("merges classnames", () => {
    expect(cn("a", "b")).toBe("a b");
  });

  it("deduplicates tailwind classes", () => {
    expect(cn("px-2", "px-4")).toBe("px-4");
  });

  it("handles conditional classnames", () => {
    expect(cn("base", false && "ignored", "added")).toBe("base added");
  });
});