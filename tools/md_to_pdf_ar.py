#!/usr/bin/env python3
"""Convert a Markdown plan document (Arabic RTL) to a readable PDF.

Usage:
    python3 tools/md_to_pdf_ar.py docs/OPERATING_PLAN_V5_AR.md docs/OPERATING_PLAN_V5_AR.pdf

Requirements (install in a virtualenv, they are documentation-only tools):
    pip install reportlab arabic-reshaper python-bidi

Fonts: Cairo Regular/Bold are downloaded once from the Google Fonts repository
and cached in ~/.cache/jerba-fonts/. If the download is not possible, pass
--font /path/to/font.ttf (a font that covers Arabic).

What it supports: headings, paragraphs, bullet lists, numbered lists, simple
pipe tables, fenced code blocks, block quotes, and horizontal rules. It is a
presentation helper for the plan documents, not a full Markdown renderer.
"""

from __future__ import annotations

import argparse
import os
import re
import sys
import textwrap
import urllib.request

from reportlab.lib import colors
from reportlab.lib.enums import TA_RIGHT, TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    HRFlowable,
    KeepTogether,
    ListFlowable,
    ListItem,
    PageBreak,
    Paragraph,
    Preformatted,
    SimpleDocTemplate,
    Spacer,
    Table,
    TableStyle,
)

import arabic_reshaper
from bidi.algorithm import get_display

FONT_CACHE = os.path.join(os.path.expanduser("~"), ".cache", "jerba-fonts")
FONT_URLS = {
    "Cairo-Regular.ttf": "https://raw.githubusercontent.com/google/fonts/main/ofl/cairo/Cairo%5Bslnt%2Cwght%5D.ttf",
    "Cairo-Bold.ttf": "https://raw.githubusercontent.com/google/fonts/main/ofl/amiri/Amiri-Bold.ttf",
}

# DejaVu ships with most Linux distributions, covers Arabic + Arabic
# Presentation Forms, and renders correctly after reshaping, so it is the
# first fallback when no font is given on the command line.
DEJAVU_REGULAR = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
DEJAVU_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"

PAGE_W, PAGE_H = A4
MARGIN = 16 * mm


def ensure_fonts() -> tuple[str, str]:
    if os.path.exists(DEJAVU_REGULAR):
        bold = DEJAVU_BOLD if os.path.exists(DEJAVU_BOLD) else DEJAVU_REGULAR
        return DEJAVU_REGULAR, bold
    os.makedirs(FONT_CACHE, exist_ok=True)
    for name, url in FONT_URLS.items():
        path = os.path.join(FONT_CACHE, name)
        if not os.path.exists(path):
            print(f"downloading font {name} ...", file=sys.stderr)
            urllib.request.urlretrieve(url, path)
    return (
        os.path.join(FONT_CACHE, "Cairo-Regular.ttf"),
        os.path.join(FONT_CACHE, "Cairo-Bold.ttf"),
    )


def register_fonts(regular: str, bold: str) -> None:
    pdfmetrics.registerFont(TTFont("Cairo", regular))
    pdfmetrics.registerFont(TTFont("Cairo-Bold", bold))
    pdfmetrics.registerFontFamily("Cairo", normal="Cairo", bold="Cairo-Bold")


def rtl(text: str) -> str:
    """Shape Arabic glyphs and reorder the line for right-to-left display."""
    text = text.strip()
    if not text:
        return ""
    return get_display(arabic_reshaper.reshape(text), base_dir="R")


def inline(text: str) -> str:
    """Strip Markdown inline markers and escape XML for reportlab Paragraph."""
    text = re.sub(r"\*\*(.+?)\*\*", r"\1", text)
    text = re.sub(r"`(.+?)`", r"\1", text)
    text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    return text


def plain(text: str) -> str:
    return rtl(inline(text))


def make_styles() -> dict[str, ParagraphStyle]:
    base = ParagraphStyle(
        "base",
        fontName="Cairo",
        fontSize=10.5,
        leading=17,
        alignment=TA_RIGHT,
        wordWrap="RTL",
        spaceAfter=4,
        textColor=colors.HexColor("#1a1a1a"),
    )
    return {
        "base": base,
        "h1": ParagraphStyle("h1", parent=base, fontName="Cairo-Bold", fontSize=20, leading=30, alignment=TA_CENTER, spaceBefore=2, spaceAfter=12, textColor=colors.HexColor("#0f3d2e")),
        "h2": ParagraphStyle("h2", parent=base, fontName="Cairo-Bold", fontSize=15, leading=24, spaceBefore=12, spaceAfter=6, textColor=colors.HexColor("#14532d")),
        "h3": ParagraphStyle("h3", parent=base, fontName="Cairo-Bold", fontSize=12.5, leading=20, spaceBefore=8, spaceAfter=4, textColor=colors.HexColor("#166534")),
        "quote": ParagraphStyle("quote", parent=base, textColor=colors.HexColor("#3f3f46"), rightIndent=6 * mm, leftIndent=6 * mm, spaceBefore=4, spaceAfter=8, borderPadding=4),
        "meta": ParagraphStyle("meta", parent=base, fontSize=9.5, leading=15, textColor=colors.HexColor("#52525b")),
        "cell": ParagraphStyle("cell", parent=base, fontSize=9.5, leading=14, spaceAfter=0),
        "cellhead": ParagraphStyle("cellhead", parent=base, fontName="Cairo-Bold", fontSize=9.5, leading=14, spaceAfter=0, textColor=colors.HexColor("#0f3d2e")),
        "code": ParagraphStyle("code", parent=base, fontName="Courier", fontSize=8.5, leading=12, alignment=0, wordWrap=None),
    }


