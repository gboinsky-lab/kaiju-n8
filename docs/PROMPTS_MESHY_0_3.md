# Prompts do Meshy — 0.3 (2026-10-07)

Mesmo estilo dos modelos aprovados: **"vibe Minecraft"** (formas em blocos, cantos retos, textura em pixel art).
Nome do arquivo = id no jogo. Mande o GLB que eu converto, faço o rig, testo com 2 clientes e mando prints.

## 1. Trichonephila Honju (`trichonephila_honju.glb`) — o Miguel vai criar

Conceito escolhido: **Tecedeira Abissal**. Regras para o rig de aranha (o mesmo da Trichonephila):

| Item | Por quê |
|---|---|
| 8 patas bem separadas umas das outras e do corpo | Cada pata vira um osso; patas coladas viram uma peça só |
| Patas abertas para os lados, apoiadas no chão (pose de andar parada) | O rig mede a junta de cada pata pela forma |
| Corpo (cefalotórax + abdômen) acima do chão, sem encostar | Senão o corpo "atravessa" o chão ao andar |
| Quelíceras/presas na frente, um pouco abertas | Osso da mordida |
| Frente olhando para +Z (de frente para a câmera do Meshy) | Mesmo eixo dos outros modelos |
| 10–12 mil triângulos, uma peça só, sem base | Igual aos Honju |
| Tamanho pensado: ~8–9 m de envergadura, ~3–4 m de altura | Honju 6–15 blocos (escala aprovada) |
| Textura em blocos, sem degradê fino | O Minecraft não suaviza texturas |

Regra do Miguel: Honju e Yoju são **criaturas diferentes** — não precisa parecer a Trichonephila maior.

## 2. Trajes da Força de Defesa

Render no jogador: modelo cortado em tronco/braços/pernas e preso nos ossos do jogador (`GeoArmorRenderer` da
GeckoLib). Armadura, resistência e receita continuam as dos JSONs; só muda o visual.

| Regra (vale para os 3) | Por quê |
|---|---|
| **Sem cabeça e sem capacete** (termina na gola) | A cabeça e a skin do jogador continuam aparecendo |
| **Proporção de jogador do Minecraft** (tronco quadrado, braços e pernas retangulares, ~1,9 m) | Precisa caber no boneco do jogador |
| **Braços abertos ~20°, sem encostar no tronco**; pernas um pouco afastadas | Separar braço/tronco e perna esquerda/direita (eu endireito os braços depois) |
| **4–6 mil triângulos** | Vários jogadores usando ao mesmo tempo |
| Textura em blocos, sem degradê fino; uma peça só, sem base | Estilo dos outros modelos |

### `mk1.glb` — Traje de Combate Mk1 (começar por este)

```
Blocky voxel-style armored combat suit, Minecraft aesthetic, low-poly cubic shapes, pixel-art texture. Sci-fi
military bodysuit worn by an invisible person (no head, no face, no helmet, suit ends at the collar). Black
bodysuit with white and light gray armor plates on chest, shoulders, forearms, thighs and shins, thin glowing
cyan lines along the arms, chest and legs, compact cyan core light in the center of the chest, armored gloves
and boots. Minecraft player proportions: square torso, rectangular arms and legs. Arms slightly open 20 degrees
from the body, not touching the torso, legs slightly apart, standing. Single mesh, no base.
```

Evitar: `head, face, helmet, cape, weapon, backpack, base, ground, text, smooth realistic surface, arms touching body`

### `mk1_reinforced.glb` — Mk1 Reforçado

```
Blocky voxel-style heavy armored combat suit, Minecraft aesthetic, low-poly cubic shapes, pixel-art texture.
Reinforced version of a black sci-fi military bodysuit: thicker dark gray and white armor plates, large blocky
shoulder pads, armored chest plate with an orange glowing core, orange glowing lines on arms and legs, heavy
gauntlets and armored boots. No head, no face, no helmet, suit ends at the collar. Minecraft player proportions:
square torso, rectangular arms and legs. Arms slightly open 20 degrees from the body, not touching the torso,
legs slightly apart, standing. Single mesh, no base.
```

### `training_suit.glb` — Traje de Treino (opcional)

```
Blocky voxel-style training uniform, Minecraft aesthetic, low-poly cubic shapes, pixel-art texture. Simple
olive-green padded training jumpsuit with light gray knee pads, elbow pads and a padded vest, dark belt, simple
gloves and boots, no glowing parts. No head, no face, no helmet, suit ends at the collar. Minecraft player
proportions: square torso, rectangular arms and legs. Arms slightly open 20 degrees from the body, not touching
the torso, legs slightly apart, standing. Single mesh, no base.
```

Se o Meshy colar os braços no corpo ou desenhar cabeça, gere de novo antes de mandar.
