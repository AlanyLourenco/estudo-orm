"""
Gera o PDF da apresentação a partir de src.html.

Uso (dentro da pasta fonte-slides):
    python gerar-pdf.py

Precisa de: Python 3 e Google Chrome (ou Microsoft Edge) instalados.
Se o navegador estiver em outro lugar, informe o caminho na variável CHROME.
"""
import base64
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

AQUI = Path(__file__).resolve().parent
PDF = AQUI.parent / "ORM - aproximando objetos e banco de dados.pdf"

CANDIDATOS = [
    os.environ.get("CHROME", ""),
    r"C:\Program Files\Google\Chrome\Application\chrome.exe",
    r"C:\Program Files (x86)\Google\Chrome\Application\chrome.exe",
    r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
    shutil.which("google-chrome") or "",
    shutil.which("chromium") or "",
    shutil.which("chromium-browser") or "",
]


def achar_navegador():
    for c in CANDIDATOS:
        if c and Path(c).exists():
            return c
    sys.exit("Chrome/Edge não encontrado. Defina a variável CHROME com o caminho do executável.")


def main():
    # 1) embute as fontes (o Chrome não carrega fontes locais via file://)
    fontes = AQUI / "fonts"
    css = (fontes / "fonts.css").read_text(encoding="utf-8")
    css = re.sub(
        r"url\(([^)]+)\)",
        lambda m: "url(data:font/woff2;base64,"
        + base64.b64encode((fontes / m.group(1)).read_bytes()).decode() + ")",
        css,
    )
    fonte = (AQUI / "src.html").read_text(encoding="utf-8")
    (AQUI / "slides.html").write_text(fonte.replace("/*FONTS*/", css), encoding="utf-8")

    # 2) imprime em PDF (1920×1080 por página)
    navegador = achar_navegador()
    subprocess.run(
        [navegador, "--headless=new", "--disable-gpu", "--no-pdf-header-footer",
         "--virtual-time-budget=15000", f"--print-to-pdf={PDF}",
         (AQUI / "slides.html").as_uri()],
        check=True, capture_output=True,
    )
    print(f"PDF gerado: {PDF}")


if __name__ == "__main__":
    main()
