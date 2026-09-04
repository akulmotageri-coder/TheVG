---
name: Cognitive Check-In System
colors:
  surface: '#f9f9ff'
  surface-dim: '#d7dae3'
  surface-bright: '#f9f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f1f3fd'
  surface-container: '#ebeef7'
  surface-container-high: '#e5e8f2'
  surface-container-highest: '#dfe2ec'
  on-surface: '#181c23'
  on-surface-variant: '#574144'
  inverse-surface: '#2d3138'
  inverse-on-surface: '#eef0fa'
  outline: '#8a7174'
  outline-variant: '#ddbfc2'
  surface-tint: '#a9324f'
  primary: '#7e0f31'
  on-primary: '#ffffff'
  primary-container: '#9e2a47'
  on-primary-container: '#ffbac3'
  inverse-primary: '#ffb2bc'
  secondary: '#056686'
  on-secondary: '#ffffff'
  secondary-container: '#94dbff'
  on-secondary-container: '#006180'
  tertiary: '#543a00'
  on-tertiary: '#ffffff'
  tertiary-container: '#725000'
  on-tertiary-container: '#f8c461'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffd9dd'
  primary-fixed-dim: '#ffb2bc'
  on-primary-fixed: '#400013'
  on-primary-fixed-variant: '#891838'
  secondary-fixed: '#c0e8ff'
  secondary-fixed-dim: '#88d0f4'
  on-secondary-fixed: '#001e2b'
  on-secondary-fixed-variant: '#004d66'
  tertiary-fixed: '#ffdea8'
  tertiary-fixed-dim: '#f2be5c'
  on-tertiary-fixed: '#271900'
  on-tertiary-fixed-variant: '#5e4200'
  background: '#f9f9ff'
  on-background: '#181c23'
  surface-variant: '#dfe2ec'
typography:
  headline-xl:
    fontFamily: plusJakartaSans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
  headline-xl-mobile:
    fontFamily: plusJakartaSans
    fontSize: 26px
    fontWeight: '700'
    lineHeight: 32px
  headline-lg:
    fontFamily: plusJakartaSans
    fontSize: 22px
    fontWeight: '700'
    lineHeight: 28px
  headline-md:
    fontFamily: plusJakartaSans
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  headline-sm:
    fontFamily: plusJakartaSans
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 22px
  body-lg:
    fontFamily: plusJakartaSans
    fontSize: 15px
    fontWeight: '400'
    lineHeight: 22px
  body-md:
    fontFamily: plusJakartaSans
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
  body-sm:
    fontFamily: plusJakartaSans
    fontSize: 11px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
  label-md:
    fontFamily: inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
  label-sm:
    fontFamily: inter
    fontSize: 10px
    fontWeight: '600'
    lineHeight: 14px
    letterSpacing: 0.04em
  display-stat:
    fontFamily: plusJakartaSans
    fontSize: 36px
    fontWeight: '800'
    lineHeight: 44px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  space-2xs: 0.25rem
  space-xs: 0.5rem
  space-sm: 0.75rem
  space-md: 1rem
  space-lg: 1.25rem
  space-xl: 1.5rem
  space-2xl: 2rem
  space-3xl: 2.5rem
  screen-edge: 1.25rem
  card-pad: 1.25rem
  bottom-nav-height: 4.5rem
---

## Brand & Style

The design system is engineered for cognitive assessments, neurological monitoring, and wellness check-ins. It bridges clinical rigor with accessible, non-threatening healthcare interfaces. By moving away from stark white medical software and hyper-saturated gaming tropes, the design fosters reassurance, mental clarity, and distraction-free focus.

The visual style is **Soft Warm Clinical Minimalism**:
- **Tone:** Empathetic, scientifically grounded, calm, and deliberate.
- **Visual Personality:** Soft tinted pastel card containers (blush, ice blue, pale butter) nested against an off-white background, framed with pill geometry and anchored by deep rich burgundy CTAs.
- **Mental Clarity:** Extreme visual hierarchy ensuring that participants experiencing cognitive fatigue or high stress can effortlessly locate action points, instructions, and test boundaries.

## Colors

The palette uses low-saturation, tint-washed surfaces paired with high-contrast text and a commanding wine/burgundy accent for focused user actions.

### Functional Roles
- **Primary Accent (`#9E2A47`):** Primary action buttons, active navigation indicators, key metrics, and completion highlights.
- **Secondary Accent (`#2B7A9B` / `#E9F4F9`):** Working memory tasks, digit span modules, and cognitive pace indicators.
- **Tertiary Accent (`#B88A2D` / `#FEF9E7`):** Visual scanning tasks, trail making challenges, cautionary feedback, and practice states.
- **Surface Background (`#FBFBFB` / `#F8F7F5`):** Universal warm canvas that reduces eye fatigue compared to pure clinical white.
- **Surface Containers:**
  - `Surface-Blush` (`#FDE8ED` / `#FCECEF`): Stroop assessments, executive function pills, backward recall blocks.
  - `Surface-Sky` (`#E9F4F9`): Digit span sequences, pacing cards, informational banners.
  - `Surface-Butter` (`#FEF9E7`): Trail making paths, warning tags, progress callouts.
  - `Surface-Elevated` (`#FFFFFF`): Interactive test canvases, numpads, modal sheets, and metric metric summary pods.
- **Text & Neutral Tones:**
  - `Text-Primary` (`#1E2229`): Heavy charcoal for headlines, prompt instructions, and keypad values.
  - `Text-Secondary` (`#737885`): Muted slate for helper copy, subtitles, and completion timestamps.
  - `Border-Subtle` (`#EAECEF`): Faint boundaries for interactive keypads and outlined buttons.

