"""Procedural building / prop models. Origin = centre of footprint at ground level.

Every builder returns a Mesh whose collision boxes are filled in. Footprint
radius (for terrain flattening and spacing) is registered in PREFABS.
"""
import math

from .meshkit import Mesh

MUD, ROOF, PLASTER, DOME, WOOD, STONE = "afg_mud", "afg_mudroof", "afg_plaster", "afg_dome", "afg_wood", "afg_stone"
HESCO, CONCRETE, METAL, CANVAS, LEAVES, BARK = "afg_hesco", "afg_concrete", "afg_metal", "afg_canvas", "afg_leaves", "afg_bark"
WINDOW, DOOR, SANDBAG, TENT_W = "afg_window", "afg_door", "afg_sandbag", "afg_tent_w"

S_MUD, S_STONE, S_WOOD, S_METAL, S_CONC = 7, 37, 51, 53, 4  # collision surfaces (see README)


def _door(m, x, y, face, w=1.4, h=2.3):
    """Decal quad slightly in front of a wall. face: 's','n','e','w'."""
    e = 0.03
    if face == "s":
        m.quad((x - w / 2, y - e, 0), (x + w / 2, y - e, 0), (x + w / 2, y - e, h), (x - w / 2, y - e, h), DOOR,
               uvs=[(0, 1), (1, 1), (1, 0), (0, 0)])
    elif face == "n":
        m.quad((x + w / 2, y + e, 0), (x - w / 2, y + e, 0), (x - w / 2, y + e, h), (x + w / 2, y + e, h), DOOR,
               uvs=[(0, 1), (1, 1), (1, 0), (0, 0)])
    elif face == "e":
        m.quad((x + e, y - w / 2, 0), (x + e, y + w / 2, 0), (x + e, y + w / 2, h), (x + e, y - w / 2, h), DOOR,
               uvs=[(0, 1), (1, 1), (1, 0), (0, 0)])
    else:
        m.quad((x - e, y + w / 2, 0), (x - e, y - w / 2, 0), (x - e, y - w / 2, h), (x - e, y + w / 2, h), DOOR,
               uvs=[(0, 1), (1, 1), (1, 0), (0, 0)])


def _window(m, x, y, z, face, w=1.0, h=1.0):
    e = 0.03
    uv = [(0, 1), (1, 1), (1, 0), (0, 0)]
    if face == "s":
        m.quad((x - w / 2, y - e, z), (x + w / 2, y - e, z), (x + w / 2, y - e, z + h), (x - w / 2, y - e, z + h), WINDOW, uvs=uv)
    elif face == "n":
        m.quad((x + w / 2, y + e, z), (x - w / 2, y + e, z), (x - w / 2, y + e, z + h), (x + w / 2, y + e, z + h), WINDOW, uvs=uv)
    elif face == "e":
        m.quad((x + e, y - w / 2, z), (x + e, y + w / 2, z), (x + e, y + w / 2, z + h), (x + e, y - w / 2, z + h), WINDOW, uvs=uv)
    else:
        m.quad((x - e, y + w / 2, z), (x - e, y - w / 2, z), (x - e, y - w / 2, z + h), (x - e, y + w / 2, z + h), WINDOW, uvs=uv)


def _house_body(m, x0, y0, x1, y1, h, tex=MUD, parapet=True):
    m.box(x0, y0, -1.5, x1, y1, h, tex, top=ROOF, surface=S_MUD)
    if parapet:
        t = 0.3
        for (a, b, c, d) in [(x0, y0, x1, y0 + t), (x0, y1 - t, x1, y1), (x0, y0, x0 + t, y1), (x1 - t, y0, x1, y1)]:
            m.box(a, b, h, c, d, h + 0.5, tex, collide=False)
        # roof is walkable: parapet collision as one thin ring of boxes
        m.boxes.append(((x0, y0, h), (x1, y0 + t, h + 0.5), S_MUD))
        m.boxes.append(((x0, y1 - t, h), (x1, y1, h + 0.5), S_MUD))


