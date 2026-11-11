"""Génère toutes les textures du mod Lucky (items animés, armure, particules) + modèles/lang. Usage : python3 tools/gen_assets.py"""
import json, math, os, sys
import numpy as np
from PIL import Image
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import portal, shapes

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources')
A = os.path.join(ROOT, 'assets', 'luckymod')
for d in ('textures/item', 'textures/particle', 'textures/models/armor', 'models/item', 'particles', 'lang'):
    os.makedirs(os.path.join(A, d), exist_ok=True)

# ------------------------------------------------------------------ items animés
for name, fn in shapes.ALL.items():
    portal.save_animated(fn(), os.path.join(A, 'textures/item', name + '.png'))
portal.save_animated(shapes.bow(0), os.path.join(A, 'textures/item/lucky_bow.png'))
for i in range(3):
    portal.save_animated(shapes.bow(i), os.path.join(A, 'textures/item', 'lucky_bow_pulling_%d.png' % i))

# ------------------------------------------------------------------ modèles d'items
def jwrite(path, obj):
    with open(os.path.join(A, path), 'w') as fh:
        json.dump(obj, fh, indent=2)

for name in ('lucky_pickaxe', 'lucky_shovel', 'lucky_sword', 'lucky_lighter', 'lucky_brush'):
    parent = 'minecraft:item/handheld' if name != 'lucky_lighter' else 'minecraft:item/handheld'
    jwrite('models/item/%s.json' % name, {'parent': parent, 'textures': {'layer0': 'luckymod:item/' + name}})
for name in ('lucky_helmet', 'lucky_chestplate', 'lucky_leggings', 'lucky_boots'):
    jwrite('models/item/%s.json' % name, {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'luckymod:item/' + name}})
jwrite('models/item/lucky_bow.json', {
    'parent': 'minecraft:item/bow',
    'textures': {'layer0': 'luckymod:item/lucky_bow'},
    'overrides': [
        {'predicate': {'pulling': 1}, 'model': 'luckymod:item/lucky_bow_pulling_0'},
        {'predicate': {'pulling': 1, 'pull': 0.65}, 'model': 'luckymod:item/lucky_bow_pulling_1'},
        {'predicate': {'pulling': 1, 'pull': 0.9}, 'model': 'luckymod:item/lucky_bow_pulling_2'},
    ]})
for i in range(3):
    jwrite('models/item/lucky_bow_pulling_%d.json' % i, {
        'parent': 'minecraft:item/bow_pulling_%d' % i,
        'textures': {'layer0': 'luckymod:item/lucky_bow_pulling_%d' % i}})

# ------------------------------------------------------------------ armure (UV humanoïde vanilla / Netherite 64x32)
rng = np.random.default_rng(99)
W, H = 64, 32
yy, xx = np.mgrid[0:H, 0:W]
neb = 0.5 + 0.5 * np.sin((xx + yy) / 5.0) * np.cos(xx / 7.0)
tex = np.zeros((H, W, 3))
tex[..., 0] = 3 + 4 * neb
tex[..., 1] = 26 + 24 * neb
tex[..., 2] = 12 + 12 * neb
for _ in range(190):
    x, y = rng.integers(0, W), rng.integers(0, H)
    v = rng.uniform(0.4, 1.0)
    col = portal.DIM + (portal.MID - portal.DIM) * min(v / 0.6, 1.0) if v < 0.6 else portal.MID + (portal.HOT - portal.MID) * ((v - 0.6) / 0.4)
    tex[y, x] = col
    if v > 0.92:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if 0 <= x + dx < W and 0 <= y + dy < H:
                tex[y + dy, x + dx] = np.maximum(tex[y + dy, x + dx], portal.MID * 0.6)

FACES = [  # (x0, y0, x1, y1) exclusifs
    (8, 0, 16, 8), (16, 0, 24, 8), (0, 8, 8, 16), (8, 8, 16, 16), (16, 8, 24, 16), (24, 8, 32, 16),
    (20, 16, 28, 20), (28, 16, 36, 20), (16, 20, 20, 32), (20, 20, 28, 32), (28, 20, 32, 32), (32, 20, 40, 32),
    (44, 16, 48, 20), (48, 16, 52, 20), (40, 20, 44, 32), (44, 20, 48, 32), (48, 20, 52, 32), (52, 20, 56, 32),
    (4, 16, 8, 20), (8, 16, 12, 20), (0, 20, 4, 32), (4, 20, 8, 32), (8, 20, 12, 32), (12, 20, 16, 32),
]
for x0, y0, x1, y1 in FACES:
    tex[y0, x0:x1] = tex[y0, x0:x1] * 0.4 + np.array([60, 200, 90]) * 0.6          # arête haute claire
    tex[y0:y1, x0] = tex[y0:y1, x0] * 0.5 + np.array([50, 170, 80]) * 0.5
    tex[y1 - 1, x0:x1] = tex[y1 - 1, x0:x1] * 0.35                                 # arête basse sombre
    tex[y0:y1, x1 - 1] = tex[y0:y1, x1 - 1] * 0.45
