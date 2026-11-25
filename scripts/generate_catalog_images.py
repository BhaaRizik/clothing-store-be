import os
import sys

W, H = 600, 800


def shadow(cx=300, cy=700, rx=170, ry=18):
    return f'<ellipse cx="{cx}" cy="{cy}" rx="{rx}" ry="{ry}" fill="#000" opacity="0.07"/>'


def hanger(y=0):
    return (
        f'<g fill="none" stroke="#9A8F84" stroke-width="7" stroke-linecap="round" stroke-linejoin="round" transform="translate(0,{y})">'
        '<path d="M300 165 V140 Q300 112 322 112 Q342 112 342 132"/>'
        '<path d="M300 165 L195 222 M300 165 L405 222"/></g>'
    )


def ol(stroke):
    return f'stroke="{stroke}" stroke-width="4" stroke-linejoin="round" stroke-linecap="round"'


def oxford_shirt(p, f='#A9C8E8', s='#5F86AD', collar='#BFD6EE', button='#F4F8FC'):
    buttons = ''.join(f'<circle cx="300" cy="{y}" r="6" fill="{button}" stroke="{s}" stroke-width="2"/>' for y in (335, 395, 455, 515, 575, 630))
    return (
        hanger(5) + shadow(rx=150) +
        f'<path d="M245 222 L180 245 L125 320 L100 600 L152 607 L180 400 L180 660 L420 660 L420 400 L448 607 L500 600 L475 320 L420 245 L355 222 Z" fill="{f}" {ol(s)}/>'
        f'<path d="M103 568 L154 575 M497 568 L446 575" stroke="{s}" stroke-width="3"/>'
        f'<rect x="205" y="330" width="62" height="68" rx="5" fill="none" stroke="{s}" stroke-width="3"/>'
        f'<path d="M300 292 V660" stroke="{s}" stroke-width="3"/>'
        f'<path d="M245 222 L300 292 L268 305 L232 242 Z M355 222 L300 292 L332 305 L368 242 Z" fill="{collar}" {ol(s)}/>'
        f'<circle cx="262" cy="294" r="4" fill="{s}"/><circle cx="338" cy="294" r="4" fill="{s}"/>' + buttons
    )


def chinos(p, f='#D9C4A0', s='#9C8559', stitch=None, rise=0):
    st = stitch or s
    dash = ' stroke-dasharray="7 6"' if stitch else ''
    top = 170 - rise
    return (
        shadow(rx=140) +
        f'<path d="M200 {top + 38} L183 668 L287 668 L300 345 L313 668 L417 668 L400 {top + 38} Z" fill="{f}" {ol(s)}/>'
        f'<rect x="200" y="{top}" width="200" height="40" rx="4" fill="{f}" {ol(s)}/>'
        + ''.join(f'<rect x="{x}" y="{top - 4}" width="10" height="48" rx="3" fill="{f}" stroke="{s}" stroke-width="3"/>' for x in (222, 290, 368))
        + f'<circle cx="300" cy="{top + 20}" r="6" fill="#C9CDD2" stroke="{s}" stroke-width="2"/>'
        f'<path d="M300 {top + 40} V{top + 150} Q300 {top + 165} 285 {top + 170}" fill="none" stroke="{st}" stroke-width="3"{dash}/>'
        f'<path d="M212 {top + 44} Q248 {top + 60} 246 {top + 115} M388 {top + 44} Q352 {top + 60} 354 {top + 115}" fill="none" stroke="{st}" stroke-width="3"{dash}/>'
        f'<path d="M188 642 L284 642 M316 642 L412 642" stroke="{st}" stroke-width="3"{dash}/>'
    )


def jeans(p):
    return chinos(p, f='#5B82B5', s='#34557F', stitch='#E0B868', rise=25)