def house_small():
    m = Mesh()
    _house_body(m, -4, -3, 4, 3, 3.2)
    _door(m, 0, -3, "s")
    _window(m, -2.5, -3, 1.3, "s"); _window(m, 2.5, -3, 1.3, "s")
    _window(m, 4, 0, 1.3, "e"); _window(m, -4, 0, 1.3, "w")
    # roof beams poking out (typical vigas)
    for i in range(-3, 4, 2):
        m.box(i - 0.1, 3, 2.7, i + 0.1, 3.5, 2.9, WOOD, collide=False)
    return m


def house_large():
    m = Mesh()
    _house_body(m, -5, -4, 5, 4, 3.4, parapet=False)
    _house_body(m, -5, 0, 1, 4, 6.4)
    _door(m, -2, -4, "s"); _door(m, 3, -4, "s")
    for x in (-3.5, 0.5, 3.5):
        _window(m, x, -4, 1.4, "s")
    _window(m, -2, 0, 4.2, "s"); _window(m, -4, 4, 4.2, "n")
    _window(m, 5, -2, 1.4, "e"); _window(m, -5, 2, 4.2, "w")
    # small stair hut
    m.box(2.5, 1.5, 3.4, 4.5, 3.5, 5.6, MUD, top=ROOF, surface=S_MUD)
    return m


def compound():
    """Qala: square walled courtyard, corner towers, gate on the south side."""
    m = Mesh()
    L, H, T = 13.0, 4.2, 0.9
    m.wall(-L, L, L, L, -1.5, H, T, MUD, ROOF, surface=S_MUD)          # north
    m.wall(-L, -L, -L, L, -1.5, H, T, MUD, ROOF, surface=S_MUD)        # west
    m.wall(L, -L, L, L, -1.5, H, T, MUD, ROOF, surface=S_MUD)          # east
    m.wall(-L, -L, -2.5, -L, -1.5, H, T, MUD, ROOF, surface=S_MUD)     # south left
    m.wall(2.5, -L, L, -L, -1.5, H, T, MUD, ROOF, surface=S_MUD)       # south right
    m.box(-2.5, -L - T / 2, H - 0.8, 2.5, -L + T / 2, H + 0.4, MUD, ROOF, surface=S_MUD)  # gate lintel
    for cx in (-L, L):
        for cy in (-L, L):
            m.cylinder(cx, cy, -1.5, H + 2.0, 2.0, MUD, sides=10, top_tex=ROOF, surface=S_MUD, r_top=1.6)
    # inner house along the north wall
    inner = Mesh()
    _house_body(inner, -8, 5, 8, 12.5, 3.2)
    for x in (-5, 0, 5):
        _door(inner, x, 5, "s")
    for x in (-6.5, -2.5, 2.5, 6.5):
        _window(inner, x, 5, 1.2, "s")
    m.extend(inner)
    # gate doors (open)
    m.box(-2.5, -L - 0.2, 0, -2.3, -L + 2.3, 3.2, DOOR, surface=S_WOOD)
    m.box(2.3, -L - 0.2, 0, 2.5, -L + 2.3, 3.2, DOOR, surface=S_WOOD)
    return m


