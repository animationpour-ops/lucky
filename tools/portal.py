"""Moteur de texture 'portail de l'End en vert' : champ d'étoiles animé + formes d'items."""
import math
import numpy as np
from PIL import Image

N = 16  # nombre d'images de l'animation (boucle parfaite)
S = 16  # taille d'un item

# ---------------------------------------------------------------- champ d'étoiles
def _layer(seed, count, big=0):
    rng = np.random.default_rng(seed)
    inten = np.zeros((S, S))
    phase = np.zeros((S, S))
    for _ in range(count):
        x, y = rng.integers(0, S, 2)
        inten[y, x] = max(inten[y, x], rng.uniform(0.55, 1.0))
        phase[y, x] = rng.uniform(0, 2 * math.pi)
    for _ in range(big):  # grosses étoiles en croix
        x, y = rng.integers(0, S, 2)
        inten[y, x] = 1.0
        phase[y, x] = rng.uniform(0, 2 * math.pi)
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            xx, yy = (x + dx) % S, (y + dy) % S
            inten[yy, xx] = max(inten[yy, xx], 0.38)
            phase[yy, xx] = phase[y, x]
    return inten, phase

# (intensité, phase, décalage x/image, décalage y/image, fréquence de scintillement)
LAYERS = [
    (*_layer(11, 26), 1, 0, 1),
    (*_layer(23, 14), 0, 1, 2),
    (*_layer(37, 6, big=2), -1, -1, 1),
]
DIM = np.array([14, 110, 58], float)
MID = np.array([40, 235, 105], float)
HOT = np.array([225, 255, 230], float)

def starfield(f):
    """Image f (0..N-1) du champ d'étoiles, tableau (S,S,3) float 0..255, tuilable."""
    yy, xx = np.mgrid[0:S, 0:S]
    neb = 0.5 + 0.5 * np.sin(2 * math.pi * ((xx + yy) / S + f / N))
    neb2 = 0.5 + 0.5 * np.sin(2 * math.pi * ((xx - 2 * yy) / S - f / N))
    bg = np.zeros((S, S, 3))
    bg[..., 0] = 2 + 4 * neb
    bg[..., 1] = 20 + 26 * neb + 10 * neb2
    bg[..., 2] = 10 + 14 * neb + 8 * neb2
    for inten, phase, sx, sy, k in LAYERS:
        i = np.roll(inten, (sy * f, sx * f), axis=(0, 1))
        p = np.roll(phase, (sy * f, sx * f), axis=(0, 1))
        tw = 0.62 + 0.38 * np.sin(2 * math.pi * (f * k / N) + p)
        v = np.clip(i * tw, 0, 1)
        col = np.where(v[..., None] < 0.6,
                       DIM + (MID - DIM) * (v[..., None] / 0.6),
                       MID + (HOT - MID) * ((v[..., None] - 0.6) / 0.4))
        m = (i > 0)[..., None]
        bg = np.where(m, np.maximum(bg, col * np.clip(v[..., None] * 1.15, 0, 1)), bg)
    return bg

# ---------------------------------------------------------------- formes
def grid():
    return [['.'] * S for _ in range(S)]

def _dist_seg(px, py, x0, y0, x1, y1):
    dx, dy = x1 - x0, y1 - y0
    l2 = dx * dx + dy * dy
    t = 0.0 if l2 == 0 else max(0.0, min(1.0, ((px - x0) * dx + (py - y0) * dy) / l2))
    return math.hypot(px - (x0 + t * dx), py - (y0 + t * dy))

def line(g, x0, y0, x1, y1, ch, w=1.2):
    for y in range(S):
        for x in range(S):
            if _dist_seg(x + .5, y + .5, x0, y0, x1, y1) <= w / 2:
                g[y][x] = ch

def poly(g, pts, ch, w=1.2):
    for a, b in zip(pts, pts[1:]):
        line(g, a[0], a[1], b[0], b[1], ch, w)

