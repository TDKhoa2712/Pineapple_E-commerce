import { test, expect } from "@playwright/test";

test.describe("Admin Dashboard", () => {
  test.beforeEach(async ({ page }) => {
    // Mock auth
    await page.route("**/api/v1/auth/me", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          success: true,
          data: {
            id: 1,
            email: "admin@test.com",
            fullName: "Admin Test",
            roles: ["ROLE_ADMIN"],
            status: "ACTIVE",
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          },
        }),
      });
    });

    await page.route("**/api/v1/orders/admin**", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          success: true,
          data: {
            content: [],
            page: 0,
            size: 20,
            totalElements: 0,
            totalPages: 0,
            last: true,
          },
        }),
      });
    });

    await page.route("**/api/v1/farms/admin**", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          success: true,
          data: {
            content: [],
            page: 0,
            size: 20,
            totalElements: 0,
            totalPages: 0,
            last: true,
          },
        }),
      });
    });

    // Pre-set localStorage to bypass auth check
    await page.addInitScript(() => {
      localStorage.setItem("accessToken", "mock-token");
    });
  });

  test("shows dashboard heading", async ({ page }) => {
    await page.goto("/admin/dashboard");
    await expect(page.getByText("Tổng quan")).toBeVisible();
  });

  test("shows sidebar navigation links", async ({ page }) => {
    await page.goto("/admin/dashboard");
    await expect(page.getByText("Đơn hàng")).toBeVisible();
    await expect(page.getByText("Người dùng")).toBeVisible();
    await expect(page.getByText("Nông trại")).toBeVisible();
  });
});