def mosque():
    m = Mesh()
    m.box(-8, -7, -1.5, 8, 7, 6.5, PLASTER, top=ROOF, surface=S_CONC)
    _door(m, 0, -7, "s", w=2.4, h=3.2)
    for x in (-5.5, -3, 3, 5.5):
        _window(m, x, -7, 2.2, "s", w=1.0, h=2.0)
    for y in (-4, 0, 4):
        _window(m, 8, y, 2.2, "e", w=1.0, h=2.0)
        _window(m, -8, y, 2.2, "w", w=1.0, h=2.0)
    m.cylinder(0, 0, 6.5, 8.0, 4.6, PLASTER, sides=16, top_tex=ROOF, collide=False)
    m.dome(0, 0, 8.0, 4.6, DOME, sides=16, rings=6, height=5.0)
    m.boxes.append(((-3.6, -3.6, 6.5), (3.6, 3.6, 12.0), S_CONC))
    # minaret
    m.cylinder(6.5, 5.5, 6.5, 21.0, 1.1, PLASTER, sides=10, top_tex=ROOF)
    m.cylinder(6.5, 5.5, 18.0, 18.6, 1.6, PLASTER, sides=10, collide=False)
    m.dome(6.5, 5.5, 21.0, 1.1, DOME, sides=10, rings=3, height=1.8)
    # porch columns
    for x in (-6, -2, 2, 6):
        m.cylinder(x, -9.5, 0, 4.0, 0.35, PLASTER, sides=8)
    m.box(-8, -10.2, 4.0, 8, -7, 4.5, PLASTER, top=ROOF, surface=S_CONC)
    return m


def shop_row():
    """Bazaar row: 4 shops with coloured awnings."""
    m = Mesh()
    m.box(-11, 0, -1.5, 11, 5, 3.6, MUD, top=ROOF, surface=S_MUD)
    awn = ["afg_awn_red", "afg_awn_blue", "afg_awn_green", "afg_awn_red"]
    for i in range(4):
        x0 = -11 + i * 5.5
        _door(m, x0 + 2.75, 0, "s", w=3.6, h=2.6)
        # sloped awning
        m.quad((x0 + 0.3, -2.4, 2.4), (x0 + 5.2, -2.4, 2.4), (x0 + 5.2, 0, 3.1), (x0 + 0.3, 0, 3.1), awn[i],
               uvs=[(0, 1), (1, 1), (1, 0), (0, 0)])
        m.quad((x0 + 5.2, -2.4, 2.4), (x0 + 0.3, -2.4, 2.4), (x0 + 0.3, 0, 3.1), (x0 + 5.2, 0, 3.1), awn[i],
               uvs=[(0, 1), (1, 1), (1, 0), (0, 0)])  # underside
        for px in (x0 + 0.4, x0 + 5.1):
            m.box(px - 0.08, -2.4, 0, px + 0.08, -2.25, 2.4, WOOD, collide=False)
        # goods crates
        m.box(x0 + 1, -1.6, 0, x0 + 2.2, -0.6, 0.8, WOOD, surface=S_WOOD)
    return m


def watchtower():
    m = Mesh()
    m.box(-2.5, -2.5, -1.5, 2.5, 2.5, 9, STONE, top=ROOF, surface=S_STONE)
    for i in range(-2, 3, 2):
        for side in (-1, 1):
            m.box(i - 0.4, side * 2.5 - 0.3, 9, i + 0.4, side * 2.5 + 0.3, 10, STONE, collide=False)
            m.box(side * 2.5 - 0.3, i - 0.4, 9, side * 2.5 + 0.3, i + 0.4, 10, STONE, collide=False)
    _door(m, 0, -2.5, "s", w=1.2, h=2.0)
    _window(m, 0, -2.5, 6, "s", w=0.5, h=0.8)
    return m


def police_station():
    m = Mesh()
    m.box(-9, -6, -1.5, 9, 6, 4.2, PLASTER, top=CONCRETE, surface=S_CONC)
    m.box(-9.2, -6.2, 4.2, 9.2, 6.2, 4.6, CONCRETE, surface=S_CONC)
    _door(m, 0, -6, "s", w=2.0, h=2.6)
    for x in (-6, -3, 3, 6):
        _window(m, x, -6, 1.4, "s", w=1.4, h=1.2)
    # flag pole
    m.cylinder(-7, -8, 0, 10, 0.08, METAL, sides=6, collide=False)
    m.quad((-7, -8, 8.2), (-4.5, -8, 8.2), (-4.5, -8, 9.8), (-7, -8, 9.8), "afg_flag",
           uvs=[(0, 1), (1, 1), (1, 0), (0, 0)])
    m.quad((-4.5, -8, 8.2), (-7, -8, 8.2), (-7, -8, 9.8), (-4.5, -8, 9.8), "afg_flag",
           uvs=[(1, 1), (0, 1), (0, 0), (1, 0)])
    # sandbag post at the door
    m.box(-3, -8.5, 0, 3, -7.5, 1.3, SANDBAG, surface=S_CONC)
    return m


