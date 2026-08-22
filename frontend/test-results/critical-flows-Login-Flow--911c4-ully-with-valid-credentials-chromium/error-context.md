# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: critical-flows.spec.ts >> Login Flow >> should login successfully with valid credentials
- Location: e2e\critical-flows.spec.ts:15:7

# Error details

```
Error: expect(locator).toBeVisible() failed

Locator: locator('text=Buenos días')
Expected: visible
Timeout: 10000ms
Error: element(s) not found

Call log:
  - Expect "toBeVisible" with timeout 10000ms
  - waiting for locator('text=Buenos días')

```

```yaml
- navigation "Navegación principal":
  - text: school AulaIA Admin Portal
  - list:
    - listitem:
      - link "dashboard Dashboard":
        - /url: /admin/dashboard
    - listitem:
      - link "school Estudiantes":
        - /url: /admin/estudiantes
    - listitem:
      - link "person_4 Docentes":
        - /url: /admin/docentes
    - listitem:
      - link "grade Grados":
        - /url: /admin/grados
    - listitem:
      - link "meeting_room Secciones":
        - /url: /admin/secciones
    - listitem:
      - link "book Cursos":
        - /url: /admin/cursos
    - listitem:
      - link "calendar_today Horarios":
        - /url: /admin/horarios
    - listitem:
      - link "fact_check Asistencias":
        - /url: /admin/asistencias
    - listitem:
      - link "analytics Reportes":
        - /url: /admin/reportes
    - listitem:
      - link "psychology IA":
        - /url: /admin/ia
    - listitem:
      - link "settings Ajustes":
        - /url: /admin/ajustes
  - link "cast_for_education Modo Aula":
    - /url: /modo-aula
  - button "logout Cerrar sesión"
- banner:
  - heading "Dashboard" [level=2]
  - text: search
  - textbox "Buscar..."
  - button "Notificaciones": notifications
  - button "Ayuda": help
  - button "Perfil": account_circle
- main:
  - heading "Panel de Administración" [level=1]
  - heading "Resumen General" [level=2]
  - paragraph: Desde este panel podrás gestionar estudiantes, docentes, cursos, secciones y revisar reportes globales de asistencia. Utiliza el menú lateral para navegar por las diferentes opciones administrativas.
```

# Test source