def overcoat(p, f='#4A4F57', s='#2C3036', lapel='#5C626B', double=False, belt=None, epaulettes=False):
    if double:
        buttons = ''.join(f'<circle cx="{x}" cy="{y}" r="9" fill="{s}"/>' for x in (262, 338) for y in (380, 450, 520))
    else:
        buttons = ''.join(f'<circle cx="300" cy="{y}" r="9" fill="{s}"/>' for y in (410, 490, 570))
    b = ''
    if belt:
        b = (f'<rect x="168" y="455" width="264" height="30" rx="4" fill="{belt}" {ol(s)}/>'
             f'<rect x="282" y="449" width="36" height="42" rx="5" fill="none" stroke="#6B5130" stroke-width="5"/>'
             f'<path d="M318 470 L345 470 L360 560 L338 562 Z" fill="{belt}" {ol(s)}/>')
    ep = ''
    if epaulettes:
        ep = (f'<path d="M188 212 L238 190 L242 204 L194 226 Z M412 212 L362 190 L358 204 L406 226 Z" fill="{lapel}" {ol(s)}/>')
    return (
        shadow(cy=752, rx=160) +
        f'<path d="M230 170 L172 200 L132 300 L110 610 L162 616 L177 380 L170 730 L430 730 L423 380 L438 616 L490 610 L468 300 L428 200 L370 170 Z" fill="{f}" {ol(s)}/>'
        f'<path d="M113 580 L163 586 M487 580 L437 586" stroke="{s}" stroke-width="3"/>'
        f'<path d="M300 335 V730" stroke="{s}" stroke-width="3"/>'
        f'<path d="M232 171 L300 335 L268 352 L206 228 Z M368 171 L300 335 L332 352 L394 228 Z" fill="{lapel}" {ol(s)}/>'
        f'<path d="M190 560 L248 556 M352 556 L410 560" stroke="{s}" stroke-width="4"/>'
        + buttons + b + ep
    )


def trench(p):
    return overcoat(p, f='#C9A26B', s='#8B6A3C', lapel='#D6B482', double=True, belt='#B08850', epaulettes=True)


def windbreaker(p):
    f, s, d = '#7A8450', '#4E5533', '#69723F'
    strips = '<path d="M117 488 L167 494 M483 488 L433 494" stroke="#E8ECEF" stroke-width="9" stroke-linecap="round"/>'
    return (
        shadow(cy=672, rx=150) +
        f'<path d="M232 205 Q222 108 300 98 Q378 108 368 205 Q300 175 232 205 Z" fill="{d}" {ol(s)}/>'
        f'<path d="M235 205 L176 228 L132 322 L106 570 L157 578 L179 392 L181 630 L419 630 L421 392 L443 578 L494 570 L468 322 L424 228 L365 205 Q300 245 235 205 Z" fill="{f}" {ol(s)}/>'
        f'<path d="M300 226 V630" stroke="{s}" stroke-width="5"/>'
        f'<rect x="293" y="252" width="14" height="26" rx="3" fill="#C9CFC0" stroke="{s}" stroke-width="2"/>'
        f'<path d="M330 305 L385 292" stroke="{s}" stroke-width="4"/>'
        f'<path d="M181 600 L419 600 M108 545 L158 552 M492 545 L442 552" stroke="{s}" stroke-width="3"/>'
        f'<path d="M268 212 L262 262 M332 212 L338 262" stroke="#E8ECEF" stroke-width="3"/>'
        f'<circle cx="262" cy="266" r="5" fill="#E8ECEF"/><circle cx="338" cy="266" r="5" fill="#E8ECEF"/>'
        + strips +
        '<path d="M186 340 L272 340 M328 340 L414 340" stroke="#E8ECEF" stroke-width="7" stroke-linecap="round"/>'
    )


