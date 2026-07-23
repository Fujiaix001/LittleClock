import os
import urllib.request
import subprocess
import sys

fonts_map = {
    "font_digital.ttf": "https://github.com/google/fonts/raw/main/ofl/dotgothic16/DotGothic16-Regular.ttf",
    "font_sans.ttf": "https://github.com/google/fonts/raw/main/ofl/notosansjp/NotoSansJP%5Bwght%5D.ttf",
    "font_serif.ttf": "https://github.com/google/fonts/raw/main/ofl/notoserifjp/NotoSerifJP%5Bwght%5D.ttf",
    "font_rounded.ttf": "https://github.com/google/fonts/raw/main/ofl/zenmarugothic/ZenMaruGothic-Medium.ttf",
    "font_kai.ttf": "https://github.com/google/fonts/raw/main/ofl/kleeone/KleeOne-SemiBold.ttf",
    "font_heavy.ttf": "https://github.com/google/fonts/raw/main/ofl/delagothicone/DelaGothicOne-Regular.ttf",
    "font_audiowide.ttf": "https://github.com/google/fonts/raw/main/ofl/audiowide/Audiowide-Regular.ttf",
    "font_oxanium.ttf": "https://github.com/google/fonts/raw/main/ofl/oxanium/Oxanium%5Bwght%5D.ttf",
    "font_sairastencil.ttf": "https://github.com/google/fonts/raw/main/ofl/sairastencilone/SairaStencilOne-Regular.ttf",
    "font_zendots.ttf": "https://github.com/google/fonts/raw/main/ofl/zendots/ZenDots-Regular.ttf",
}

cjk_font_names = {
    "font_digital.ttf",
    "font_sans.ttf",
    "font_serif.ttf",
    "font_rounded.ttf",
    "font_kai.ttf",
    "font_heavy.ttf",
}

target_dir = os.path.join(os.path.dirname(__file__), "app", "src", "main", "assets", "fonts")
os.makedirs(target_dir, exist_ok=True)

temp_dir = os.path.join(os.environ["TEMP"], "font_download_cache")
os.makedirs(temp_dir, exist_ok=True)

latin_chars = "0123456789:./-_()[],+ ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
cjk_date_chars = "年月日時星期一二三四五六"

headers = {'User-Agent': 'Mozilla/5.0'}

for name, url in fonts_map.items():
    raw_path = os.path.join(temp_dir, name)
    out_path = os.path.join(target_dir, name)
    print(f"Processing {name}...")
    if not os.path.exists(raw_path) or os.path.getsize(raw_path) < 1000:
        req = urllib.request.Request(url, headers=headers)
        with urllib.request.urlopen(req) as resp, open(raw_path, 'wb') as out_file:
            out_file.write(resp.read())
    
    subset_chars = latin_chars + (cjk_date_chars if name in cjk_font_names else "")
    cmd = [
        sys.executable, "-m", "fontTools.subset",
        raw_path,
        f"--text={subset_chars}",
        f"--output-file={out_path}"
    ]
    subprocess.run(cmd, check=True)
    size_kb = os.path.getsize(out_path) / 1024.0
    print(f"Generated {out_path}: {size_kb:.1f} KB")

storopia_path = os.path.join(target_dir, "font_storopia.ttf")
if not os.path.exists(storopia_path):
    raise FileNotFoundError(
        "font_storopia.ttf is an internal test asset and must be supplied separately"
    )

print("All distributable fonts were subset successfully; Storopia was preserved.")
