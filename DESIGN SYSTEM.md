# APP DESIGN STANDARD — iOS 26 INSPIRED

## 1. Objective

Design and implement a complete, consistent, production-quality user interface inspired by Apple's iOS design language.

The design must feel:
- Minimalist, elegant, clean, and carefully composed.
- Content-first, with clear visual hierarchy.
- Refined through spacing, typography, geometry, materials, and motion.
- Consistent across every screen and component.
- Native to the platform on which the application runs.

Use Apple's Human Interface Guidelines as the primary visual reference.

Do not interpret this specification as a request to add blur, pill shapes, or rounded corners everywhere.

## 2. Mandatory Design Principles

### A. Navigation
- Use a floating bottom navigation bar when appropriate for the application.
- Keep navigation visually separate from the main content.
- Use simple monochrome icons and concise labels.
- Show the selected destination using a clear, restrained state.
- Support safe areas and device corner geometry.
- Allow navigation to minimize or adapt while scrolling where appropriate.
- Never cover important content or essential controls.
- Use top floating toolbars for contextual actions when appropriate.

### B. Buttons and Controls
- Use circular icon buttons for compact, standalone actions.
- Use capsule-shaped buttons for primary actions and suitable grouped controls.
- Use rounded rectangles for cards, input fields, and content containers where appropriate.
- Maintain consistent component dimensions and internal spacing.
- Provide visible default, pressed, focused, selected, disabled, and loading states when relevant.
- Give icon buttons accessible labels.
- Do not make every button circular or every action a pill.

### C. Materials and Surfaces
- Prefer solid or standard material surfaces for content, cards, and primary UI regions.
- Use subtle elevation (light shadows) only where it clarifies hierarchy.
- Keep foreground text and icons highly readable in both light and dark mode.
- Avoid excessive blur, reflections, gradients, glow, and decorative borders.
- Provide an accessible appearance when motion is reduced.

### D. Typography and Layout
- Use the platform's appropriate system font.
- Establish a clear hierarchy for page titles, section headings, body text, supporting labels, and metadata.
- Use generous but purposeful whitespace.
- Apply consistent alignment and content margins.
- Prefer simple surfaces over nested cards.
- Keep text readable and layouts responsive.
- Support dark mode, light mode, larger text, localization, and right-to-left layouts when required.

### E. Motion and Interaction
- Use fluid, natural transitions and restrained spring animations.
- Apply small press-scale changes to suitable interactive controls.
- Use transitions that communicate state changes and preserve spatial continuity.
- Avoid excessive bounce, elastic motion, parallax, and animation for decoration.
- Respect reduced-motion settings.
- Keep interactions responsive and avoid blocking the interface during animation.

## 3. Starting Design Tokens

Create one central token system. Adapt the values to the target platform and actual interface.

Spacing:
- 4 units: micro spacing
- 8 units: compact spacing
- 12 units: component spacing
- 16 units: standard spacing
- 24 units: section spacing
- 32 units: major separation

Geometry:
- Small corner radius: 8 units
- Standard surface radius: 16 units
- Large surface radius: 24 units
- Capsule radius: maximum available radius
- Circular controls: equal width and height

Controls:
- Minimum touch target: 44 × 44 pt on iOS / 48 dp on Android.
- Primary button height: approximately 48–52 pt/dp.
- Circular icon button: approximately 44–48 pt/dp.
- Icon size: approximately 20–24 pt/dp.
- Floating navigation: approximately 64–72 pt/dp tall, adjusted for safe areas and platform conventions.

Typography:
- Use semantic text styles rather than arbitrary sizes.
- Prefer medium or semibold weights for headings.
- Use regular weights for body text.
- Reserve heavy weights for intentional emphasis.

Colors:
- Use semantic background, surface, text, separator, accent, and status colors.
- Support light and dark appearances.
- Maintain accessible contrast.
- Keep the default palette neutral and introduce accent color with restraint.

Elevation and materials:
- Prefer subtle shadows over strong elevation.
- Use solid surfaces for most content.
- Never sacrifice readability for visual effects.

Motion:
- Use short, responsive feedback animations.
- Prefer spring-based transitions for suitable navigation and component interactions.
- Avoid imposing one animation curve on every interaction.

These are project-level starting values, not Apple's official internal design-token measurements. Centralize them so that they can be tuned consistently.

## 4. Reusable Component Architecture

Implement a small, reusable component library appropriate to the chosen framework.

Consider components such as:
- FloatingNavigationBar
- FloatingToolbar
- CircularIconButton
- CapsuleButton
- StandardSurface
- AdaptiveSheet
- SearchField
- SegmentedControl
- LoadingState
- EmptyState
- ErrorState

Do not create components merely to satisfy this list. Build the components the application actually needs.

Each component must use shared design tokens and consistent interaction rules.

## 5. Engineering Requirements

Before changing the application:

1. Inspect the repository, framework, architecture, existing screens, and dependencies.
2. Identify the target platforms and supported screen sizes.
3. Understand the current application features and preserve working behavior.
4. Create a concise design-system specification before implementing the UI.

During implementation:

1. Implement shared tokens and components first.
2. Use native platform components when they provide the correct behavior and appearance.
3. Use custom components only when necessary.
4. Keep business logic separate from presentation.
5. Avoid unnecessary dependencies and duplicated styles.
6. Do not replace functioning features with static mockups.

## 6. Visual Quality Assurance

Review every important screen after implementation.

Check:
- Spacing, alignment, typography, and visual hierarchy.
- Floating navigation placement and safe-area handling.
- Surface and text readability in light and dark backgrounds.
- Circular and capsule control consistency.
- Loading, empty, error, and disabled states.
- Small screens, large text, and landscape layouts where supported.
- Dark mode, contrast, keyboard navigation, and touch targets.
- Animation quality and reduced-motion behavior.

Build and run the application using the available tools. Inspect screenshots or actual running screens, correct visual defects, and repeat the review.

Do not declare the design complete merely because the code compiles.

## 7. Acceptance Criteria

The implementation is complete when:
- Every screen follows the same design language.
- The components and tokens are reused consistently.
- Navigation and interactive controls behave correctly.
- The interface remains readable and accessible.
- Existing required functionality continues to work.
- The application builds successfully and major visual issues have been checked.

The final result must look intentionally designed, not like a collection of unrelated AI-generated screens.

## 8. Required Deliverables

Produce:
1. A documented design system.
2. Centralized design tokens.
3. Reusable UI components.
4. Implemented application screens.
5. A concise summary of changes, validation results, and known limitations.

Make reasonable design decisions independently. Do not ask for approval for every small visual detail. Prioritize usability, consistency, simplicity, accessibility, and maintainability.