def denim_vest(p):
    f, s, st = '#6F8FB8', '#3F5D82', '#E0B868'
    return (
        shadow(cy=632, rx=130) +
        f'<path d="M240 195 L192 212 Q172 300 186 365 L186 610 L414 610 L414 365 Q428 300 408 212 L360 195 L300 340 Z" fill="{f}" {ol(s)}/>'
        f'<path d="M300 340 V610" stroke="{s}" stroke-width="3"/>'
        + ''.join(f'<circle cx="300" cy="{y}" r="7" fill="#C9A86A" stroke="{s}" stroke-width="2"/>' for y in (380, 440, 500, 560))
        + f'<path d="M212 300 H270 V335 H212 Z M330 300 H388 V335 H330 Z" fill="#5F7FA8" {ol(s)}/>'
        f'<path d="M200 230 Q186 300 198 360 M400 230 Q414 300 402 360" fill="none" stroke="{st}" stroke-width="3" stroke-dasharray="7 6"/>'
        f'<path d="M190 585 H410" stroke="{st}" stroke-width="3" stroke-dasharray="7 6"/>'
    )


def floral_dress(p):
    f, s = '#E8A7B5', '#A65D6E'
    def flower(x, y, r=11, c='#FFF6EE'):
        petals = ''.join(f'<circle cx="{x + dx}" cy="{y + dy}" r="{r * 0.55:.1f}" fill="{c}"/>' for dx, dy in ((0, -r * .7), (r * .67, -r * .2), (r * .42, r * .58), (-r * .42, r * .58), (-r * .67, -r * .2)))
        return petals + f'<circle cx="{x}" cy="{y}" r="{r * 0.35:.1f}" fill="#F2C14E"/>'
    flowers = ''.join(flower(x, y, r) for x, y, r in (
        (262, 420, 12), (330, 450, 10), (240, 520, 13), (300, 540, 11), (365, 520, 12), (215, 620, 12), (280, 630, 10),
        (345, 615, 13), (395, 650, 10), (265, 270, 9), (330, 300, 8), (245, 690, 9), (330, 690, 9)))
    leaves = ''.join(f'<ellipse cx="{x}" cy="{y}" rx="9" ry="4" fill="#7FA77A" transform="rotate({a} {x} {y})"/>' for x, y, a in (
        (280, 410, 30), (315, 470, -20), (258, 545, 40), (382, 540, -30), (300, 645, 20), (365, 600, 35)))
    return (
        hanger(5) + shadow(cy=725, rx=150) +
        f'<path d="M225 212 L182 252 L206 288 L220 266 Z M375 212 L418 252 L394 288 L380 266 Z" fill="{f}" {ol(s)}/>'
        f'<path d="M256 200 L224 212 L216 262 L232 362 L368 362 L384 262 L376 212 L344 200 Q300 250 256 200 Z" fill="{f}" {ol(s)}/>'
        f'<path d="M232 362 L168 705 Q300 735 432 705 L368 362 Z" fill="{f}" {ol(s)}/>'
        + leaves + flowers +
        f'<rect x="226" y="350" width="148" height="22" rx="6" fill="#D98D9E" {ol(s)}/>'
        f'<path d="M240 361 Q200 330 205 360 Q200 392 240 361 Z" fill="#D98D9E" {ol(s)}/>'
        f'<path d="M238 365 L214 440 M244 366 L236 448" stroke="{s}" stroke-width="7" stroke-linecap="round"/>'
        f'<path d="M238 365 L214 440 M244 366 L236 448" stroke="#D98D9E" stroke-width="3" stroke-linecap="round"/>'
    )


