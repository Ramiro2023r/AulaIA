---
name: Academic Precision & Clarity
colors:
  surface: '#faf8ff'
  surface-dim: '#d2d9f4'
  surface-bright: '#faf8ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f3ff'
  surface-container: '#eaedff'
  surface-container-high: '#e2e7ff'
  surface-container-highest: '#dae2fd'
  on-surface: '#131b2e'
  on-surface-variant: '#434655'
  inverse-surface: '#283044'
  inverse-on-surface: '#eef0ff'
  outline: '#737686'
  outline-variant: '#c3c6d7'
  surface-tint: '#0053db'
  primary: '#004ac6'
  on-primary: '#ffffff'
  primary-container: '#2563eb'
  on-primary-container: '#eeefff'
  inverse-primary: '#b4c5ff'
  secondary: '#006c49'
  on-secondary: '#ffffff'
  secondary-container: '#6cf8bb'
  on-secondary-container: '#00714d'
  tertiary: '#784b00'
  on-tertiary: '#ffffff'
  tertiary-container: '#996100'
  on-tertiary-container: '#ffeedd'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dbe1ff'
  primary-fixed-dim: '#b4c5ff'
  on-primary-fixed: '#00174b'
  on-primary-fixed-variant: '#003ea8'
  secondary-fixed: '#6ffbbe'
  secondary-fixed-dim: '#4edea3'
  on-secondary-fixed: '#002113'
  on-secondary-fixed-variant: '#005236'
  tertiary-fixed: '#ffddb8'
  tertiary-fixed-dim: '#ffb95f'
  on-tertiary-fixed: '#2a1700'
  on-tertiary-fixed-variant: '#653e00'
  background: '#faf8ff'
  on-background: '#131b2e'
  surface-variant: '#dae2fd'
typography:
  display:
    fontFamily: Inter
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  label-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
  label-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 4px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  xxl: 48px
  container-margin: 24px
  gutter: 16px
---

## Brand & Style

The design system is built on a **SaaS-inspired Corporate Modern** aesthetic tailored for the educational sector. It prioritizes clarity, trust, and efficiency, bridging the gap between professional administrative tools for teachers and accessible, encouraging interfaces for 11-year-old students.

The visual language avoids "childish" tropes (like primary-color blocks or heavy cartoons) in favor of a sophisticated, structured environment that respects the maturity of upper-primary students. The style utilizes heavy white space, a disciplined grid, and subtle depth to create a focused atmosphere conducive to learning and management.

## Colors

This design system uses a logic-driven palette to ensure functional clarity. 
- **Primary Blue** is the signal for action and navigation.
- **Success Green** and **Warning Orange** are used strictly for status indicators (attendance marked, late arrival, etc.).
- **Neutral/Background** shades utilize a cool-gray scale to reduce eye strain during prolonged classroom use.

To meet WCAG AA standards, all text on white or light-gray surfaces must use the Primary Text (#0F172A) or Secondary Text (#64748B) colors. Interactive states must never rely on color alone; they should be accompanied by weight changes or iconography.

## Typography

**Inter** is the sole typeface for this design system, chosen for its exceptional legibility in data-heavy SaaS environments. 

- **Hierarchy:** Use `display` only for dashboard overviews. `headline-lg` and `headline-md` define section architecture.
- **Legibility:** `body-lg` is preferred for student-facing instructions to ensure high readability.
- **Accessibility:** Maintain a minimum font size of 14px for all functional labels. Use `label-sm` sparingly for secondary metadata.

## Layout & Spacing

The design system employs a **12-column fluid grid** for desktop and a **4-column grid** for mobile. 

- **Generous Breathing Room:** A base unit of 8px (sm) governs the rhythm. Use `lg` (24px) for internal card padding and `xxl` (48px) to separate distinct functional blocks.
- **Touch Targets:** All interactive elements (buttons, checkboxes) must occupy a minimum area of 44x44px, regardless of the visual size of the icon or label.
- **Alignment:** Consistent left-alignment is required for all data lists to aid rapid scanning by teachers.

## Elevation & Depth

This design system uses **Tonal Layers** and **Ambient Shadows** to create a sense of organized hierarchy without visual clutter.

- **Level 0 (Background):** #F8FAFC. The canvas for all content.
- **Level 1 (Cards/Containers):** #FFFFFF. Used for the main content area, featuring a 1px border (#E2E8F0) and a very soft shadow: `0px 4px 6px -1px rgba(0, 0, 0, 0.05)`.
- **Level 2 (Dropdowns/Modals):** Floating elements use a more pronounced but still diffused shadow: `0px 10px 15px -3px rgba(0, 0, 0, 0.1)`.

Avoid high-contrast black shadows or heavy inner-shadows. The goal is to make elements appear to rest gently on the background.

## Shapes

The shape language is defined as **Rounded**, conveying friendliness and safety suitable for an educational environment.

- **Standard Elements:** Buttons, Input fields, and Chips use a `0.5rem` (8px) radius.
- **Large Containers:** Cards and Modals use `rounded-lg` (16px) to soften the overall appearance of the dashboard.
- **Interactive States:** On hover, elements do not change shape but may increase the stroke width or slightly darken the background color.

## Components

### Buttons
- **Primary:** Solid #2563EB with white text. 16px horizontal padding.
- **Secondary:** White background with #E2E8F0 border and #0F172A text.
- **States:** Hover states should be a 10% darken of the base color. Focus states must show a 2px offset ring in Primary Blue.

### Chips (Attendance Status)
- Use a light background (10% opacity of the status color) with a bold label.
- For example, "Present" uses light green background with #10B981 text and a small leading check icon.

### Input Fields
- Labels must always be visible (not placeholder-only).
- Border: #E2E8F0; Background: #FFFFFF.
- Height: 48px to ensure easy tapping for students on tablets.

### Cards
- White background, 16px corner radius, subtle shadow.
- Header sections within cards should be separated by a fine 1px horizontal rule.

### Icons
- Use **Lucide-style** linear icons with a 2px stroke width. 
- Icons should be paired with text labels wherever possible to ensure accessibility for younger users.

### Attendance Grid
- A specialized component showing student avatars. 
- Use a 12px gap between items. 
- Selection state: A 2px solid #2563EB border around the student card.