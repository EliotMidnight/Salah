#!/usr/bin/env python3
"""
Convert the Salah Icons themeable SVGs into Android VectorDrawables.

Salah Icons (CC BY 4.0) ship 149 icons in two forms:
  themeable/  colours written as var(--token, #default) - the ones we want
  baked/      the same art with colours baked in, for tools that do not do CSS

We use the themeable set because we can map its semantic colour tokens onto our
own palette, which is the whole reason the set is "themeable". Each SVG paints
with up to six tokens:

  --ink       strokes and line art        -> onSurfaceVariant
  --tone      the main filled shape       -> primary
  --paper     knock-out / inner detail    -> surface
  --halo      a background disc           -> DROPPED (see below)
  --soft-line faint construction lines   -> outlineVariant
  --mid-line  mid-weight construction     -> outline

Two deliberate decisions:

1. The `--halo` circle is dropped. Every icon carries a baked 88r background
   disc sized for a filled tile. We want the glyph on our own surfaces, and a
   hardcoded disc would fight the light/dark surfaces it sits on.

2. Light and dark are emitted as two drawable folders rather than one tinted
   drawable. A single drawable would have to be tinted with a single colour,
   which would flatten these two-tone icons into a silhouette and throw away
   what makes the set look considered. Two folders keeps the ink/tone/paper
   relationship intact per theme.

Usage:
  python3 tools/convert_salah_icons.py [--icons DIR] [--out app/src/main/res]
"""

import argparse
import os
import re
import sys
import xml.etree.ElementTree as ET

SVG_NS = "{http://www.w3.org/2000/svg}"

# Salah Icons' six semantic colour tokens, mapped onto the app palette.
# Values are the *dark* palette from Color.kt; the light pass overrides below.
DARK_TOKENS = {
    "ink": "#9AA8B6",       # TextSecondaryDark  - strokes
    "tone": "#7DD3FC",      # AccentDark         - main fill
    "paper": "#121820",     # SurfaceDark        - knock-out detail
    "soft-line": "#232C37",  # OutlineVariantDark
    "mid-line": "#55636F",   # OutlineDark
}

LIGHT_TOKENS = {
    "ink": "#475569",       # TextSecondaryLight
    "tone": "#0369A1",      # AccentLight
    "paper": "#FFFFFF",     # SurfaceLight
    "soft-line": "#E4E9EF",  # OutlineVariantLight
    "mid-line": "#7E8B99",   # OutlineLight
}

# The icons we actually ship. Chosen for one clear meaning each; the full set is
# 149 and dropping all of it in would be the icon spam the design brief warns
# against. Categories we have no screen for (eid, wudu, dua, ramadan) are
# omitted deliberately - they can be added when a screen needs them.
WANTED = {
    # times/ - the core of the app
    "times/fajr", "times/sunrise", "times/dhuhr", "times/asr",
    "times/maghrib", "times/isha", "times/next-prayer-countdown",
    "times/prayer-alarm", "times/prayer-streak", "times/prayer-timetable",
    "times/location-pin", "times/location-for-times", "times/hijri-calendar",
    "times/sun-path", "times/last-third-of-night", "times/prayed-on-time",
    "times/moon-sighting",
    # quran/
    "quran/surah-list", "quran/ayah-marker", "quran/bookmark",
    "quran/bookmark-closed", "quran/search-ayah", "quran/mushaf",
    "quran/juz-progress", "quran/khatm", "quran/recitation",
    "quran/translation", "quran/daily-reading-goal",
    # mosque/
    "mosque/kaaba", "mosque/qiblah-compass", "mosque/qiblah-direction",
    "mosque/time-and-direction", "mosque/mosque",
    # salah/
    "salah/five-daily-prayers", "salah/prayer-tracker", "salah/niyyah",
    "salah/sujud", "salah/ruku", "salah/takbir", "salah/taslim",
    # ramadan/ - only the ones the Prayer screen's night periods reference
    "ramadan/crescent", "ramadan/imsak", "ramadan/lantern",
}

VAR_RE = re.compile(r"var\(\s*--([a-z-]+)\s*,\s*(#[0-9A-Fa-f]{3,8})\s*\)")
ATTR_RE = re.compile(r"\bvar\(\s*--[a-z-]+\s*,", )


def resolve(value, tokens):
    """Replace every var(--token, #default) in an attribute with a concrete colour."""
    if value is None:
        return None
    if "var(--halo" in value:
        return None  # signal: drop this element

    def sub(match):
        name, fallback = match.group(1), match.group(2)
        return tokens.get(name, fallback)

    return VAR_RE.sub(sub, value)


def hex_to_argb(colour):
    """#RGB / #RRGGBB / #AARRGGBB -> #AARRGGBB for Android."""
    c = colour.lstrip("#")
    if len(c) == 3:
        c = "".join(ch * 2 for ch in c)
    if len(c) == 6:
        c = "FF" + c
    if len(c) != 8:
        raise ValueError(f"cannot parse colour {colour}")
    return "#" + c.upper()