def cable_sweater(p, f='#EFE6D2', s='#B3A47F', d='#D9CCAE'):
    def cable(x):
        segs = ''.join(f' C{x + 18} {y + 15} {x + 18} {y + 35} {x} {y + 50}' for y in range(260, 580, 50))
        segs2 = ''.join(f' C{x - 18} {y + 15} {x - 18} {y + 35} {x} {y + 50}' for y in range(260, 580, 50))
        return (f'<path d="M{x} 260{segs}" fill="none" stroke="{d}" stroke-width="7"/>'
                f'<path d="M{x} 260{segs2}" fill="none" stroke="{d}" stroke-width="7"/>')
    ribs = ''.join(f'<path d="M{x} 612 V640" stroke="{d}" stroke-width="3"/>' for x in range(196, 410, 12))
    return (
        shadow(cy=665, rx=165) +
        f'<path d="M240 190 L180 210 L130 300 L105 600 L162 612 L182 400 L185 640 L415 640 L418 400 L438 612 L495 600 L470 300 L420 210 L360 190 Q300 228 240 190 Z" fill="{f}" {ol(s)}/>'
        + cable(250) + cable(300) + cable(350) + ribs +
        f'<path d="M185 610 H415 M106 578 L163 588 M494 578 L437 588" stroke="{s}" stroke-width="3"/>'
        f'<path d="M240 190 Q300 232 360 190" fill="none" stroke="{d}" stroke-width="16"/>'
        f'<path d="M240 190 Q300 232 360 190" fill="none" stroke="{s}" stroke-width="3"/>'
    )


def tank_top(p, f='#FFFFFF', s='#9AA2AC', rib='#E3E7EC'):
    cid = f'{p}tank'
    ribs = ''.join(f'<path d="M{x} 160 V640" stroke="{rib}" stroke-width="3"/>' for x in range(196, 410, 13))
    shape = 'M226 168 L258 168 Q300 280 342 168 L374 168 Q376 262 402 302 L406 630 L194 630 L198 302 Q224 262 226 168 Z'
    return (
        shadow(cy=655, rx=130) +
        f'<clipPath id="{cid}"><path d="{shape}"/></clipPath>'
        f'<path d="{shape}" fill="{f}"/>'
        f'<g clip-path="url(#{cid})">{ribs}</g>'
        f'<path d="{shape}" fill="none" {ol(s)}/>'
        f'<path d="M196 605 H404" stroke="{s}" stroke-width="2"/>'
    )


def _shoe(dx, dy, upper, s, sole, extra='', tall=False, scale=1.0):
    path = ('M150 520 L152 425 Q158 398 192 395 L262 390 Q302 386 332 420 L420 466 Q472 478 482 510 L482 540 L150 540 Z')
    return (f'<g transform="translate({dx},{dy}) scale({scale})">'
            f'<rect x="140" y="528" width="350" height="34" rx="14" fill="{sole}" {ol(s)}/>'
            f'<path d="{path}" fill="{upper}" {ol(s)}/>' + extra + '</g>')


def sneakers(p):
    s = '#8E959D'
    laces = ''.join(f'<path d="M{262 + i * 18} {398 + i * 11} L{248 + i * 18} {420 + i * 11}" stroke="{s}" stroke-width="4"/>' for i in range(5))
    extra = (laces + f'<path d="M150 470 Q200 478 236 470" fill="none" stroke="#D5D9DE" stroke-width="3"/>'
             f'<path d="M152 425 L176 420 L178 520 L150 520 Z" fill="#E6E2D6" {ol(s)}/>'
             f'<path d="M150 528 H490" stroke="#D5D9DE" stroke-width="3"/>')
    return (shadow(cy=605, rx=210) +
            _shoe(50, -70, '#F1F2F3', s, '#E4E6E8', extra, scale=0.92) +
            _shoe(-10, 20, '#FDFDFD', s, '#EEF0F2', extra))


def running_trainers(p, upper='#9EA5AD', upper_back='#B4BAC1', s='#5F666E', dots='#7E858D'):
    mesh = ''.join(f'<circle cx="{x}" cy="{y}" r="2.6" fill="{dots}"/>' for x in range(200, 330, 16) for y in range(440, 510, 16))
    laces = ''.join(f'<path d="M{262 + i * 18} {398 + i * 11} L{248 + i * 18} {420 + i * 11}" stroke="#F4F5F6" stroke-width="4"/>' for i in range(5))
    extra = (mesh + laces +
             f'<path d="M152 425 Q170 470 160 520 L150 520 Z" fill="#F26B5B" {ol(s)}/>'
             f'<path d="M300 505 Q360 470 440 495" fill="none" stroke="#F26B5B" stroke-width="8" stroke-linecap="round"/>'
             f'<path d="M140 545 Q300 556 490 545" fill="none" stroke="#D5D9DE" stroke-width="3"/>')
    return (shadow(cy=612, rx=210) +
            _shoe(50, -70, upper_back, s, '#FAFAFA', extra, scale=0.92) +
            f'<g transform="translate(-10,14)"><rect x="140" y="534" width="350" height="44" rx="18" fill="#FAFAFA" {ol(s)}/></g>' +
            _shoe(-10, 20, upper, s, '#FAFAFA', extra))


