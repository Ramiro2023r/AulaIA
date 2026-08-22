import { test, expect } from '@playwright/test';

test.describe('Login Flow', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
  });

  test('should show login form', async ({ page }) => {
    await expect(page.locator('h1:has-text("AulaIA")')).toBeVisible();
    await expect(page.locator('input[type="text"], input[formcontrolname="username"]')).toBeVisible();
    await expect(page.locator('input[type="password"], input[formcontrolname="password"]')).toBeVisible();
    await expect(page.locator('button[type="submit"]')).toBeVisible();
  });

  test('should login successfully with valid credentials', async ({ page }) => {
    await page.fill('input[type="text"], input[formcontrolname="username"]', 'admin');
    await page.fill('input[type="password"], input[formcontrolname="password"]', 'Admin12345678!');
    await page.click('button[type="submit"]');
    
    // Wait for redirect to dashboard
    await page.waitForURL(/\/admin\/dashboard/, { timeout: 10000 });
    await expect(page.locator('text=Buenos días')).toBeVisible({ timeout: 10000 });
  });

  test('should show error with invalid credentials', async ({ page }) => {
    await page.fill('input[type="text"], input[formcontrolname="username"]', 'invalid');
    await page.fill('input[type="password"], input[formcontrolname="password"]', 'wrong');
    await page.click('button[type="submit"]');
    
    await expect(page.locator('text=Error, text=Credenciales, text=Invalid, text=Incorrectas')).toBeVisible({ timeout: 5000 });
  });
});

test.describe('Admin Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    // Login first
    await page.goto('/login');
    await page.fill('input[type="text"], input[formcontrolname="username"]', 'admin');
    await page.fill('input[type="password"], input[formcontrolname="password"]', 'Admin12345678!');
    await page.click('button[type="submit"]');
    await page.waitForURL(/\/admin\/dashboard/, { timeout: 15000 });
  });

  test('should display admin dashboard', async ({ page }) => {
    // Dashboard loads (may show empty state for admin without classes)
    await expect(page.locator('h1, h2')).toContainText('Dashboard', { timeout: 10000 });
  });

  test('should navigate to estudiantes', async ({ page }) => {
    await page.click('text=Estudiantes, a:has-text("Estudiantes")');
    await expect(page).toHaveURL(/\/admin\/estudiantes/);
    await expect(page.locator('text=Gestión de Estudiantes')).toBeVisible();
  });

  test('should navigate to reportes', async ({ page }) => {
    await page.click('text=Reportes, a:has-text("Reportes")');
    await expect(page).toHaveURL(/\/admin\/reportes/);
    await expect(page.locator('text=Reportes de Asistencia')).toBeVisible();
  });

  test('should navigate to IA', async ({ page }) => {
    await page.click('text=IA, a:has-text("IA")');
    await expect(page).toHaveURL(/\/admin\/ia/);
    await expect(page.locator('text=AulaIA IA')).toBeVisible();
  });
});

test.describe('Estudiantes CRUD', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[type="text"], input[formcontrolname="username"]', 'admin');
    await page.fill('input[type="password"], input[formcontrolname="password"]', 'Admin12345678!');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL(/\/admin\/dashboard/);
    await page.click('text=Estudiantes, a:has-text("Estudiantes")');
    await expect(page).toHaveURL(/\/admin\/estudiantes/);
  });

  test('should list estudiantes', async ({ page }) => {
    await expect(page.locator('table, .table')).toBeVisible();
  });

  test('should create new estudiante', async ({ page }) => {
    await page.click('text=Nuevo Estudiante, button:has-text("Nuevo")');
    await expect(page).toHaveURL(/\/admin\/estudiantes\/nuevo/);
    
    // Fill form
    await page.fill('input[formcontrolname="codigo"], input[placeholder*="código" i]', `TEST-${Date.now()}`);
    await page.fill('input[formcontrolname="nombres"], input[placeholder*="Nombres" i]', 'Test');
    await page.fill('input[formcontrolname="apellidos"], input[placeholder*="Apellidos" i]', 'Estudiante');
    
    // Select grado/sección if dropdowns exist
    const gradoSelect = page.locator('select[formcontrolname="gradoId"], select:near(:text("Grado"))').first();
    if (await gradoSelect.isVisible()) {
      await gradoSelect.selectOption({ index: 1 });
    }
    
    const seccionSelect = page.locator('select[formcontrolname="seccionId"], select:near(:text("Sección"))').first();
    if (await seccionSelect.isVisible()) {
      await seccionSelect.selectOption({ index: 1 });
    }
    
    await page.click('button:has-text("Guardar")');
    
    // Should redirect back to list
    await expect(page).toHaveURL(/\/admin\/estudiantes$/);
    await expect(page.locator('text=Estudiante creado')).toBeVisible({ timeout: 5000 });
  });
});

test.describe('Modo Aula', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[type="text"], input[formcontrolname="username"]', 'admin');
    await page.fill('input[type="password"], input[formcontrolname="password"]', 'Admin12345678!');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL(/\/admin\/dashboard/);
  });

  test('should open Modo Aula from dashboard', async ({ page }) => {
    await page.click('text=Abrir Modo Aula, button:has-text("Abrir Modo Aula")');
    await expect(page).toHaveURL(/\/modo-aula/);
    await expect(page.locator('text=Escanea tu código QR')).toBeVisible();
  });

  test('should show manual code input option', async ({ page }) => {
    await page.goto('/modo-aula');
    await expect(page.locator('text=Ingresar código manual, button:has-text("Ingresar código manual")')).toBeVisible();
  });
});

test.describe('Reportes', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[type="text"], input[formcontrolname="username"]', 'admin');
    await page.fill('input[type="password"], input[formcontrolname="password"]', 'Admin12345678!');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL(/\/admin\/dashboard/);
    await page.click('text=Reportes, a:has-text("Reportes")');
    await expect(page).toHaveURL(/\/admin\/reportes/);
  });

  test('should display reportes page with filters', async ({ page }) => {
    await expect(page.locator('text=Reportes de Asistencia')).toBeVisible();
    await expect(page.locator('input[type="date"]').first()).toBeVisible();
    await expect(page.locator('button:has-text("Generar")')).toBeVisible();
    await expect(page.locator('button:has-text("Excel")')).toBeVisible();
    await expect(page.locator('button:has-text("PDF")')).toBeVisible();
  });
});

test.describe('IA Asistente', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[type="text"], input[formcontrolname="username"]', 'admin');
    await page.fill('input[type="password"], input[formcontrolname="password"]', 'Admin12345678!');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL(/\/admin\/dashboard/);
    await page.click('text=IA, a:has-text("IA")');
    await expect(page).toHaveURL(/\/admin\/ia/);
  });

  test('should display IA page', async ({ page }) => {
    await expect(page.locator('text=AulaIA IA')).toBeVisible();
    await expect(page.locator('input[placeholder*="pregunta" i], textarea[placeholder*="pregunta" i]')).toBeVisible();
  });

  test('should show quick question chips', async ({ page }) => {
    await expect(page.locator('text=¿Quiénes faltaron hoy?')).toBeVisible();
    await expect(page.locator('text=Resumen de esta semana')).toBeVisible();
    await expect(page.locator('text=Estudiantes con tardanzas')).toBeVisible();
  });
});