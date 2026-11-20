---
name: Fiyat Radar Design System
colors:
  surface: '#fbf8ff'
  surface-dim: '#dad9e3'
  surface-bright: '#fbf8ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f4f2fd'
  surface-container: '#F1F5F9'
  surface-container-high: '#e8e7f1'
  surface-container-highest: '#e3e1ec'
  on-surface: '#1a1b22'
  on-surface-variant: '#46464b'
  inverse-surface: '#2f3038'
  inverse-on-surface: '#f1effa'
  outline: '#76777b'
  outline-variant: '#c7c6cb'
  surface-tint: '#5e5e62'
  primary: '#000000'
  on-primary: '#ffffff'
  primary-container: '#1b1b1f'
  on-primary-container: '#848387'
  inverse-primary: '#c7c6ca'
  secondary: '#526600'
  on-secondary: '#ffffff'
  secondary-container: '#caf233'
  on-secondary-container: '#576c00'
  tertiary: '#000000'
  on-tertiary: '#ffffff'
  tertiary-container: '#0b1c30'
  on-tertiary-container: '#75859d'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#e3e2e6'
  primary-fixed-dim: '#c7c6ca'
  on-primary-fixed: '#1b1b1f'
  on-primary-fixed-variant: '#46464a'
  secondary-fixed: '#caf233'
  secondary-fixed-dim: '#afd505'
  on-secondary-fixed: '#171e00'
  on-secondary-fixed-variant: '#3d4c00'
  tertiary-fixed: '#d3e4fe'
  tertiary-fixed-dim: '#b7c8e1'
  on-tertiary-fixed: '#0b1c30'
  on-tertiary-fixed-variant: '#38485d'
  background: '#fbf8ff'
  on-background: '#1a1b22'
  surface-variant: '#e3e1ec'
  radar-lime: '#C8F031'
  radar-lime-dim: '#AECF28'
  radar-lime-surface: '#F6FDE8'
  ink-primary: '#121316'
  ink-secondary: '#27272A'
  ink-muted: '#52525B'
  border-subtle: '#E4E4E7'
  border-strong: '#D4D4D8'
  surface-canvas: '#F8FAFC'
  surface-card: '#FFFFFF'
  delta-positive: '#15803D'
  delta-negative: '#DC2626'
typography:
  display-lg:
    fontFamily: Geist
    fontSize: 36px
    fontWeight: '600'
    lineHeight: 44px
    letterSpacing: -0.03em
  display-lg-mobile:
    fontFamily: Geist
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Geist
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Geist
    fontSize: 20px
    fontWeight: '500'
    lineHeight: 28px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Geist
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 24px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Geist
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: -0.005em
  body-md:
    fontFamily: Geist
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0em
  body-sm:
    fontFamily: Geist
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
    letterSpacing: 0em
  data-price-lg:
    fontFamily: Geist Mono
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.02em
  data-price-md:
    fontFamily: Geist Mono
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: -0.01em
  data-meta:
    fontFamily: Geist Mono
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
    letterSpacing: 0em
  label-caps:
    fontFamily: Geist Mono
    fontSize: 10px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.06em
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-sm: 0.5rem
  gutter-lg: 1.5rem
  margin: 1rem
  margin-tablet: 1.5rem
  margin-desktop: 2.5rem
  space-2xs: 0.125rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 0.75rem
  space-lg: 1rem
  space-xl: 1.5rem
  space-2xl: 2rem
  space-3xl: 3rem
---

## Brand & Style

This design system embodies a utility-first, analytical apparatus for smart consumers navigating the Turkish e-commerce landscape. The aesthetic avoids e-commerce retail tropes, hyperactive sales badges, and aggressive promotional gradients in favor of an austere, high-density financial instrument interface.

### Brand Personality & Emotional Posture
- **Analytical & Precise:** Information is laid out with surgical accuracy. Data integrity outranks visual decoration.
- **Calm & Objective:** The product operates as an impartial observer. It does not push deals; it reports market facts.
- **Tool-Like Utility:** Fast response, crisp visual containers, and explicit typographic hierarchy create an experience closer to an engineering terminal or financial dashboard than a discount forum.