def slip_ons(p):
    s = '#1C2642'
    path = 'M150 520 L152 470 Q160 440 200 440 L300 438 Q350 440 400 466 Q470 480 482 510 L482 540 L150 540 Z'
    gore = f'<path d="M232 440 L246 470 L260 440 Z" fill="#46557F" {ol(s)}/>'
    def shoe(dx, dy, sc):
        return (f'<g transform="translate({dx},{dy}) scale({sc})">'
                f'<rect x="140" y="528" width="350" height="34" rx="12" fill="#F4F4F4" {ol(s)}/>'
                f'<path d="M140 548 H490" stroke="#C8423A" stroke-width="4"/>'
                f'<path d="{path}" fill="#2F3E66" {ol(s)}/>'
                f'<path d="M190 446 Q300 470 400 468" fill="none" stroke="#46557F" stroke-width="3" stroke-dasharray="6 5"/>'
                + gore + '</g>')
    return shadow(cy=605, rx=210) + shoe(50, -70, 0.92) + shoe(-10, 20, 1.0)


def chelsea_boots(p):
    s = '#5A3823'
    path = 'M205 220 L322 220 L326 462 Q352 494 420 512 Q474 524 478 566 L478 590 L190 590 L192 500 Q198 380 205 220 Z'
    def boot(dx, dy, sc, f):
        return (f'<g transform="translate({dx},{dy}) scale({sc})">'
                f'<path d="M195 222 Q182 196 205 192 L212 222 Z" fill="{f}" {ol(s)}/>'
                f'<path d="{path}" fill="{f}" {ol(s)}/>'
                f'<path d="M238 230 Q262 330 246 430 L292 430 Q276 330 300 230 Z" fill="#5E3C28" {ol(s)}/>'
                + ''.join(f'<path d="M{x} 236 V424" stroke="#4A2F1F" stroke-width="2"/>' for x in (256, 268, 280))
                + f'<rect x="186" y="586" width="296" height="22" rx="6" fill="#3B2618" {ol(s)}/>'
                f'<rect x="188" y="586" width="62" height="40" rx="5" fill="#3B2618" {ol(s)}/>'
                '</g>')
    return shadow(cy=650, rx=200) + boot(70, -60, 0.92, '#7B4E33') + boot(-20, 20, 1.0, '#8A5A3C')


def leather_belt(p):
    f, s = '#7B4A2A', '#4E2E19'
    holes = ''.join(f'<circle cx="{300 + 150 * c:.1f}" cy="{400 + 150 * sn:.1f}" r="5" fill="{s}"/>' for c, sn in ((-0.5, 0.866), (-0.64, 0.766), (-0.77, 0.64), (-0.87, 0.5)))
    return (
        shadow(cy=610, rx=190) +
        f'<circle cx="300" cy="400" r="100" fill="none" stroke="{s}" stroke-width="50"/>'
        f'<circle cx="300" cy="400" r="100" fill="none" stroke="{f}" stroke-width="42"/>'
        f'<circle cx="300" cy="400" r="150" fill="none" stroke="{s}" stroke-width="50"/>'
        f'<circle cx="300" cy="400" r="150" fill="none" stroke="{f}" stroke-width="42"/>'
        f'<circle cx="300" cy="400" r="150" fill="none" stroke="#C08A5E" stroke-width="2" stroke-dasharray="8 7"/>'
        f'<circle cx="300" cy="400" r="100" fill="none" stroke="#C08A5E" stroke-width="2" stroke-dasharray="8 7"/>'
        + holes +
        f'<rect x="420" y="352" width="66" height="96" rx="14" fill="none" stroke="#7F868F" stroke-width="16"/>'
        f'<rect x="420" y="352" width="66" height="96" rx="14" fill="none" stroke="#C3C9D0" stroke-width="10"/>'
        f'<path d="M424 400 H470" stroke="#C3C9D0" stroke-width="8" stroke-linecap="round"/>'
    )