```ts
  1   | import { test, expect } from '@playwright/test';
  2   | 
  3   | test.describe('Login Flow', () => {
  4   |   test.beforeEach(async ({ page }) => {
  5   |     await page.goto('/login');
  6   |   });
  7   | 
  8   |   test('should show login form', async ({ page }) => {
  9   |     await expect(page.locator('h1:has-text("AulaIA")')).toBeVisible();
  10  |     await expect(page.locator('input[type="text"], input[formcontrolname="username"]')).toBeVisible();
  11  |     await expect(page.locator('input[type="password"], input[formcontrolname="password"]')).toBeVisible();
  12  |     await expect(page.locator('button[type="submit"]')).toBeVisible();
  13  |   });
  14  | 
  15  |   test('should login successfully with valid credentials', async ({ page }) => {
  16  |     await page.fill('input[type="text"], input[formcontrolname="username"]', 'admin');
  17  |     await page.fill('input[type="password"], input[formcontrolname="password"]', 'Admin12345678!');
  18  |     await page.click('button[type="submit"]');
  19  |     
  20  |     // Wait for redirect to dashboard
  21  |     await page.waitForURL(/\/admin\/dashboard/, { timeout: 10000 });
> 22  |     await expect(page.locator('text=Buenos días')).toBeVisible({ timeout: 10000 });
      |                                                    ^ Error: expect(locator).toBeVisible() failed
  23  |   });
  24  | 
  25  |   test('should show error with invalid credentials', async ({ page }) => {
  26  |     await page.fill('input[type="text"], input[formcontrolname="username"]', 'invalid');
  27  |     await page.fill('input[type="password"], input[formcontrolname="password"]', 'wrong');
  28  |     await page.click('button[type="submit"]');
  29  |     
  30  |     await expect(page.locator('text=Error, text=Credenciales, text=Invalid, text=Incorrectas')).toBeVisible({ timeout: 5000 });
  31  |   });
  32  | });
  33  | 
  34  | test.describe('Admin Dashboard', () => {
  35  |   test.beforeEach(async ({ page }) => {
  36  |     // Login first
  37  |     await page.goto('/login');
  38  |     await page.fill('input[type="text"], input[formcontrolname="username"]', 'admin');
  39  |     await page.fill('input[type="password"], input[formcontrolname="password"]', 'Admin12345678!');
  40  |     await page.click('button[type="submit"]');
  41  |     await page.waitForURL(/\/admin\/dashboard/, { timeout: 15000 });
  42  |   });
  43  | 
  44  |   test('should display admin dashboard', async ({ page }) => {
  45  |     // Dashboard loads (may show empty state for admin without classes)
  46  |     await expect(page.locator('h1, h2')).toContainText('Dashboard', { timeout: 10000 });
  47  |   });
  48  | 
  49  |   test('should navigate to estudiantes', async ({ page }) => {
  50  |     await page.click('text=Estudiantes, a:has-text("Estudiantes")');
  51  |     await expect(page).toHaveURL(/\/admin\/estudiantes/);
  52  |     await expect(page.locator('text=Gestión de Estudiantes')).toBeVisible();
  53  |   });
  54  | 
  55  |   test('should navigate to reportes', async ({ page }) => {
  56  |     await page.click('text=Reportes, a:has-text("Reportes")');
  57  |     await expect(page).toHaveURL(/\/admin\/reportes/);
  58  |     await expect(page.locator('text=Reportes de Asistencia')).toBeVisible();
  59  |   });
  60  | 
  61  |   test('should navigate to IA', async ({ page }) => {
  62  |     await page.click('text=IA, a:has-text("IA")');
  63  |     await expect(page).toHaveURL(/\/admin\/ia/);
  64  |     await expect(page.locator('text=AulaIA IA')).toBeVisible();
  65  |   });
  66  | });
  67  | 
  68  | test.describe('Estudiantes CRUD', () => {
  69  |   test.beforeEach(async ({ page }) => {
  70  |     await page.goto('/login');
  71  |     await page.fill('input[type="text"], input[formcontrolname="username"]', 'admin');
  72  |     await page.fill('input[type="password"], input[formcontrolname="password"]', 'Admin12345678!');
  73  |     await page.click('button[type="submit"]');
  74  |     await expect(page).toHaveURL(/\/admin\/dashboard/);
  75  |     await page.click('text=Estudiantes, a:has-text("Estudiantes")');
  76  |     await expect(page).toHaveURL(/\/admin\/estudiantes/);
  77  |   });
  78  | 
  79  |   test('should list estudiantes', async ({ page }) => {
  80  |     await expect(page.locator('table, .table')).toBeVisible();
  81  |   });
  82  | 
  83  |   test('should create new estudiante', async ({ page }) => {
  84  |     await page.click('text=Nuevo Estudiante, button:has-text("Nuevo")');
  85  |     await expect(page).toHaveURL(/\/admin\/estudiantes\/nuevo/);
  86  |     
  87  |     // Fill form
  88  |     await page.fill('input[formcontrolname="codigo"], input[placeholder*="código" i]', `TEST-${Date.now()}`);
  89  |     await page.fill('input[formcontrolname="nombres"], input[placeholder*="Nombres" i]', 'Test');
  90  |     await page.fill('input[formcontrolname="apellidos"], input[placeholder*="Apellidos" i]', 'Estudiante');
  91  |     
  92  |     // Select grado/sección if dropdowns exist
  93  |     const gradoSelect = page.locator('select[formcontrolname="gradoId"], select:near(:text("Grado"))').first();
  94  |     if (await gradoSelect.isVisible()) {
  95  |       await gradoSelect.selectOption({ index: 1 });
  96  |     }
  97  |     
  98  |     const seccionSelect = page.locator('select[formcontrolname="seccionId"], select:near(:text("Sección"))').first();
  99  |     if (await seccionSelect.isVisible()) {
  100 |       await seccionSelect.selectOption({ index: 1 });
  101 |     }
  102 |     
  103 |     await page.click('button:has-text("Guardar")');
  104 |     
  105 |     // Should redirect back to list
  106 |     await expect(page).toHaveURL(/\/admin\/estudiantes$/);
  107 |     await expect(page.locator('text=Estudiante creado')).toBeVisible({ timeout: 5000 });
  108 |   });
  109 | });
  110 | 
  111 | test.describe('Modo Aula', () => {
  112 |   test.beforeEach(async ({ page }) => {
  113 |     await page.goto('/login');
  114 |     await page.fill('input[type="text"], input[formcontrolname="username"]', 'admin');
  115 |     await page.fill('input[type="password"], input[formcontrolname="password"]', 'Admin12345678!');
  116 |     await page.click('button[type="submit"]');
  117 |     await expect(page).toHaveURL(/\/admin\/dashboard/);
  118 |   });
  119 | 
  120 |   test('should open Modo Aula from dashboard', async ({ page }) => {
  121 |     await page.click('text=Abrir Modo Aula, button:has-text("Abrir Modo Aula")');
  122 |     await expect(page).toHaveURL(/\/modo-aula/);
```