# Shapes we convert losslessly into <path> arc commands.
CONVERTIBLE_TAGS = {"path", "g", "circle", "ellipse", "rect"}
# Things we cannot represent without changing the artwork. These are a hard
# error rather than a skip: silently dropping geometry from an icon is worse
# than not shipping it, because nobody would notice until it was on screen.
UNSUPPORTED_TAGS = {"line", "polyline", "polygon", "linearGradient",
                    "radialGradient", "stop", "use", "image", "text",
                    "mask", "filter", "marker", "pattern", "style", "defs"}


class Converter:
    def __init__(self, tokens, viewport=240.0):
        self.tokens = tokens
        self.viewport = viewport
        self.output = []
        self.warnings = []

    def convert_group(self, element, depth=0, inherited=None):
        indent = "    " * depth
        inherited = dict(inherited or {})

        # These icons put fill/stroke/stroke-width on a wrapping <g> and rely on
        # SVG inheritance for the children. Android VectorDrawables have no
        # inheritance, so the values have to be pushed down explicitly or all the
        # line art silently disappears.
        for key in ("fill", "stroke", "stroke-width", "stroke-linecap", "stroke-linejoin"):
            if element.get(key) is not None:
                inherited[key] = element.get(key)
        for child in element:
            tag = child.tag.replace(SVG_NS, "")
            if tag in UNSUPPORTED_TAGS:
                raise ConversionError(
                    f"<{tag}> cannot be represented in a VectorDrawable without "
                    f"changing the artwork; refusing to ship a mangled icon")
            if tag not in CONVERTIBLE_TAGS:
                raise ConversionError(f"unexpected <{tag}> element")

            if tag == "g":
                # A <g> can only be expressed as a <group> if it carries
                # attributes; otherwise it is pure nesting.
                attrs = self.group_attrs(child)
                if attrs:
                    self.output.append(f"{indent}<group")
                    for k, v in attrs.items():
                        self.output.append(f'{indent}    {k}="{v}"')
                    self.output.append(f"{indent}    >")
                    self.convert_group(child, depth + 2, inherited)
                    self.output.append(f"{indent}    </group>")
                else:
                    self.convert_group(child, depth + 1, inherited)
                continue

            if tag == "circle":
                cx = float(child.get("cx", 0)); cy = float(child.get("cy", 0))
                r = float(child.get("r", 0))
                if r <= 0:
                    return
                d = (f"M{_num(cx - r)},{_num(cy)} "
                     f"a{_num(r)},{_num(r)} 0 1,0 {_num(2 * r)},0 "
                     f"a{_num(r)},{_num(r)} 0 1,0 {_num(-2 * r)},0Z")
                self.emit_path(child, d, indent, is_fill=True, inherited=inherited)
                continue

            if tag == "ellipse":
                cx = float(child.get("cx", 0)); cy = float(child.get("cy", 0))
                rx = float(child.get("rx", 0)); ry = float(child.get("ry", 0))
                if rx <= 0 or ry <= 0:
                    return
                d = (f"M{_num(cx - rx)},{_num(cy)} "
                     f"a{_num(rx)},{_num(ry)} 0 1,0 {_num(2 * rx)},0 "
                     f"a{_num(rx)},{_num(ry)} 0 1,0 {_num(-2 * rx)},0Z")
                self.emit_path(child, d, indent, is_fill=True, inherited=inherited)
                continue

            if tag == "rect":
                x = float(child.get("x", 0)); y = float(child.get("y", 0))
                w = float(child.get("width", 0)); h = float(child.get("height", 0))
                rx = float(child.get("rx", 0) or 0)
                if w <= 0 or h <= 0:
                    return
                if rx > 0:
                    rx = min(rx, w / 2, h / 2)
                    d = (f"M{_num(x + rx)},{_num(y)} "
                         f"H{_num(x + w - rx)} A{_num(rx)},{_num(rx)} 0 0,1 {_num(x + w)},{_num(y + rx)} "
                         f"V{_num(y + h - rx)} A{_num(rx)},{_num(rx)} 0 0,1 {_num(x + w - rx)},{_num(y + h)} "
                         f"H{_num(x + rx)} A{_num(rx)},{_num(rx)} 0 0,1 {_num(x)},{_num(y + h - rx)} "
                         f"V{_num(y + rx)} A{_num(rx)},{_num(rx)} 0 0,1 {_num(x + rx)},{_num(y)}Z")
                else:
                    d = (f"M{_num(x)},{_num(y)} H{_num(x + w)} V{_num(y + h)} "
                         f"H{_num(x)}Z")
                self.emit_path(child, d, indent, is_fill=True, inherited=inherited)
                continue

            if tag == "path":
                self.convert_path(child, indent, inherited)

    def group_attrs(self, element):
        out = {}
        transform = element.get("transform")
        if transform and "translate" in transform:
            # Only the translate(x y) form is used, and only with no rotation.
            nums = re.findall(r"-?\d+\.?\d*", transform)
            if len(nums) >= 2 and "rotate" not in transform:
                out["android:translateX"] = _num(nums[0])
                out["android:translateY"] = _num(nums[1])
        return out

    def convert_path(self, element, indent, inherited=None):
        d = element.get("d")
        if not d:
            return
        self.emit_path(element, d, indent, is_fill=False, inherited=inherited)

    def emit_path(self, element, d, indent, is_fill, inherited=None):
        inherited = inherited or {}

        def attr(name):
            return element.get(name, inherited.get(name))

        raw_fill = attr("fill")
        raw_stroke = attr("stroke")
        sw = attr("stroke-width")

        # The baked background disc. Dropped deliberately: it is a hardcoded
        # opaque circle sized for a filled tile, and it would sit wrongly on our
        # own light and dark surfaces.
        if "var(--halo" in (raw_fill or "") or "var(--halo" in (raw_stroke or ""):
            return

        fill = resolve(raw_fill, self.tokens)
        stroke = resolve(raw_stroke, self.tokens)

        # A shape with no fill of its own inside a stroked group is meant to be a
        # line, not a filled blob - that is how these SVGs draw their line art.
        if raw_fill is None and raw_stroke not in (None, "none"):
            fill_colour = "@android:color/transparent"
        elif fill and fill != "none":
            fill_colour = hex_to_argb(fill)
        else:
            fill_colour = "@android:color/transparent"

        if stroke and stroke != "none":
            stroke_colour = hex_to_argb(stroke)
            width = sw or "2.4"
        else:
            stroke_colour = "@android:color/transparent"
            width = "0"

        self.output.append(f"{indent}<path")
        self.output.append(f'{indent}    android:fillColor="{fill_colour}"')
        self.output.append(f'{indent}    android:strokeColor="{stroke_colour}"')
        self.output.append(f'{indent}    android:strokeWidth="{width}"')
        self.output.append(f'{indent}    android:strokeLineCap="round"')
        self.output.append(f'{indent}    android:strokeLineJoin="round"')
        self.output.append(f'{indent}    android:pathData="{d}" />')


