"""يبني أيقونة «بيننا» من مصدر العلامة: `docs/design/source/logo-source.png`.

قابل للتشغيل في أي وقت: `python3 tools/identity/build_icons.py`
يُنتج: mipmap-*/ic_launcher{,_round}.webp لكل الكثافات، وdrawable/ic_launcher_foreground.png
وic_launcher_monochrome.png، ومعاينة في docs/design/app-icon-preview.png.
الإطار مكتوب مرة واحدة حتى لا تختلف الأيقونات بين الأجهزة.
"""
from __future__ import annotations

import io
import pathlib
import sys

try:
    from PIL import Image, ImageChops, ImageDraw
except ImportError:  # pragma: no cover
    sys.exit("مطلوب Pillow: python3 -m pip install pillow")

ROOT = pathlib.Path(__file__).resolve().parents[2]
SRC = ROOT / "docs/design/source/logo-source.png"
RES = ROOT / "app/src/main/res"
NAVY = (0x14, 0x30, 0x4F)

# نِسب معتمدة: العلامة داخل المنطقة الآمنة للأيقونة التكيّفية (أقصى 66%) ولا تلمس الحافة.
LEGACY_MARK = 0.72
ADAPTIVE_MARK = 0.66
CROP = 0.80          # من الصورة المصدر: وسطها هو العلامة، وحواشيها لون الخلفية
CORNER = 0.22        # استدارة الأيقونة المربّعة القديمة
DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
BASE = 1024


def load_source() -> Image.Image:
    image = Image.open(SRC).convert("RGB")
    side = min(image.size)
    return image.crop(
        ((image.width - side) // 2, (image.height - side) // 2,
         (image.width - side) // 2 + side, (image.height - side) // 2 + side)
    )


def mark(source: Image.Image, canvas: int, ratio: float) -> Image.Image:
    """العلامة وحدها بخلفية شفّافة.

    صورة المصدر خلفيتها النيلية نفسها، فلو قُصّت كما هي لظهر مربّع باهت فوق لوننا المسطّح
    (اختلاف تدرّج خفيف في الأصل). هنا تُفتح الخلفية بمقارنة كل بكسل بلون النيلية: ما يقارب
    اللون يصبح شفافًا، والحواف تُفتح بتدرّج لا بقطع حاد حتى لا تبقى أسنان.
    """
    inner = int(source.width * CROP)
    box = ((source.width - inner) // 2, (source.height - inner) // 2,
           (source.width + inner) // 2, (source.height + inner) // 2)
    size = int(canvas * ratio)
    face = source.crop(box).resize((size, size), Image.LANCZOS).convert("RGB")

    bg = Image.new("RGB", face.size, NAVY)
    diff = ImageChops.difference(face, bg)
    # أقصى فرق في أي قناة: 0 = خلفية، وأكثر = علامة
    distance = ImageChops.lighter(ImageChops.lighter(*diff.split()[:2]), diff.split()[2])
    # داخل الخلفية (فرق < 22) شفاف، وحول الحواف تدرّج حتى 60، ثم صلب
    alpha = distance.point(lambda value: 0 if value < 22 else min(255, (value - 22) * 255 // 38))
    keyed = face.convert("RGBA")
    keyed.putalpha(alpha)
    return keyed


def square_icon(source: Image.Image, size: int, round_shape: bool) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(canvas)
    if round_shape:
        draw.ellipse([0, 0, size - 1, size - 1], fill=NAVY)
    else:
        draw.rounded_rectangle([0, 0, size - 1, size - 1], radius=int(size * CORNER), fill=NAVY)
    face = mark(source, size, LEGACY_MARK)
    canvas.alpha_composite(face, ((size - face.width) // 2, (size - face.height) // 2))
    return canvas


def foreground(source: Image.Image, size: int) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    face = mark(source, size, ADAPTIVE_MARK)
    canvas.alpha_composite(face, ((size - face.width) // 2, (size - face.height) // 2))
    return canvas


def monochrome(source: Image.Image, size: int) -> Image.Image:
    """نسخة بلون واحد لأندرويد 13+: العلامة بيضاء بشفافية الخلفية نفسها."""
    face = mark(source, size, ADAPTIVE_MARK)
    layer = Image.new("RGBA", face.size, (255, 255, 255, 255))
    layer.putalpha(face.getchannel("A"))
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    canvas.alpha_composite(layer, ((size - layer.width) // 2, (size - layer.height) // 2))
    return canvas


def main() -> None:
    source = load_source()
    for density, size in DENSITIES.items():
        folder = RES / f"mipmap-{density}"
        folder.mkdir(parents=True, exist_ok=True)
        square_icon(source, size, False).save(folder / "ic_launcher.webp", "WEBP", quality=95, method=6)
        square_icon(source, size, True).save(folder / "ic_launcher_round.webp", "WEBP", quality=95, method=6)

    drawable = RES / "drawable"
    drawable.mkdir(parents=True, exist_ok=True)
    for name, image in (("ic_launcher_foreground.png", foreground(source, 432)),
                        ("ic_launcher_monochrome.png", monochrome(source, 432))):
        buffer = io.BytesIO()
        image.save(buffer, "PNG")
        (drawable / name).write_bytes(buffer.getvalue())

    preview = Image.new("RGBA", (1200, 460), (250, 247, 240, 255))
    preview.alpha_composite(square_icon(source, 360, False), (40, 50))
    preview.alpha_composite(square_icon(source, 300, True), (450, 80))
    preview.alpha_composite(foreground(source, 330), (800, 65))
    preview.convert("RGB").save(ROOT / "docs/design/app-icon-preview.png", "PNG")
    print("تم: أيقونات كل الكثافات + معاينة")


if __name__ == "__main__":
    main()