def beanie(p, f='#8E949C', s='#5E636A', d='#7D838B'):
    ribs_dome = ''.join(f'<path d="M{x} 470 Q{300 + (x - 300) * 0.55:.0f} 300 300 232" fill="none" stroke="{d}" stroke-width="3"/>' for x in range(190, 420, 22))
    ribs_cuff = ''.join(f'<path d="M{x} 470 V575" stroke="{d}" stroke-width="5"/>' for x in range(178, 425, 14))
    return (
        shadow(cy=598, rx=160) +
        f'<path d="M172 480 Q166 236 300 226 Q434 236 428 480 Z" fill="{f}" {ol(s)}/>' + ribs_dome +
        f'<rect x="164" y="465" width="272" height="115" rx="22" fill="{f}" {ol(s)}/>' + ribs_cuff +
        f'<rect x="164" y="465" width="272" height="115" rx="22" fill="none" {ol(s)}/>'
    )


def silk_scarf(p):
    cid = f'{p}scarf'
    sq = 'M300 210 L470 380 L300 550 L130 380 Z'
    print_ = ('<rect x="100" y="180" width="400" height="400" fill="#264653"/>'
              '<circle cx="250" cy="330" r="70" fill="#E9C46A"/>'
              '<circle cx="360" cy="440" r="85" fill="#E76F51"/>'
              '<path d="M120 470 Q250 360 480 300" fill="none" stroke="#2A9D8F" stroke-width="26"/>'
              '<path d="M150 280 Q300 230 430 470" fill="none" stroke="#F4E9D8" stroke-width="8"/>'
              '<circle cx="330" cy="290" r="22" fill="#2A9D8F"/>')
    return (
        shadow(cy=640, rx=170) +
        f'<clipPath id="{cid}"><path d="{sq}"/></clipPath>'
        f'<g clip-path="url(#{cid})">{print_}</g>'
        f'<path d="{sq}" fill="none" stroke="#1D3640" stroke-width="10" stroke-linejoin="round"/>'
        f'<path d="M130 380 L300 550 L470 380 L300 600 Z" fill="#E76F51" stroke="#1D3640" stroke-width="6" stroke-linejoin="round"/>'
        f'<path d="M190 440 L300 545 L410 440 L300 575 Z" fill="#E9C46A" opacity="0.9"/>'
        f'<path d="M130 380 L470 380" stroke="#1D3640" stroke-width="3" opacity="0.35"/>'
    )


def sunglasses(p, fr='#1F1F23', spots=None):
    gid = f'{p}lens'
    return (
        shadow(cy=520, rx=200) +
        f'<defs><linearGradient id="{gid}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#2E3440"/><stop offset="1" stop-color="#6B7889"/></linearGradient></defs>'
        f'<path d="M120 352 L70 330 M480 352 L530 330" stroke="{fr}" stroke-width="12" stroke-linecap="round"/>'
        f'<rect x="120" y="340" width="160" height="120" rx="44" fill="url(#{gid})" stroke="{fr}" stroke-width="14"/>'
        f'<rect x="320" y="340" width="160" height="120" rx="44" fill="url(#{gid})" stroke="{fr}" stroke-width="14"/>'
        f'<path d="M280 372 Q300 352 320 372" fill="none" stroke="{fr}" stroke-width="12"/>'
        f'<path d="M150 362 L182 362 M350 362 L382 362" stroke="#FFFFFF" stroke-width="6" stroke-linecap="round" opacity="0.45"/>'
        + (''.join(f'<circle cx="{x}" cy="{y}" r="{r}" fill="{spots}"/>' for x, y, r in (
            (150, 340, 4), (196, 341, 5), (246, 339, 3), (126, 400, 4), (170, 460, 5), (232, 461, 4),
            (352, 340, 5), (404, 339, 3), (452, 341, 4), (474, 402, 5), (380, 460, 4), (440, 461, 3),
            (300, 362, 4))) if spots else '')
    )