def hesco_wall():
    """10 m HESCO barrier section."""
    m = Mesh()
    for i in range(5):
        x = -5 + i * 2
        m.box(x + 0.02, -0.7, -1.0, x + 1.98, 0.7, 2.3, HESCO, surface=S_CONC)
    return m


def guard_tower():
    m = Mesh()
    for x in (-1.8, 1.8):
        for y in (-1.8, 1.8):
            m.box(x - 0.2, y - 0.2, -1, x + 0.2, y + 0.2, 6, WOOD, surface=S_WOOD)
    m.box(-2.2, -2.2, 6, 2.2, 2.2, 6.3, WOOD, surface=S_WOOD)
    for (a, b, c, d) in [(-2.2, -2.2, 2.2, -1.7), (-2.2, 1.7, 2.2, 2.2), (-2.2, -2.2, -1.7, 2.2), (1.7, -2.2, 2.2, 2.2)]:
        m.box(a, b, 6.3, c, d, 7.4, SANDBAG, surface=S_CONC)
    m.box(-2.4, -2.4, 9.0, 2.4, 2.4, 9.25, METAL, surface=S_METAL)
    for x in (-2.0, 2.0):
        for y in (-2.0, 2.0):
            m.box(x - 0.08, y - 0.08, 7.4, x + 0.08, y + 0.08, 9.0, WOOD, collide=False)
    # ladder
    for z in range(0, 6):
        m.box(-0.5, -2.35, z + 0.4, 0.5, -2.25, z + 0.5, WOOD, collide=False)
    return m


def container():
    m = Mesh()
    m.box(-3.0, -1.2, 0, 3.0, 1.2, 2.6, METAL, surface=S_METAL, shade=0.9)
    return m


def tent(tex=CANVAS):
    m = Mesh()
    w, l, h, r = 3.0, 5.0, 1.6, 3.2
    m.box(-w, -l, 0, w, l, h, tex, surface=0)
    # gable roof
    m.quad((-w, l, h), (-w, -l, h), (0, -l, r), (0, l, r), tex)
    m.quad((w, -l, h), (w, l, h), (0, l, r), (0, -l, r), tex)
    m.tri((-w, -l, h), (w, -l, h), (0, -l, r), tex)
    m.tri((w, l, h), (-w, l, h), (0, l, r), tex)
    m.boxes.append(((-w * 0.6, -l, h), (w * 0.6, l, r), 0))
    return m


def hangar():
    m = Mesh()
    w, l, h = 14, 20, 8
    m.box(-w, -l, -1, w, l, h, METAL, top=METAL, surface=S_METAL)
    # dark opening on the south face
    m.quad((-w + 2, -l - 0.04, 0), (w - 2, -l - 0.04, 0), (w - 2, -l - 0.04, h - 1.5), (-w + 2, -l - 0.04, h - 1.5),
           WINDOW, uvs=[(0, 1), (3, 1), (3, 0), (0, 0)])
    return m


def runway():
    m = Mesh()
    m.box(-15, -150, -1.0, 15, 150, 0.15, CONCRETE, surface=S_CONC, shade=1.0)
    # centre line dashes
    for y in range(-140, 141, 20):
        m.box(-0.4, y, 0.15, 0.4, y + 10, 0.17, TENT_W, collide=False)
    return m


