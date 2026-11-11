"""Silhouettes 16x16 des items Lucky (B = portail animé, H = manche, G = garde/accent, S = corde, F = flamme, A = flèche)."""
import math
from portal import *

def sword():
    g = grid()
    line(g, 14.0, 2.0, 7.0, 9.0, 'B', 2.3)
    pix(g, [(14, 1), (15, 0)], 'B')
    line(g, 4.2, 7.6, 8.4, 11.8, 'G', 1.5)
    line(g, 5.0, 11.0, 2.4, 13.6, 'H', 1.4)
    pix(g, [(1, 14)], 'G')
    return g

def pickaxe():
    g = grid()
    line(g, 2.5, 14.0, 10.5, 6.0, 'H', 1.4)
    poly(g, [(2.5, 7.5), (4.5, 4.0), (8.0, 2.2), (11.8, 3.2), (13.8, 6.2), (14.0, 9.5)], 'B', 2.3)
    pix(g, [(9, 5), (10, 6)], 'G')
    return g

def shovel():
    g = grid()
    line(g, 2.5, 13.5, 9.5, 6.5, 'H', 1.4)
    pix(g, [(1, 14), (2, 14), (1, 13)], 'H')
    line(g, 8.5, 7.5, 10.0, 6.0, 'G', 1.8)
    ellipse(g, 11.9, 4.1, 3.7, 2.7, 'B')
    return g

def lighter():
    g = grid()
    rect(g, 4, 8, 11, 14, 'B')
    rect(g, 4, 5, 11, 6, 'G')
    rect(g, 4, 7, 11, 7, 'H')
    pix(g, [(8, 6), (9, 6)], 'H')
    pix(g, [(9, 0), (8, 1), (9, 1), (7, 2), (8, 2), (9, 2), (10, 2), (7, 3), (8, 3), (9, 3), (10, 3), (8, 4), (9, 4)], 'F')
    pix(g, [(12, 8), (12, 9), (12, 10)], 'H')
    return g

def bow(pull=0):
    g = grid()
    pts = []
    for i in range(0, 13):
        t = math.radians(i * 90 / 12)
        pts.append((13.2 - 11.5 * math.cos(t), 13.2 - 11.5 * math.sin(t)))
    poly(g, pts, 'B', 2.2)
    sx, sy = 13.0, 1.8   # extrémités de la corde
    ex, ey = 1.8, 13.0
    mx, my = (sx + ex) / 2 + pull * 1.7, (sy + ey) / 2 + pull * 1.7
    line(g, sx, sy, mx, my, 'S', 0.9)
    line(g, mx, my, ex, ey, 'S', 0.9)
    if pull:
        line(g, mx, my, mx - 8.5, my - 8.5, 'A', 0.9)
        pix(g, [(int(mx - 9), int(my - 9))], 'A')
    return g

def helmet():
    g = grid()
    rect(g, 3, 3, 12, 6, 'B')
    rect(g, 2, 5, 13, 12, 'B')
    erase(g, 5, 9, 10, 12)
    rect(g, 6, 2, 9, 2, 'G')
    return g

def chestplate():
    g = grid()
    rect(g, 4, 3, 11, 14, 'B')
    rect(g, 1, 3, 3, 9, 'B')
    rect(g, 12, 3, 14, 9, 'B')
    erase(g, 6, 3, 9, 4)
    rect(g, 7, 7, 8, 9, 'G')
    return g

def leggings():
    g = grid()
    rect(g, 3, 2, 12, 5, 'B')
    rect(g, 3, 6, 6, 14, 'B')
    rect(g, 9, 6, 12, 14, 'B')
    rect(g, 7, 6, 8, 7, 'B')
    rect(g, 3, 2, 12, 2, 'G')
    return g

def boots():
    g = grid()
    rect(g, 2, 4, 6, 12, 'B')
    rect(g, 1, 11, 7, 13, 'B')
    rect(g, 9, 4, 13, 12, 'B')
    rect(g, 8, 11, 14, 13, 'B')
    rect(g, 2, 4, 6, 5, 'G')
    rect(g, 9, 4, 13, 5, 'G')
    return g

def brush():
    g = grid()
    line(g, 2.5, 13.5, 8.2, 7.8, 'H', 1.5)
    line(g, 7.6, 8.4, 10.2, 5.8, 'G', 2.2)
    ellipse(g, 12.2, 3.8, 3.4, 2.5, 'B')
    pix(g, [(14, 1), (15, 0)], 'B')
    for p in [(11, 5), (12, 4), (13, 3)]:
        g[p[1]][p[0]] = 'H'
    return g

ALL = {
    'lucky_sword': sword, 'lucky_pickaxe': pickaxe, 'lucky_shovel': shovel, 'lucky_lighter': lighter,
    'lucky_helmet': helmet, 'lucky_chestplate': chestplate, 'lucky_leggings': leggings, 'lucky_boots': boots,
    'lucky_brush': brush,
}
