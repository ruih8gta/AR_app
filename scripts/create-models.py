"""Original procedural models. No downloaded or third-party model assets."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def part(p, s, c, shape="sphere", r=0):
    return dict(p=p, s=s, c=c, shape=shape, r=r)


def group(name, pivot, parts):
    return dict(id=name, pivot=pivot, parts=parts)


green, light, belly, dark = "#56866B", "#679A79", "#D6DCB1", "#203C35"
dino = [
    group("body", [0, 0, 0], [
        part([0, .76, 0], [.34, .47, .42], green),
        part([0, .79, .29], [.25, .34, .19], belly),
        *[part([x, .91, .28], [.085, .19, .085], green, r=24 if x > 0 else -24) for x in [-.31, .31]],
        *[part([x, .78, .36], [.08, .06, .11], belly) for x in [-.31, .31]],
    ]),
    group("head", [0, 1.12, .17], [
        part([0, .22, .1], [.33, .32, .4], light),
        part([0, .13, .39], [.3, .19, .28], light),
        part([0, -.025, .35], [.27, .065, .29], belly),
        *[part([x, .3, .27], [.065, .08, .075], "#FFF8E9") for x in [-.288, .288]],
        *[part([x, .3, .29], [.025, .045, .045], dark) for x in [-.332, .332]],
        *[part([x, .21, .619], [.026, .025, .02], dark) for x in [-.14, .14]],
    ]),
    group("tail", [0, .64, -.3], [
        part([0, .02, -.34], [.23, .19, .46], green),
        part([0, .09, -.72], [.12, .11, .3], light),
    ]),
]
for name, x in [("legL", -.22), ("legR", .22)]:
    dino.append(group(name, [x, .39, .02], [
        part([0, 0, 0], [.18, .27, .2], "#47765D"),
        part([0, -.26, .11], [.12, .13, .15], green),
        part([0, -.32, .23], [.16, .07, .24], green),
    ]))

orange, cream, brown = "#D98A45", "#F3E4C9", "#513C35"
fox = [
    group("body", [0, 0, 0], [
        part([0, .5, 0], [.26, .29, .46], "#C97739"),
        part([0, .52, .32], [.22, .26, .16], cream),
    ]),
    group("head", [0, .76, .32], [
        part([0, .13, .09], [.29, .29, .29], orange),
        *[part([x, .42, .06], [.12, .25, .12], orange, "cone", -13 if x > 0 else 13) for x in [-.17, .17]],
        *[part([x, .43, .12], [.065, .17, .05], brown, "cone", -13 if x > 0 else 13) for x in [-.17, .17]],
        *[part([x, -.01, .26], [.16, .11, .2], cream) for x in [-.13, .13]],
        part([0, .045, .42], [.065, .055, .05], "#303934"),
        *[part([x, .18, .28], [.034, .05, .035], "#303934") for x in [-.21, .21]],
    ]),
    group("tail", [0, .42, -.32], [
        part([0, .06, -.36], [.21, .23, .48], "#C97739"),
        part([0, .08, -.72], [.16, .17, .26], cream),
    ]),
]
for name, x, z in [("legL", -.17, .27), ("legR", .17, .27), ("backL", -.17, -.27), ("backR", .17, -.27)]:
    fox.append(group(name, [x, .25, z], [
        part([0, -.06, 0], [.08, .19, .09], brown),
        part([0, -.2, .03], [.08, .05, .12], brown),
    ]))

catalog = [
    dict(id="dino", name="ティラノ", subtitle="ちいさな恐竜、大きな好奇心。", groups=dino),
    dict(id="fox", name="キツネ", subtitle="ふさふさのしっぽを、すぐそばに。", groups=fox),
]
assets = ROOT / "app/src/main/assets"
assets.mkdir(parents=True, exist_ok=True)
data = json.dumps(catalog, ensure_ascii=False, separators=(",", ":"))
(assets / "creatures.json").write_text(data, encoding="utf-8")
template = ROOT / "demo/palm-safari.template.html"
if template.exists():
    (ROOT / "demo/palm-safari.html").write_text(
        template.read_text(encoding="utf-8").replace("__CREATURE_CATALOG__", data), encoding="utf-8")
print(f"Generated {len(catalog)} original models, {sum(len(g['parts']) for m in catalog for g in m['groups'])} parts.")