def helipad():
    m = Mesh()
    m.box(-10, -10, -1.0, 10, 10, 0.2, CONCRETE, surface=S_CONC)
    for (a, b, c, d) in [(-4, -5, -2.8, 5), (2.8, -5, 4, 5), (-2.8, -0.6, 2.8, 0.6)]:
        m.box(a, b, 0.2, c, d, 0.22, TENT_W, collide=False)
    return m


def barracks():
    m = Mesh()
    m.box(-10, -4, -1, 10, 4, 3.2, CONCRETE, top=METAL, surface=S_CONC)
    _door(m, 0, -4, "s", w=1.6, h=2.3)
    for x in (-7, -4, 4, 7):
        _window(m, x, -4, 1.2, "s")
    return m


def checkpoint_hut():
    m = Mesh()
    m.box(-2, -2, -1, 2, 2, 2.8, CONCRETE, top=METAL, surface=S_CONC)
    _door(m, 0, -2, "s", w=1.0, h=2.2)
    _window(m, 2, 0, 1.2, "e", w=1.6)
    _window(m, -2, 0, 1.2, "w", w=1.6)
    return m


def barrier():
    """Concrete road block (jersey style)."""
    m = Mesh()
    m.box(-2, -0.4, 0, 2, 0.4, 1.0, CONCRETE, surface=S_CONC)
    return m


def sandbag_wall():
    m = Mesh()
    m.box(-3, -0.5, 0, 3, 0.5, 1.2, SANDBAG, surface=S_CONC)
    return m


def fortress():
    m = Mesh()
    L, H, T = 22.0, 7.5, 1.6
    m.wall(-L, L, L, L, -3, H, T, STONE, ROOF, surface=S_STONE)
    m.wall(-L, -L, -L, L, -3, H, T, STONE, ROOF, surface=S_STONE)
    m.wall(L, -L, L, L, -3, H, T, STONE, ROOF, surface=S_STONE)
    m.wall(-L, -L, -3, -L, -3, H, T, STONE, ROOF, surface=S_STONE)
    m.wall(3, -L, L, -L, -3, H, T, STONE, ROOF, surface=S_STONE)
    m.box(-3, -L - T / 2, H - 1.2, 3, -L + T / 2, H + 0.5, STONE, ROOF, surface=S_STONE)
    for cx in (-L, L):
        for cy in (-L, L):
            m.cylinder(cx, cy, -3, H + 3.5, 3.4, STONE, sides=12, top_tex=ROOF, surface=S_STONE, r_top=3.0)
    # keep
    m.box(-7, 6, -2, 7, 18, 11, STONE, top=ROOF, surface=S_STONE)
    _door(m, 0, 6, "s", w=2, h=2.8)
    for x in (-4, 4):
        _window(m, x, 6, 6.5, "s", w=0.6, h=1.2)
    # walkway ramps up to the walls
    m.box(-L + 0.8, -6, 0, -L + 4, 14, 1.2, STONE, surface=S_STONE)
    return m


def ruin_wall():
    m = Mesh()
    hs = [3.5, 5.2, 2.0, 4.4, 1.2, 3.8]
    for i, h in enumerate(hs):
        m.box(-9 + i * 3, -0.6, -1.5, -6 + i * 3, 0.6, h, MUD, top=MUD, surface=S_MUD)
    return m


def ruin_tower():
    m = Mesh()
    m.cylinder(0, 0, -1.5, 9.0, 3.2, MUD, sides=12, top_tex=ROOF, surface=S_MUD, r_top=2.6)
    m.box(-1.2, -3.4, 7.5, 1.2, -2.6, 10.2, MUD, surface=S_MUD)
    return m


def poplar():
    m = Mesh()
    m.cylinder(0, 0, -0.5, 4, 0.25, BARK, sides=6, collide=True, top_tex=BARK)
    # tall slim canopy: two stacked octagonal cones
    m.cylinder(0, 0, 2.5, 9.0, 1.4, LEAVES, sides=8, collide=False, r_top=1.7)
    m.cylinder(0, 0, 9.0, 15.0, 1.7, LEAVES, sides=8, collide=False, r_top=0.0)
    m.boxes = [((-0.3, -0.3, 0), (0.3, 0.3, 4), S_WOOD)]
    return m


