import { test, expect } from "@playwright/test";

/**
 * One happy path is worth more than 50 shallow tests.
 * Login → import fixture CSV → see meal premium → approve → download Gusto bytes.
 */
test("harbor dental meal premium then gusto download", async ({ page, request }) => {
  await page.goto("/login");
  await page.getByLabel("Email").fill("owner@harbordental.example");
  await page.getByLabel("Password").fill("HarborDental!demo");
  await page.getByRole("button", { name: "Sign in" }).click();
  await expect(page.getByRole("heading", { name: /risk|desk/i })).toBeVisible();
  await page.getByRole("link", { name: "Time & imports" }).click();
  await page.getByRole("button", { name: "Commit import" }).click();
  await page.getByRole("link", { name: "Payroll runs" }).click();
  await page.getByRole("button", { name: "Run engine" }).first().click();
  await expect(page.getByText(/MEAL_PREMIUM|Meal/i)).toBeVisible();
  await page.getByLabel(/I confirm I have reviewed exceptions/).check();
  await page.getByRole("button", { name: "Approve" }).click();
  await expect(page.locator("pre.csv")).toContainText("employee_code");
});
