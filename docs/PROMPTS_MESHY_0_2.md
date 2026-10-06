# Prompts do Meshy — modelos novos (0.2, 2026-10-06)

Para todos os mobs **menos** o soldado, a aranha (`trichonephila`) e o Primigenius Yoju (`primigenius`), que
continuam como estão. Mesmo estilo dos três aprovados: **"vibe Minecraft"**, ou seja, formas em blocos, cantos
retos e textura em pixel art.

## Regras que valem para todos (o rig automático depende disso)

| Item | Por quê |
|---|---|
| **Nome do arquivo = id** (`primigenius_resurrected.glb` etc.), mesmo nome de antes | O conversor acha pelo nome e o jogo já espera esse id |
| **Pose A**: braços abertos uns 30–40° do corpo, cotovelos levemente dobrados, pernas afastadas, de pé | O rig separa braço, antebraço e perna pela forma; braço colado no corpo vira tronco |
| **Boca um pouco aberta** (mandíbula separada da cabeça) | Osso `jaw` (rugido e mordida) |
| **Cauda para trás e para cima**, sem encostar no chão nem nas pernas | Osso `tail` separado das pernas |
| Frente olhando para **+Z** (de frente para a câmera do Meshy) | Mesmo eixo dos outros modelos |
| **Uma peça só**, sem base, sem chão, sem arma, sem texto | O conversor não tira base nem chão |
| Textura **sem degradê fino** (cores chapadas em blocos) | O Minecraft não usa mipmap; detalhe fino vira chiado |
| Triângulos no alvo da tabela (pedir já no Meshy) | O mod não precisa reduzir e não abre buracos |

Os textos abaixo estão em inglês porque o Meshy entende melhor. Se o campo tiver limite de 600 caracteres, cada
prompt cabe. O campo "evitar" vai no negative prompt, se houver; se não houver, acrescente no fim do prompt como
"no ...".

---

## 1. `primigenius_resurrected.glb` — Primigenius ressurgido (Yoju revivido pelo No. 9)

| Altura | Largura (ombros) | Triângulos | Cores |
|---|---|---|---|
| 6 m | ~3,1 m | 8–10 mil | verde-musgo/verde-tóxico, ossos pálidos, costuras verde-neon |

Mesmo corpo do Primigenius (bípede bruto com cauda), mas "remontado" pelo Kaiju No. 9.

```
Blocky voxel-style kaiju monster, Minecraft aesthetic, low-poly cubic shapes with pixel-art texture. Hulking
bipedal reptilian brute, 6 meters tall, hunched back, massive arms with three-clawed hands, thick legs, long
tail pointing back and up. Reanimated corpse look: sickly moss-green armored skin, cracked plates, exposed pale
rib bones on one side, glowing neon-green stitched seams across chest and arms, one glowing eye. Mouth slightly
open with blocky teeth. A-pose, arms 35 degrees away from body, legs apart, standing. Single mesh, no base.
```

Evitar: `smooth organic surface, realistic skin, base, ground, weapon, text, thin spikes, arms touching body`

---

## 2. `primigenius_honju.glb` — Primigenius Honju ("Titã Bruto")

| Altura | Largura (ombros) | Triângulos | Cores |
|---|---|---|---|
| 9 m | ~5,7 m | 10–12 mil | marrom-rocha, placas cinza-escuras, chifres osso, núcleo laranja no peito |

Regra do projeto: o Honju é **outra criatura**, não um Yoju maior. Aqui ele é mais largo, de chifres, com
antebraços enormes.

```
Blocky voxel-style giant kaiju boss, Minecraft aesthetic, cubic low-poly shapes, pixel-art texture. Colossal
bipedal titan, 9 meters tall, very wide shoulders, gorilla-like oversized forearms with huge blocky fists, short
thick legs, short heavy tail. Two large curved horns on the head, jaw slightly open with square fangs.
Rock-like brown hide covered by dark gray armor slabs, glowing orange core visible in the center of the chest.
A-pose, arms 30 degrees away from body, legs apart, standing upright. Single mesh, no base.
```

Evitar: `smooth organic surface, realistic, quadruped, knuckle walking, base, ground, text, thin details`

---

## 3. `primigenius_revived.glb` — Primigenius revivido (Honju revivido pelo No. 9)

| Altura | Largura (ombros) | Triângulos | Cores |
|---|---|---|---|
| 9 m | ~5,7 m | 10–12 mil | roxo-escuro/violeta, ossos pálidos, costuras e núcleo magenta brilhante |

Mesmo corpo do Honju (o rig reaproveita os cortes), com cara de "trazido de volta".

```
Blocky voxel-style giant kaiju boss, Minecraft aesthetic, cubic low-poly shapes, pixel-art texture. Colossal
bipedal titan, 9 meters tall, very wide shoulders, oversized forearms with blocky fists, short thick legs, short
heavy tail. Reanimated look: dark purple and violet hide, cracked armor slabs, one horn broken off, exposed pale
ribs on the side, glowing magenta stitched seams and a pulsing magenta core in the chest, glowing eyes. Jaw
slightly open. A-pose, arms 30 degrees away from body, legs apart, standing. Single mesh, no base.
```

Evitar: `smooth organic surface, realistic, quadruped, base, ground, text, thin details, arms touching body`

---

## 4. `kaiju_no9.glb` — Kaiju No. 9 (opcional: o modelo atual já está convertido)

| Altura | Largura | Triângulos | Cores |
|---|---|---|---|
| 2,0 m | ~0,8 m | ~8 mil | cinza-osso e preto, detalhes em verde-ácido (a cor das costuras dos revividos) |

Só refaça se quiser o No. 9 no mesmo estilo em blocos dos outros. As costuras verde/magenta dos revividos
"assinam" o trabalho dele, por isso o verde-ácido.

```
Blocky voxel-style humanoid kaiju, Minecraft aesthetic, cubic low-poly shapes, pixel-art texture. Slender
intelligent monster, 2 meters tall, human proportions, long thin limbs, segmented bone-gray exoskeleton with
black gaps, elongated head with a smooth faceless mask and two narrow glowing acid-green eye slits, ribbed
chest, clawed fingers, small acid-green glowing veins on arms. Calm, calculating posture. A-pose, arms 35
degrees away from body, legs slightly apart. Single mesh, no base.
```

Evitar: `bulky, tail, wings, cape, clothes, weapon, base, ground, text, realistic skin`

---

## Depois de gerar

1. Salve com o nome exato da tabela, na mesma pasta dos outros GLB.
2. Me mande o arquivo. Eu converto (`meshy_convert.py`), meço os cortes do rig (`rig_primigenius_mesh.py`, tabela
   `SPECIES`) e testo no jogo antes de mandar.
3. Se o Meshy colar os braços no corpo ou a cauda no chão, gere de novo antes de mandar. É o que mais atrapalha
   o rig.