def mulberry():
    m = Mesh()
    m.cylinder(0, 0, -0.5, 2.6, 0.3, BARK, sides=6, collide=False, top_tex=BARK)
    m.dome(0, 0, 2.2, 3.4, LEAVES, sides=10, rings=4, height=3.0)
    m.cylinder(0, 0, 1.6, 2.2, 2.6, LEAVES, sides=10, collide=False, r_top=3.4)
    m.boxes = [((-0.35, -0.35, 0), (0.35, 0.35, 2.6), S_WOOD)]
    return m


def bush():
    m = Mesh()
    m.dome(0, 0, -0.2, 1.1, LEAVES, sides=7, rings=2, height=0.9)
    return m


def haystack():
    m = Mesh()
    m.dome(0, 0, -0.3, 2.2, "afg_mudroof", sides=10, rings=4, height=2.8)
    m.boxes = [((-1.6, -1.6, 0), (1.6, 1.6, 2.2), 0)]
    return m


def well():
    m = Mesh()
    m.cylinder(0, 0, -0.5, 0.9, 1.0, STONE, sides=10, top_tex=WINDOW)
    for x in (-0.9, 0.9):
        m.box(x - 0.08, -0.08, 0.9, x + 0.08, 0.08, 2.3, WOOD, collide=False)
    m.box(-1.0, -0.08, 2.2, 1.0, 0.08, 2.35, WOOD, collide=False)
    return m


def cave_mouth():
    """Rock outcrop with a dark opening - bandit hideout entrance."""
    m = Mesh()
    m.box(-8, -4, -2, 8, 6, 7, "afg_rock", surface=35)
    m.quad((-2, -4.05, 0), (2, -4.05, 0), (2, -4.05, 3.2), (-2, -4.05, 3.2), WINDOW,
           uvs=[(0.1, 0.9), (0.9, 0.9), (0.9, 0.1), (0.1, 0.1)])
    m.box(-6, -6, 0, -3, -4, 1.2, SANDBAG, surface=S_CONC)
    m.box(3, -6, 0, 6, -4, 1.2, SANDBAG, surface=S_CONC)
    return m


# name: (builder, draw distance, footprint radius)
PREFABS = {
    "afg_house_s": (house_small, 220, 6),
    "afg_house_l": (house_large, 240, 7),
    "afg_compound": (compound, 299, 17),
    "afg_mosque": (mosque, 299, 13),
    "afg_shops": (shop_row, 220, 12),
    "afg_wtower": (watchtower, 299, 4),
    "afg_police": (police_station, 250, 11),
    "afg_hesco": (hesco_wall, 220, 6),
    "afg_gtower": (guard_tower, 250, 3),
    "afg_contain": (container, 200, 4),
    "afg_tent": (tent, 200, 6),
    "afg_tent_dk": (lambda: tent("afg_tent_dark"), 200, 6),
    "afg_hangar": (hangar, 299, 25),
    "afg_runway": (runway, 299, 150),
    "afg_helipad": (helipad, 250, 11),
    "afg_barracks": (barracks, 250, 11),
    "afg_cp_hut": (checkpoint_hut, 200, 3),
    "afg_barrier": (barrier, 150, 2),
    "afg_sandbags": (sandbag_wall, 150, 3),
    "afg_fortress": (fortress, 299, 27),
    "afg_ruinwall": (ruin_wall, 220, 10),
    "afg_ruintower": (ruin_tower, 260, 4),
    "afg_poplar": (poplar, 250, 2),
    "afg_mulberry": (mulberry, 200, 3),
    "afg_bush": (bush, 120, 1),
    "afg_haystack": (haystack, 150, 2),
    "afg_well": (well, 120, 1),
    "afg_cave": (cave_mouth, 250, 9),
}