def flush_paragraph(flow, lines, style):
    if lines:
        text = " ".join(line.strip() for line in lines)
        flow.append(Paragraph(plain(text), style))
        lines.clear()


def flush_list(flow, items, styles, ordered):
    if not items:
        return
    entries = []
    for item in items:
        entries.append(ListItem(Paragraph(plain(item), styles["base"]), rightIndent=8 * mm, leftIndent=8 * mm))
    flow.append(
        ListFlowable(
            entries,
            bulletType="1" if ordered else "bullet",
            bulletFormat="%s." if ordered else None,
            bulletFontName="Cairo",
            bulletFontSize=10.5,
            start="1" if ordered else None,
            leftIndent=10 * mm,
        )
    )
    items.clear()


def flush_table(flow, rows, styles):
    if not rows:
        return
    width = PAGE_W - 2 * MARGIN
    ncols = max(len(r) for r in rows)
    data = []
    for i, row in enumerate(rows):
        style = styles["cellhead"] if i == 0 else styles["cell"]
        cells = []
        for col in range(ncols):
            value = row[col] if col < len(row) else ""
            cells.append(Paragraph(plain(value), style))
        data.append(cells)
    table = Table(data, colWidths=[width / ncols] * ncols, repeatRows=1, hAlign="RIGHT")
    table.setStyle(
        TableStyle(
            [
                ("GRID", (0, 0), (-1, -1), 0.4, colors.HexColor("#a1a1aa")),
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#f0fdf4")),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 4),
                ("RIGHTPADDING", (0, 0), (-1, -1), 4),
                ("TOPPADDING", (0, 0), (-1, -1), 3),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 3),
            ]
        )
    )
    flow.append(table)
    flow.append(Spacer(1, 5))
    rows.clear()


def convert(source: str, target: str, font_regular: str, font_bold: str) -> None:
    register_fonts(font_regular, font_bold)
    styles = make_styles()

    with open(source, encoding="utf-8") as handle:
        lines = handle.read().splitlines()

    doc = SimpleDocTemplate(
        target,
        pagesize=A4,
        rightMargin=MARGIN,
        leftMargin=MARGIN,
        topMargin=MARGIN,
        bottomMargin=MARGIN,
        title=os.path.basename(source),
        author="جِربة | Jerba",
    )
    flow = []
    paragraph: list[str] = []
    bullet_items: list[str] = []
    ordered_items: list[str] = []
    table_rows: list[list[str]] = []
    in_code = False
    code_lines: list[str] = []
    first_h1 = True

    def flush_all():
        flush_paragraph(flow, paragraph, styles["base"])
        flush_list(flow, bullet_items, styles, ordered=False)
        flush_list(flow, ordered_items, styles, ordered=True)
        flush_table(flow, table_rows, styles)

    for raw in lines:
        line = raw.rstrip()
        stripped = line.strip()

        if stripped.startswith("```"):
            if in_code:
                flow.append(Preformatted("\n".join(code_lines), styles["code"]))
                flow.append(Spacer(1, 5))
                code_lines.clear()
                in_code = False
            else:
                flush_all()
                in_code = True
            continue
        if in_code:
            code_lines.append(line)
            continue

        if not stripped:
            flush_all()
            continue

        if set(stripped) <= {"-"} and len(stripped) >= 3:
            flush_all()
            flow.append(HRFlowable(width="100%", thickness=0.6, color=colors.HexColor("#d4d4d8"), spaceBefore=2, spaceAfter=6))
            continue

        heading = re.match(r"^(#{1,4})\s+(.*)$", stripped)
        if heading:
            flush_all()
            level = len(heading.group(1))
            text = heading.group(2)
            if level == 1 and first_h1:
                first_h1 = False
                flow.append(Paragraph(plain(text), styles["h1"]))
            else:
                flow.append(Paragraph(plain(text), styles["h2" if level <= 2 else "h3"]))
            continue

        if stripped.startswith(">"):
            flush_all()
            flow.append(Paragraph(plain(stripped.lstrip("> ")), styles["quote"]))
            continue

        if stripped.startswith("|"):
            flush_paragraph(flow, paragraph, styles["base"])
            flush_list(flow, bullet_items, styles, ordered=False)
            flush_list(flow, ordered_items, styles, ordered=True)
            if set(stripped) <= {"|", "-", ":", " "}:
                continue
            cells = [cell.strip() for cell in stripped.strip("|").split("|")]
            table_rows.append(cells)
            continue

        match = re.match(r"^[-*]\s+(.*)$", stripped)
        if match:
            flush_paragraph(flow, paragraph, styles["base"])
            flush_list(flow, ordered_items, styles, ordered=True)
            flush_table(flow, table_rows, styles)
            bullet_items.append(match.group(1))
            continue

        match = re.match(r"^\d+[.)]\s+(.*)$", stripped)
        if match:
            flush_paragraph(flow, paragraph, styles["base"])
            flush_list(flow, bullet_items, styles, ordered=False)
            flush_table(flow, table_rows, styles)
            ordered_items.append(match.group(1))
            continue

        paragraph.append(stripped)

    flush_all()
    doc.build(flow)
    print(f"wrote {target}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source")
    parser.add_argument("target")
    parser.add_argument("--font", help="regular TTF font that covers Arabic (optional)")
    parser.add_argument("--font-bold", help="bold TTF font (optional)")
    args = parser.parse_args()

    if args.font:
        regular = args.font
        bold = args.font_bold or args.font
    else:
        regular, bold = ensure_fonts()
    convert(args.source, args.target, regular, bold)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
