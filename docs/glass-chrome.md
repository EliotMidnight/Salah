# Glass chrome

Glass in Salah is **chrome only**. The floating dock is the one surface that
samples what is behind it; pages, cards, rows, fields, sheets and text stay
opaque. This is the same rule as the iOS 26 reference, adapted to Compose and to
`minSdk 24`: a glass material is a navigation layer, not a decoration for
content.

## Why not transparent content

A translucent card makes the reader's eye work to separate text from whatever
is moving underneath it. It also makes contrast dependent on the page behind it,
which is impossible to prove for every scroll position. Content therefore keeps
the existing semantic `surface`; only the dock has a backdrop.

There is no glass-on-glass: the dock is the only element that reads
`LocalGlassBackdrop`, and content is never wrapped in `GlassSurface`.

## The gate

One function, `glassEnabled` in `ui/theme/Glass.kt`, decides whether the
material is allowed:

| Input | Source | Effect |
| --- | --- | --- |
| `Glass.ENABLED` | build flag, default `true` | `false` ships the opaque chrome |
| SDK >= 31 | `Build.VERSION.SDK_INT` | below 31 there is no `RenderEffect` blur |
| Reduce Transparency | app setting, persisted as `pref_reduce_transparency` | forces opaque chrome |
| High contrast text | system accessibility setting | forces opaque chrome and a 2 dp border |

The app root resolves the gate once and provides `LocalGlassEnabled` and
`LocalHighContrast`. A call site never decides for itself. The default for
`LocalGlassEnabled` is `false`, so previews and tests that do not opt in get the
safe path.

Android has no public system "Reduce Transparency" signal, so the app exposes
its own switch in **Settings → Appearance**. It is shown only when the build and
platform can actually render glass (`Glass.available`); on older devices the
setting would have no visible effect. The choice is persisted as
`pref_reduce_transparency` and is cleared by **Reset all settings**, like the
theme and accent.

High contrast text is read on every resume:

- API 36+ uses `AccessibilityManager.isHighContrastTextEnabled()`.
- API 31–35 uses the readable secure setting
  `high_text_contrast_enabled`, because the public API does not exist there.

The opaque path is not a second layout. It is the same `SalahDock`, same
`GlassChromeScaffold`, same touch targets and same insets; only `GlassSurface`
paints `surface` instead of the captured backdrop.

## Layer anatomy

`GlassChromeScaffold` in `ui/components/GlassChrome.kt` owns the capture. The
content is drawn normally; during that draw it is also recorded into a
`GraphicsLayer` at the dock rectangle plus two blur radii of padding. Recording
only that slice keeps the per-frame fill cost bounded and still gives the blur
kernel pixels outside the dock edge.

`GlassSurface` then composes, in order:

1. **Backdrop** — the captured slice, blurred at `Glass.blurRegular` (24 dp),
   magnified by 1.5 % around the dock centre (a restrained lensing cue), with a
   `TileMode.Clamp` edge.
2. **Saturation** — a colour matrix at `Glass.saturation` (1.6), so the blurred
   colour does not turn grey.
3. **Tint body** — `surface` at `Glass.tintAlpha` (64 %). This is deliberately
   stronger than the iOS reference's 10–18 % film: dark labels need a floor that
   survives any backdrop. `GlassContrastTest` proves 4.5:1 for both light text on
   the darkest backdrop and dark text on the lightest.
4. **Specular edge** — a 1 dp white-to-black linear gradient stroke. The opaque
   path uses the semantic `outline`; high contrast uses a 2 dp `onSurface`-based
   border.
5. **Grain** — a deterministic 128 px, 3.5 % alpha tile. It breaks blur banding,
   never animates, and is seeded so two screenshot recordings are byte-identical.
6. **Shadow** — 20 dp ambient/spot shadow at 16 % black, outside the clip.

The dock content is painted after the material. The labels use `onSurface`; the
selected indicator is an opaque `primaryContainer` pill, so its icon/text
contrast is the theme's tested pairing rather than a contrast guess over the
blurred layer.

## Layout and safe areas

The dock is not a `Scaffold` `bottomBar`. It floats over full-height content in
`GlassChromeScaffold`, aligned bottom-centre, with:

- the navigation-bar inset applied to the dock container;
- `Glass.dockSideGap` (20 dp) and `Glass.dockBottomGap` (12 dp);
- a 420 dp maximum width, so it stays a floating control on tablets;
- 64 dp height, 56 × 32 dp selection pill, 22 dp icons.

Because the content is underneath rather than above, every scrolling screen adds
`dockContentPadding()` to its bottom padding. `ScreenScaffold` does this in one
place; `TodayScreen`, which paints itself rather than using that scaffold, calls
the same function. The value is the dock height + bottom gap + navigation-bar
inset + one 16 dp space, derived from the same tokens and the same window insets
as the dock.

The dock has no top bar and no nested material. The existing per-screen titles
are unchanged.

## Typography and accessibility

- Labels are the existing localized `navToday`, `navQibla` and `navSettings`.
  No new user-facing English was added for the dock.
- At `fontScale >= Glass.labelsAtFontScaleBelow` (1.4) the visible labels are
  dropped; each destination keeps its accessible name through
  `contentDescription` and remains a 64 dp tall target. This avoids ellipsised
  labels at the system's largest text sizes.
- Selection uses three channels: filled vs outlined icon, an opaque pill, and
  medium vs normal label weight. Colour is not the only state.
- Press feedback is a 0.97 scale spring, suppressed when reduced motion is on.
  The material itself is static; there is no decorative movement.
- The high-contrast path increases the border to 2 dp and removes the backdrop,
  so the dock is a solid `surface` against any content.

## Testing and recording

- `GlassGateTest` pins the truth table of the gate.
- `GlassContrastTest` computes WCAG contrast for the worst backdrop in both
  themes.
- `DockScreenshotTest` records five baselines over deliberately high-frequency
  content: glass light, glass dark, opaque light, opaque dark, and glass at 1.6×
  font scale. The bars behind the dock make it obvious whether the blur is
  actually sampling content or the dock has silently fallen back to opaque.

Record on the same x86_64 host used for the existing screenshots:

```sh
./tools/build.sh record
```

For only the dock while iterating:

```sh
./tools/build.sh recordRoborazziDebug --tests 'com.example.DockScreenshotTest'
```

Baselines live in `app/src/test/screenshots/` and are compared by the normal JVM
suite. The grain is seeded, so recording twice should not produce a diff.

## Known deviations from iOS 26

- **Tint strength**: 64 % instead of the reference's 10–18 %, for measurable
  contrast over arbitrary content. The material reads as a dense frosted bar,
  not clear glass.
- **Backdrop source**: the content is captured once into a layer and blurred,
  rather than a live system backdrop. It is accurate for scrolling and
  navigation, but it is a texture, so it does not refract or react to display
  HDR.
- **Lensing**: approximated with a 1.5 % scale, not a real displacement map.
- **Platform fallbacks**: blur requires API 31; the app still ships the same
  dock below it. High contrast text below API 36 is read from a hidden but
  readable secure setting, because no public API exists there.
- **Reduce Transparency**: an app setting, not a system signal; Android does not
  expose one to third-party apps.

Any future glass surface needs its own capture rectangle and layer; the shared
`GlassBackdropState` is sized to the dock and one surface at a time. Adding a
second glass surface without splitting the capture would make both surfaces
sample the same dock slice.