LIME = np.array([140, 255, 90])
tex[11:13, 9:15] = np.array([2, 10, 6])           # fente du casque
tex[8, 8:16] = LIME * 0.9                          # liseré du casque
for (x, y) in ((23, 24), (24, 24), (22, 25), (25, 25), (23, 26), (24, 26), (23, 23), (24, 23)):
    tex[y, x] = LIME                               # trèfle de plastron
tex[20:22, 20:28] = LIME * 0.8                     # ceinture (jambières)
for x0 in (4, 44):
    tex[27, x0:x0 + 4] = LIME * 0.8                # dessus des bottes / poignets
img = Image.fromarray(np.clip(tex, 0, 255).astype(np.uint8), 'RGB').convert('RGBA')
img.save(os.path.join(A, 'textures/models/armor/lucky_layer_1.png'))
img.save(os.path.join(A, 'textures/models/armor/lucky_layer_2.png'))

# ------------------------------------------------------------------ sprites de particules (32x32, supersamplés)
SZ = 32
def sprite(fn, name):
    ss = 4
    big = np.zeros((SZ * ss, SZ * ss, 4))
    c = SZ * ss / 2
    for y in range(SZ * ss):
        for x in range(SZ * ss):
            dx, dy = (x + .5 - c) / (SZ * ss / 2), (y + .5 - c) / (SZ * ss / 2)
            big[y, x] = fn(dx, dy)
    im = Image.fromarray(np.clip(big, 0, 255).astype(np.uint8), 'RGBA').resize((SZ, SZ), Image.LANCZOS)
    im.save(os.path.join(A, 'textures/particle', name + '.png'))
    with open(os.path.join(A, 'particles', name + '.json'), 'w') as fh:
        json.dump({'textures': ['luckymod:' + name]}, fh)

def mix(a, b, t):
    return a + (b - a) * min(max(t, 0), 1)
GREEN, LIMEC, WHITE = np.array([40, 255, 90.]), np.array([150, 255, 90.]), np.array([235, 255, 240.])

def f_spark(dx, dy):
    ax, ay = abs(dx), abs(dy)
    cross = math.exp(-min(ax, ay) * 14) * max(0, 1 - max(ax, ay))
    core = math.exp(-(dx * dx + dy * dy) * 30)
    v = min(1, cross * 0.95 + core)
    return [*mix(GREEN, WHITE, core * 1.4), 255 * v]

def f_glow(dx, dy):
    r = math.hypot(dx, dy)
    v = max(0, 1 - r) ** 2.2
    return [*mix(GREEN, LIMEC, 1 - r), 255 * v]

def f_ring(dx, dy):
    r = math.hypot(dx, dy)
    v = math.exp(-((r - 0.78) ** 2) / 0.006)
    return [*mix(GREEN, WHITE, v * 0.5), 255 * v]

def f_shard(dx, dy):
    d = abs(dx) / 0.32 + abs(dy) / 0.92
    v = 1.0 if d <= 1 else 0.0
    edge = max(0, 1 - d) * 3
    return [*mix(GREEN, WHITE, 0.2 + 0.6 * (1 - min(edge, 1)) * (1 if dx > 0 else 0.3)), 255 * v]

def f_streak(dx, dy):
    v = math.exp(-dx * dx / 0.012) * max(0, 1 - abs(dy)) ** 1.1
    return [*mix(GREEN, WHITE, v * v), 255 * v]

def f_clover(dx, dy):
    best = 0.0
    for sx, sy in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        cx, cy = sx * 0.34, sy * 0.34
        r = math.hypot(dx - cx, dy - cy)
        best = max(best, 1.0 if r < 0.36 else 0.0)
    stem = abs(dx - 0.06 * dy) < 0.045 and 0 < dy < 0.9
    vein = (abs(dx) < 0.035 or abs(dy) < 0.035) and math.hypot(dx, dy) < 0.8
    if stem:
        return [30, 190, 70, 255]
    if best:
        col = mix(np.array([20, 200, 70.]), np.array([150, 255, 110.]), 0.5 - 0.5 * (dx + dy))
        if vein:
            col = col * 0.55
        return [*col, 255]
    return [0, 0, 0, 0]

def f_portal(dx, dy):
    r = math.hypot(dx, dy)
    if r > 1:
        return [0, 0, 0, 0]
    x = int((dx * 0.5 + 0.5) * portal.S) % portal.S
    y = int((dy * 0.5 + 0.5) * portal.S) % portal.S
    col = portal.starfield(3)[y, x] * 1.3 + 10
    return [*col, 255 * min(1, (1 - r) * 4)]

for name, fn in (('lucky_spark', f_spark), ('lucky_glow', f_glow), ('lucky_ring', f_ring), ('lucky_shard', f_shard),
                 ('lucky_streak', f_streak), ('lucky_clover', f_clover), ('lucky_portal', f_portal)):
    sprite(fn, name)
for alias, tex_name in (('lucky_flare', 'lucky_glow'), ('lucky_ember', 'lucky_spark')):
    with open(os.path.join(A, 'particles', alias + '.json'), 'w') as fh:
        json.dump({'textures': ['luckymod:' + tex_name]}, fh)
print('Textures générées dans', os.path.abspath(A))