ITEMS = {
    'men-classic-oxford-shirt': (oxford_shirt, '#EEF3F8', 'Light blue Oxford shirt on a hanger'),
    'men-slim-chino-trousers': (chinos, '#F5F1EA', 'Beige slim chino trousers'),
    'men-wool-blend-overcoat': (overcoat, '#ECEDEF', 'Charcoal wool overcoat'),
    'men-lightweight-performance-hooded-windbreaker-jacket-with-packable-pocket': (windbreaker, '#EFF1E8', 'Olive hooded windbreaker jacket'),
    'men-retired-denim-vest': (denim_vest, '#EDF1F6', 'Washed denim vest'),
    'women-floral-midi-dress': (floral_dress, '#FBEFF1', 'Floral print midi dress'),
    'women-cable-knit-sweater': (cable_sweater, '#F2EFEA', 'Cream cable knit sweater'),
    'women-high-rise-straight-jeans': (jeans, '#EDF1F6', 'Mid-blue straight leg jeans'),
    'women-belted-trench-coat': (trench, '#F6F0E6', 'Camel trench coat with belt'),
    'women-ribbed-tank-top': (tank_top, '#E9EDF2', 'White ribbed tank top'),
    'shoes-white-leather-sneakers': (sneakers, '#E8EEF0', 'Pair of white leather sneakers'),
    'shoes-chelsea-boots': (chelsea_boots, '#F4EEE8', 'Brown suede Chelsea boots'),
    'shoes-running-trainers': (running_trainers, '#EEF0F3', 'Grey running trainers'),
    'shoes-canvas-slip-ons': (slip_ons, '#EEF1F7', 'Navy canvas slip-on shoes'),
    'acc-leather-belt': (leather_belt, '#F5EFE9', 'Brown leather belt with buckle'),
    'acc-wool-beanie': (beanie, '#EFF0F2', 'Grey ribbed wool beanie'),
    'acc-silk-scarf': (silk_scarf, '#F6F1E8', 'Printed silk scarf folded'),
    'acc-classic-sunglasses': (sunglasses, '#EFEDEA', 'Black acetate sunglasses'),
}


COLOR_ITEMS = {
    'men-classic-oxford-shirt': (oxford_shirt, '#EEF3F8', [
        ('Light blue', 'Light blue Oxford shirt on a hanger', {}),
        ('White', 'White Oxford shirt on a hanger',
         {'f': '#FAFBFC', 's': '#7D8793', 'collar': '#EEF1F4', 'button': '#DDE2E8'}),
    ]),
    'men-slim-chino-trousers': (chinos, '#F5F1EA', [
        ('Beige', 'Beige slim chino trousers', {}),
        ('Navy', 'Navy slim chino trousers', {'f': '#2B3A5C', 's': '#141D33'}),
    ]),
    'women-cable-knit-sweater': (cable_sweater, '#F2EFEA', [
        ('Cream', 'Cream cable knit sweater', {}),
        ('Grey', 'Grey cable knit sweater', {'f': '#9EA3A8', 's': '#5F656B', 'd': '#868B91'}),
    ]),
    'women-ribbed-tank-top': (tank_top, '#E9EDF2', [
        ('White', 'White ribbed tank top', {}),
        ('Black', 'Black ribbed tank top', {'f': '#1E1F22', 's': '#000000', 'rib': '#3A3C40'}),
    ]),
    'shoes-running-trainers': (running_trainers, '#EEF0F3', [
        ('Grey', 'Grey running trainers', {}),
        ('Black', 'Black running trainers',
         {'upper': '#2A2C30', 'upper_back': '#3A3D42', 's': '#111214', 'dots': '#55595F'}),
    ]),
    'acc-wool-beanie': (beanie, '#EFF0F2', [
        ('Grey', 'Grey ribbed wool beanie', {}),
        ('Navy', 'Navy ribbed wool beanie', {'f': '#2B3A5C', 's': '#141D33', 'd': '#22304D'}),
    ]),
    'acc-classic-sunglasses': (sunglasses, '#EFEDEA', [
        ('Black', 'Black acetate sunglasses', {}),
        ('Tortoiseshell', 'Tortoiseshell acetate sunglasses', {'fr': '#8B5A2B', 'spots': '#3E2410'}),
    ]),
}