### Aesthetic Methodology
The aesthetic combines **Minimalism** and **Modern Functional Utility**. Surfaces rely on cool slate and neutral cool greys, providing a structured canvas where the single chromatic highlight—an electric, optical-clarity Radar Lime—acts exclusively as an alert state for genuine price reductions, historical minimums, and positive delta values.

### Copywriting & Linguistic Rules
- **Locale:** Turkish (`tr-TR`). Use lowercase dotted `i` and uppercase dotted `İ` accurately. Price format conforms strictly to Turkish standards: `1.499,90 TL`.
- **Punctuation:** Never use em-dashes (`—`) or en-dashes (`–`). Use simple hyphens (`-`) or colons (`:`) where separators are strictly required.
- **Imagery & Iconography:** Zero emoji. Iconography strictly adheres to Phosphor Icons in Regular and Bold weights (geometric, uniform 1.5px stroke at 16px size).

## Colors

The color palette is deliberately disciplined. It departs from standard Turkish shopping aggregators saturated with retail blue and magenta, relying on a mono-tonal foundation accented by a high-visibility utility green.

### Role Assignments
- **Primary Ink (`#121316`):** The primary structural anchor. Serves as all light-mode call-to-action buttons, high-emphasis text, table headers, and deep structural lines.
- **Radar Lime (`#C8F031`):** Strictly functional. Used exclusively for:
  1. Historical low prices and drop alerts.
  2. Numerical badges indicating "En Ucuz" (Lowest Price).
  3. Interactive Primary CTA states when rendering inside an explicit dark-mode context.
- **Tertiary Slate (`#64748B`):** Context labels, table metadata, store review counts, and auxiliary timestamps.
- **Neutral Canvas (`#F8FAFC` to `#FFFFFF`):** Cool neutral background surfaces providing subtle delineation without visual clutter.

### Prohibitions
- No purple, magenta, or blue gradient transitions.
- Radar Lime must never be used for standard navigational headers, decorative borders, or non-deal content.

## Typography

Typographic hierarchy enforces the separation between interface structure and computational data.

### Typeface Allocation
- **UI Structure & Copy:** Geist. Clean, neutral, highly legible in compact table densities and complex metadata columns.
- **Financial & Temporal Metrics:** Geist Mono. Used strictly for:
  - All currency prices (`TL`).
  - Delta percentages (`%12 düşüş`).
  - Store counts (`14 satıcı`).
  - Timestamps, crawl records, and dates (`Bugün 14:02`, `18.10.2024`).

### Tabular Formatting
All numeric tokens in Geist Mono must use `font-variant-numeric: tabular-nums` to ensure that financial tables, price tracking columns, and delta comparisons align vertically down the decimal mark.

## Layout & Spacing

The layout is built upon an 8-point rhythmic grid with a 4-point micro-scale for compact data components. The grid adapts to high-density listing demands.

### Breakpoints & Grid Composition
- **Mobile (`< 640px`):** Single column stream or 2-column comparison matrix. Outer margins are `1rem` (16px) with `0.5rem` (8px) gutters.
- **Tablet (`640px - 1024px`):** 6 or 8-column layout with `1.5rem` (24px) margins and `1rem` (16px) gutters.
- **Desktop (`> 1024px`):** 12-column layout. Max container width capped at `1280px` centered, ensuring wide data tables maintain optical scanning comfort. Section margins expand to `2.5rem` (40px).

### Density Patterns
Data tables prioritize horizontal compactness over vertical height. Cell padding adheres strictly to `space-sm` (8px) vertically and `space-md` (12px) horizontally to maximize row counts above the fold.

## Elevation & Depth

This design system avoids heavy shadows, blur treatments, and multi-layered skeuomorphism. It implements depth through tonal layering and low-contrast outlines.