class ConversionError(Exception):
    pass


def _num(value):
    f = float(value)
    return str(int(f)) if f == int(f) else str(f)


def convert_one(svg_path, tokens):
    tree = ET.parse(svg_path)
    root = tree.getroot()
    viewbox = root.get("viewBox", "0 0 240 240").split()
    w, h = float(viewbox[2]), float(viewbox[3])

    conv = Converter(tokens, viewport=w)
    conv.convert_group(root)

    body = "\n".join(conv.output)
    if not body.strip():
        return None, conv.warnings

    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        "<!-- Salah Icons (CC BY 4.0) - generated by tools/convert_salah_icons.py -->\n"
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        f'    android:width="24dp"\n'
        f'    android:height="24dp"\n'
        f'    android:viewportWidth="{_num(w)}"\n'
        f'    android:viewportHeight="{_num(h)}">\n'
        f"{body}\n"
        "</vector>\n"
    ), conv.warnings


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--icons", default="/home/eliot/Downloads/salah-icons")
    ap.add_argument("--out", default="app/src/main/res")
    args = ap.parse_args()

    src = os.path.join(args.icons, "themeable")
    if not os.path.isdir(src):
        sys.exit(f"themeable set not found at {src}")

    made, skipped, warned = 0, 0, 0
    for rel in sorted(WANTED):
        svg = os.path.join(src, rel + ".svg")
        if not os.path.isfile(svg):
            print(f"  MISSING {rel}")
            skipped += 1
            continue
        # Android resource names allow only [a-z0-9_]; the source set uses
        # hyphens ("qiblah-compass"), so they become underscores.
        name = "salah_" + rel.replace("/", "_").replace("-", "_")

        for folder, tokens in (("drawable", LIGHT_TOKENS), ("drawable-night", DARK_TOKENS)):
            out_dir = os.path.join(args.out, folder)
            os.makedirs(out_dir, exist_ok=True)
            xml, warns = convert_one(svg, tokens)
            if warns:
                warned += 1
                for w in dict.fromkeys(warns):
                    print(f"  warn {rel}: {w}")
            if xml is None:
                continue
            with open(os.path.join(out_dir, name + ".xml"), "w") as fh:
                fh.write(xml)
        made += 1

    print(f"\nconverted {made} icons ({made * 2} drawables), skipped {skipped}, {warned} with warnings")
    if made:
        print("reminder: the Salah Icons set is CC BY 4.0 and requires credit.")


if __name__ == "__main__":
    main()