def color_slug(color_name):
    return color_name.lower().replace(' ', '-')


def compose(parts, prefix):
    return ''.join(f'<g transform="translate({dx},{dy}) scale({sc})">{fn(prefix + str(i))}</g>' for i, (fn, dx, dy, sc) in enumerate(parts))


CATEGORIES = {
    'category-men': ('#E6EAF0', 'Men: overcoat and Oxford shirt',
                     [(overcoat, 150, 40, 0.68), (oxford_shirt, -10, 180, 0.6)]),
    'category-women': ('#F8E9EC', 'Women: trench coat and floral dress',
                       [(trench, 150, 40, 0.68), (floral_dress, -10, 180, 0.6)]),
    'category-shoes': ('#E8ECEE', 'Shoes: Chelsea boots and white sneakers',
                       [(chelsea_boots, 90, -40, 0.75), (sneakers, 40, 200, 0.85)]),
    'category-accessories': ('#F3EEE6', 'Accessories: beanie, sunglasses and belt',
                             [(beanie, 40, -40, 0.6), (leather_belt, 250, -20, 0.5), (sunglasses, 45, 140, 0.85)]),
}


def svg(title, bg, body):
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" height="{H}" role="img">'
            f'<title>{title}</title><rect width="{W}" height="{H}" fill="{bg}"/>{body}</svg>\n')


def main(out_dir, sheet_path):
    os.makedirs(out_dir, exist_ok=True)
    cells = []
    for slug, (fn, bg, title) in ITEMS.items():
        body = fn(slug + '-')
        with open(os.path.join(out_dir, f'{slug}.svg'), 'w') as fh:
            fh.write(svg(title, bg, body))
        cells.append((bg, body))
    for slug, (fn, bg, colors) in COLOR_ITEMS.items():
        for color_name, title, palette in colors:
            file_slug = f'{slug}-{color_slug(color_name)}'
            body = fn(file_slug + '-', **palette)
            with open(os.path.join(out_dir, f'{file_slug}.svg'), 'w') as fh:
                fh.write(svg(title, bg, body))
            cells.append((bg, body))
    for slug, (bg, title, parts) in CATEGORIES.items():
        body = compose(parts, slug + '-')
        with open(os.path.join(out_dir, f'{slug}.svg'), 'w') as fh:
            fh.write(svg(title, bg, body))
        cells.append((bg, body))
    cols, cw, ch = 5, W // 2, H // 2
    rows = (len(cells) + cols - 1) // cols
    inner = ''.join(
        f'<g transform="translate({(i % cols) * cw},{(i // cols) * ch}) scale(0.5)"><rect width="{W}" height="{H}" fill="{bg}"/>{body}</g>'
        for i, (bg, body) in enumerate(cells))
    with open(sheet_path, 'w') as fh:
        fh.write(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {cols * cw} {rows * ch}" width="{cols * cw}" height="{rows * ch}">{inner}</svg>')
    print(f'wrote {len(cells)} svgs')


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