## Typography

The typography system relies on **Plus Jakarta Sans** for headlines and structural body copy to maintain open geometric letterforms that preserve legibility during high-stress tests. **Inter** is designated for utility labels, status chips, and numerical readouts where tracking and optical weight distribution must remain crisp.

Key typographical practices:
- Large stat callouts (such as completion percentages and completion times like `1m 42s` or `92%`) use `display-stat` with tight tracking and optical vertical centering.
- Prompt text during active tests ("YELLOW", "GREEN", "Remember the sequence") prioritizes strong character contrast with neutral `#1E2229` or test-specific stimulus colors.
- Disclaimers and secondary annotations maintain minimum 11px sizing (`body-sm`) to comply with healthcare accessibility standards.

## Layout & Spacing

The layout model is optimized for single-handed mobile assessment workflows:
- **Base Grid:** 4px micro-grid, with 8px increments applied to macro UI containers.
- **Horizontal Screen Padding:** 20px (`space-lg`) on mobile devices to preserve natural thumb zones while maximizing touch targets.
- **Screen Margins:** Fixed width on compact devices with content centered up to a max width of 480px on larger tablets and foldables.
- **Vertical Spacing Rhythm:** Grouped cards and sequence pods have 12px to 16px gaps; major task sections have 24px gaps; active test stages utilize auto-flex spacers to push the primary CTA toward the bottom navigation margin.

## Elevation & Depth

This design system eschews heavy drop shadows in favor of **Tonal Layers** and **Ultra-Soft Ambient Light**:

- **Layer 0 (Canvas):** Flat warm background `#FBFBFB`.
- **Layer 1 (Tinted Assessment Cards):** Flat pastel surfaces (`#FDE8ED`, `#E9F4F9`, `#FEF9E7`) without blur or shadow, separated by natural hue contrast.
- **Layer 2 (White Elevated Canvases & Numpads):** Pure `#FFFFFF` resting on Layer 1 or Layer 0, with a soft tinted drop shadow: `0 4px 16px -2px rgba(30, 34, 41, 0.05)`.
- **Layer 3 (Floating Bars & Bottom Navigation):** Elevated pill cards with subtle border `#EAECEF` and elevated diffusion: `0 8px 24px -4px rgba(30, 34, 41, 0.08)`.
- **Ghost Borders:** Low-contrast 1px outlines (`#EAECEF` or 10% opacity tints of the parent container) outline key input boxes and white cards to maintain structural visibility in bright environments.

## Shapes

The shape system blends friendly organic roundness with structural precision:
- **Major Cards & Feature Panels:** 20px to 24px corner radii (`rounded-2xl`), delivering a welcoming, tactile tablet feel.
- **Buttons, Pills & Badges:** `rounded-full` (9999px) is strictly enforced for primary CTA buttons, test duration indicators, trial step badges, and active tab highlights.
- **Keypad Buttons & Inputs:** 16px (`rounded-xl`) rounded rectangles to create generous tap affordances.
- **Stimulus Containers:** Large active test viewports use 20px radii with subtle interior gutters.

## Components

### 1. Primary & Secondary Buttons
- **Primary CTA:** Full-width pill (`height: 52px`, `rounded-full`), filled with deep burgundy `#9E2A47`, pure white typography (`label-lg`), and optional right-pointing directional arrow. Hover/active states darken by 6% (`#89223B`).
- **Secondary / Outlined Button:** Full-width pill (`height: 48px`, `rounded-full`), transparent background, 1.5px solid burgundy border `#9E2A47`, burgundy text.
- **Ghost Action:** Simple text-only centered button (`Text-Secondary`), used for secondary skips or "Need help?".

### 2. Category Cards & Test Banners
- **Anatomy:** 20px to 24px rounded rectangles containing an icon/badge top-left, estimated time pill top-right, title (`headline-md`), and a two-line description (`body-md`).
- **Surface Color Variants:**
  - *Stroop / Attention Card:* Soft Blush `#FDE8ED` with `#9E2A47` badge elements.
  - *Digit Span / Memory Card:* Soft Ice Blue `#E9F4F9` with `#2B7A9B` badge elements.
  - *Trail Making Card:* Soft Butter Yellow `#FEF9E7` with `#B88A2D` badge elements.

### 3. Bottom Navigation Bar
- **Anatomy:** Fixed at screen bottom, elevated `#FFFFFF` bar with a subtle border-top `#EAECEF`.
- **Layout:** Three primary destinations: *Home*, *Tests*, *Summary*.
- **Active State:** Deep burgundy circle/pill indicator with icon and contrasting white glyph. Inactive states feature `#737885` icons with 11px label.

### 4. Interactive Assessment Keypad
- **Numeric Matrix:** 3x4 grid for digit input. Keys are elevated white squircles (`min-height: 56px`, `border: 1px solid #EAECEF`, `rounded-xl`) featuring centered bold digits (`headline-lg`).
- **Input Display Field:** Elongated white pill or squircle displaying the entered character sequence with dashed empty states.

### 5. Metric & Result Cards
- **Score Pods:** Dual-metric horizontal split containers with small status pill at top ("Excellent Accuracy", "Average Range", "Stable").
- **Stat Readouts:** Huge typography (`display-stat`) paired with a circular percentage ring or progress graph.
- **Comparative Indicator:** Split comparative chips (e.g., "Part A: Numbers", "Part B: Mixed") featuring small time badges and error counters.

### 6. Test Header
- **Navigation Bar:** 56px height, containing a subtle circle or back chevron on the left, centered title (`headline-sm`, e.g., "Active Test" or "Test Results"), and an optional profile avatar icon or status badge on the right.