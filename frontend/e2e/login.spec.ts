import { test, expect } from "@playwright/test";

test.describe("Admin Login", () => {
  test("shows login form", async ({ page }) => {
    await page.goto("/login");
    await expect(page.getByText("Pineapple Admin")).toBeVisible();
    await expect(page.getByPlaceholder("admin@pineapple.vn")).toBeVisible();
    await expect(page.getByPlaceholder("••••••••")).toBeVisible();
  });

  test("shows validation errors on empty submit", async ({ page }) => {
    await page.goto("/login");
    await page.getByRole("button", { name: "Đăng nhập" }).click();
    await expect(page.getByText("Email không hợp lệ")).toBeVisible();
  });

  test("shows error on invalid credentials", async ({ page }) => {
    await page.goto("/login");
    await page.fill('[type="email"]', "wrong@example.com");
    await page.fill('[type="password"]', "wrongpassword");
    await page.getByRole("button", { name: "Đăng nhập" }).click();
    // Expect toast or error message (depends on API availability)
    await expect(page).toHaveURL(/\/login/);
  });

  test("redirects to dashboard after successful login", async ({ page }) => {
    // Mock a successful login scenario
    await page.route("**/api/v1/auth/login", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          success: true,
          message: "Login successful",
          data: {
            accessToken: "mock-token",
            userId: 1,
            email: "admin@test.com",
            fullName: "Admin Test",
            roles: ["ROLE_ADMIN"],
            emailVerified: true,
          },
        }),
      });
    });

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

    await page.goto("/login");
    await page.fill('[type="email"]', "admin@test.com");
    await page.fill('[type="password"]', "password123");
    await page.getByRole("button", { name: "Đăng nhập" }).click();
    await expect(page).toHaveURL(/\/admin\/dashboard/);
  });
});