def rect(g, x0, y0, x1, y1, ch):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if 0 <= x < S and 0 <= y < S:
                g[y][x] = ch

def erase(g, x0, y0, x1, y1):
    rect(g, x0, y0, x1, y1, '.')

def ellipse(g, cx, cy, ra, rb, ch, axis=(1, -1)):
    n = math.hypot(*axis)
    ax, ay = axis[0] / n, axis[1] / n
    for y in range(S):
        for x in range(S):
            dx, dy = x + .5 - cx, y + .5 - cy
            a = dx * ax + dy * ay
            b = -dx * ay + dy * ax
            if (a / ra) ** 2 + (b / rb) ** 2 <= 1:
                g[y][x] = ch

def pix(g, pts, ch):
    for x, y in pts:
        if 0 <= x < S and 0 <= y < S:
            g[y][x] = ch

# ---------------------------------------------------------------- rendu
OUTLINE = np.array([2, 14, 7], float)
HANDLE = np.array([12, 40, 24], float)
GUARD = np.array([140, 255, 90], float)
STRING = np.array([190, 255, 200], float)

def render_frame(g, f):
    """Applique le champ d'étoiles aux pixels 'B', des couleurs fixes aux autres, + contour/biseau."""
    sf = starfield(f)
    img = np.zeros((S, S, 4))
    def at(x, y):
        return g[y][x] if 0 <= x < S and 0 <= y < S else '.'
    for y in range(S):
        for x in range(S):
            c = g[y][x]
            if c == '.':
                continue
            if c == 'B':
                col = sf[y, x].copy()
                if ((x + y) - 2 * f) % 32 in (0, 1, 2):
                    col = col + np.array([30, 70, 40])
                lit = at(x - 1, y) == '.' or at(x, y - 1) == '.'
                shade = at(x + 1, y) == '.' or at(x, y + 1) == '.'
                if lit:
                    col = col * 1.0 + np.array([20, 70, 30])
                elif shade:
                    col = col * 0.55
            elif c == 'H':
                col = HANDLE + 10 * np.sin((x + y) * 1.3)
            elif c == 'G':
                col = GUARD * (0.8 + 0.2 * math.sin(2 * math.pi * (f / N) + x))
            elif c == 'S':
                col = STRING
            elif c == 'F':  # flamme qui vacille
                k = 0.5 + 0.5 * math.sin(2 * math.pi * (f / N) * 2 + y * 1.7 + x)
                col = np.array([90, 255, 70]) * (0.7 + 0.3 * k) + np.array([60, 0, 60]) * (1 if y >= 3 else 0) * k
            elif c == 'A':
                col = np.array([210, 255, 220])
            else:
                continue
            img[y, x, :3] = np.clip(col, 0, 255)
            img[y, x, 3] = 255
    # contour sombre
    out = img.copy()
    for y in range(S):
        for x in range(S):
            if g[y][x] == '.':
                if any(at(x + dx, y + dy) != '.' for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    out[y, x, :3] = OUTLINE
                    out[y, x, 3] = 255
    return out.astype(np.uint8)

def save_animated(g, path, frametime=2):
    frames = [render_frame(g, f) for f in range(N)]
    strip = np.concatenate(frames, axis=0)
    Image.fromarray(strip, 'RGBA').save(path)
    with open(path + '.mcmeta', 'w') as fh:
        fh.write('{\n  "animation": {\n    "frametime": %d,\n    "interpolate": false\n  }\n}\n' % frametime)

def preview(grids, path, scale=10, f=5):
    """Planche de contrôle (pas livrée dans le mod)."""
    w = len(grids)
    sheet = Image.new('RGBA', (w * (S + 1), S), (40, 40, 40, 255))
    for i, g in enumerate(grids):
        im = Image.fromarray(render_frame(g, f), 'RGBA')
        sheet.paste(im, (i * (S + 1), 0), im)
    sheet.resize((sheet.width * scale, sheet.height * scale), Image.NEAREST).save(path)
