"""Deterministic food illustration helpers."""

from __future__ import annotations

import hashlib
import html
import urllib.parse


PALETTES = [
    ("#1f2937", "#f97316", "#fed7aa", "#fff7ed"),
    ("#111827", "#22c55e", "#bbf7d0", "#f0fdf4"),
    ("#18181b", "#eab308", "#fef3c7", "#fefce8"),
    ("#172554", "#38bdf8", "#bae6fd", "#eff6ff"),
    ("#3b0764", "#d946ef", "#f5d0fe", "#fdf4ff"),
    ("#450a0a", "#ef4444", "#fecaca", "#fff1f2"),
    ("#0f172a", "#14b8a6", "#99f6e4", "#f0fdfa"),
    ("#292524", "#a3e635", "#d9f99d", "#f7fee7"),
]


SHAPES = [
    "bowl",
    "plate",
    "cup",
    "steam",
    "noodle",
    "dessert",
]


def stable_int(text: str) -> int:
    return int(hashlib.sha256(text.encode("utf-8")).hexdigest()[:12], 16)


def short_label(name: str) -> str:
    clean = "".join(ch for ch in name.strip() if not ch.isspace())
    if not clean:
        return "美食"
    return clean[:4]


def shape_markup(shape: str, accent: str, soft: str, light: str) -> str:
    if shape == "cup":
        return f"""
        <rect x="230" y="120" width="140" height="118" rx="28" fill="{light}" opacity=".98"/>
        <path d="M364 146h42c28 0 43 18 37 45-6 26-27 39-59 37" fill="none" stroke="{soft}" stroke-width="16" stroke-linecap="round"/>
        <path d="M250 105c18-28 3-42 23-66M305 105c18-28 3-42 23-66M358 105c18-28 3-42 23-66" fill="none" stroke="{accent}" stroke-width="9" stroke-linecap="round" opacity=".82"/>
        """
    if shape == "plate":
        return f"""
        <ellipse cx="300" cy="196" rx="166" ry="72" fill="{light}" opacity=".98"/>
        <ellipse cx="300" cy="196" rx="108" ry="40" fill="{soft}" opacity=".85"/>
        <circle cx="260" cy="186" r="20" fill="{accent}"/>
        <circle cx="314" cy="204" r="15" fill="{accent}" opacity=".76"/>
        <path d="M376 162c-24 36-74 46-126 18" fill="none" stroke="#ffffff" stroke-width="10" stroke-linecap="round" opacity=".62"/>
        """
    if shape == "steam":
        return f"""
        <path d="M184 221h232l-24 55H208z" fill="{light}" opacity=".98"/>
        <path d="M220 141c27-38-18-52 13-91M298 141c27-38-18-52 13-91M376 141c27-38-18-52 13-91" fill="none" stroke="{accent}" stroke-width="12" stroke-linecap="round"/>
        <ellipse cx="300" cy="221" rx="136" ry="33" fill="{soft}" opacity=".9"/>
        """
    if shape == "noodle":
        return f"""
        <ellipse cx="300" cy="220" rx="145" ry="58" fill="{light}" opacity=".98"/>
        <path d="M192 206c42-38 82 38 124 0s82 38 124 0" fill="none" stroke="{accent}" stroke-width="13" stroke-linecap="round"/>
        <path d="M218 233c38-24 73 24 111 0s73 24 111 0" fill="none" stroke="{soft}" stroke-width="11" stroke-linecap="round"/>
        <path d="M210 116l190 82M413 113l-198 85" stroke="#ffffff" stroke-width="9" stroke-linecap="round" opacity=".66"/>
        """
    if shape == "dessert":
        return f"""
        <path d="M226 252h148l-16 52H242z" fill="{light}" opacity=".98"/>
        <path d="M236 143h128l35 112H201z" fill="{soft}" opacity=".92"/>
        <circle cx="300" cy="126" r="42" fill="{accent}"/>
        <circle cx="268" cy="178" r="13" fill="#ffffff" opacity=".72"/>
        <circle cx="329" cy="196" r="11" fill="#ffffff" opacity=".64"/>
        """
    return f"""
    <ellipse cx="300" cy="218" rx="152" ry="66" fill="{light}" opacity=".98"/>
    <path d="M186 205h228l-30 82H216z" fill="{soft}" opacity=".92"/>
    <circle cx="250" cy="198" r="18" fill="{accent}"/>
    <circle cx="305" cy="214" r="15" fill="{accent}" opacity=".82"/>
    <circle cx="360" cy="195" r="20" fill="{accent}" opacity=".72"/>
    <path d="M224 125c22-28-12-44 16-76M304 125c22-28-12-44 16-76M384 125c22-28-12-44 16-76" fill="none" stroke="#ffffff" stroke-width="9" stroke-linecap="round" opacity=".65"/>
    """


def accent_marks(seed: int) -> str:
    marks: list[str] = []
    for index in range(7):
        x = 56 + ((seed >> (index * 5)) % 520)
        y = 48 + ((seed >> (index * 7)) % 270)
        radius = 4 + ((seed >> (index * 3)) % 12)
        opacity = 0.10 + (((seed >> (index * 4)) % 8) / 100)
        marks.append(f'<circle cx="{x}" cy="{y}" r="{radius}" fill="#ffffff" opacity="{opacity:.2f}"/>')
    return "\n".join(marks)


def make_food_illustration(name: str, cuisine: str | None = None, unique_key: str | int | None = None) -> str:
    seed = stable_int(f"{name}|{cuisine or ''}|{unique_key or ''}")
    bg, accent, soft, light = PALETTES[seed % len(PALETTES)]
    shape = SHAPES[(seed // 7) % len(SHAPES)]
    label = html.escape(short_label(name))
    cuisine_label = html.escape((cuisine or "Local Taste")[:18])
    svg = f"""<svg xmlns="http://www.w3.org/2000/svg" width="640" height="400" viewBox="0 0 640 400">
    <defs>
      <linearGradient id="g" x1="0" x2="1" y1="0" y2="1">
        <stop offset="0" stop-color="{bg}"/>
        <stop offset="1" stop-color="{accent}"/>
      </linearGradient>
      <radialGradient id="r" cx=".76" cy=".18" r=".52">
        <stop offset="0" stop-color="#ffffff" stop-opacity=".28"/>
        <stop offset="1" stop-color="#ffffff" stop-opacity="0"/>
      </radialGradient>
    </defs>
    <rect width="640" height="400" rx="0" fill="url(#g)"/>
    <rect width="640" height="400" fill="url(#r)"/>
    <circle cx="92" cy="80" r="46" fill="#ffffff" opacity=".08"/>
    <circle cx="548" cy="316" r="86" fill="#000000" opacity=".13"/>
    {accent_marks(seed)}
    {shape_markup(shape, accent, soft, light)}
    <text x="44" y="333" fill="#ffffff" font-family="Arial,'Microsoft YaHei',sans-serif" font-size="42" font-weight="800">{label}</text>
    <text x="46" y="365" fill="#ffffff" opacity=".76" font-family="Arial,'Microsoft YaHei',sans-serif" font-size="19" font-weight="700">{cuisine_label}</text>
    </svg>"""
    return "data:image/svg+xml;charset=UTF-8," + urllib.parse.quote(svg, safe="(),/:;=")