### Architectural Surface Tiers
- **Tier 0 (Base Canvas):** `#F8FAFC`. The foundational backdrop for the entire viewport.
- **Tier 1 (Panels & Cards):** `#FFFFFF`. Defined by a 1px border (`#E4E4E7`). No drop shadows on static containers.
- **Tier 2 (Popovers, Tooltips, Search Autocomplete):** `#FFFFFF` paired with an ultra-subtle, non-diffuse bounding shadow: `0 4px 12px -2px rgba(18, 19, 22, 0.06), 0 1px 3px 0 rgba(18, 19, 22, 0.04)`.
- **Tier 3 (Modals & Overlays):** Grounded by a backdrop scrim tinted with `#121316` at 40% opacity.

### Outlines
All cards, inputs, and search modules use a 1px solid border. Active/focused borders escalate to `#121316` without changing layout geometry (using standard 1px box-sizing).

## Shapes

The geometric framework uses calibrated corner radii that maintain a consistent hierarchy across nested containers.

### Radii Hierarchy
- **Inputs & Interactive Controls:** Exactly `10px`. Ensures search bars, filters, and standard buttons look unified.
- **Product Tiles & Listing Units:** Exactly `12px`. Bridges smaller controls and larger surface cards.
- **Container Cards & Data Tables:** Exactly `14px`. Outer bounding containers provide a clean, distinct frame.
- **Badges, Chips & Meta Pills:** `9999px` (Full Pill). Differentiates meta flags (e.g., "Kargo Bedava", "Son 30 Günün En Düşüğü") from interactive touch targets.

## Components

### Search Bar (Global Anchor)
The primary entry point of the interface.
- Height: 52px on desktop, 44px on mobile.
- Background: `#FFFFFF`, border: 1px solid `#D4D4D8`, corner radius: `10px`.
- Left icon: Phosphor `MagnifyingGlass` (18px, `#71717A`).
- Focus state: Border transitions to `#121316`, zero focus ring blur.
- Keyboard shortcut indicator on desktop: Geist Mono pill (`⌘K` or `ESC`) floating on the right edge.

### Buttons
- **Primary CTA:** Solid `#121316` background, `#FFFFFF` text. Radius: `10px`. Hover: `#27272A`. Active: `#000000`.
- **Secondary / Action:** `#FFFFFF` background, 1px border `#E4E4E7`, `#121316` text. Hover: `#F1F5F9`.
- **Radar Action (Contextual Deal Trigger):** Solid `#C8F031` background, `#121316` bold text. Radius: `10px`. Reserved strictly for direct navigation to the absolute lowest verified price or price-drop alerts.

### Price Chips & Badges
- **Pill Shape:** Fully rounded (`9999px`), internal padding `2px 8px`.
- **Lowest Price Alert ("En Ucuz"):** Background `#C8F031`, text `#121316`, Geist Mono 11px Bold.
- **Neutral Attribute Pill:** Background `#F1F5F9`, text `#52525B`, Geist Mono 11px Regular.
- **Delta Negative (Price Increased):** Background `#FEE2E2`, text `#DC2626`.
- **Delta Positive (Price Dropped):** Background `#DCFCE7`, text `#15803D`.

### Data Comparison Cards & Merchant Rows
- **Card Enclosure:** 14px radius, white surface, 1px `#E4E4E7` border.
- **Merchant List Rows:** Border-bottom 1px `#F1F5F9`. No hover elevation; use subtle background shift to `#F8FAFC`.
- **Price Column:** Geist Mono 16px Bold, tabular numbers, aligned right. Store names render in Geist 14px Medium.

### Input Fields & Select Boxes
- Height: 40px. Radius: `10px`.
- Typography: Geist 14px.
- Background: `#FFFFFF`. Border: 1px solid `#E4E4E7`.

### Price Sparklines & History Graphs
- Monochromatic line plots in `#121316` (1.5px stroke width).
- Baseline reference area lightly filled with `#F1F5F9`.
- Data points at historic lows marked with a 4px filled circle in `#C8F031` with a 1px `#121316` stroke.