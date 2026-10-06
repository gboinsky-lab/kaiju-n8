# COMBAT_VFX_AND_DESTRUCTION.md — Efeitos, ataques especiais e destruição

Memória técnica de VFX, ataques e destruição. Leia antes de "Continue os efeitos" ou "Adicione este ataque".

## Decisões aprovadas (Etapa A/B)

- Modelos do Meshy: armas pelo carregador OBJ do NeoForge; personagens com malha presa aos ossos da GeckoLib.
- Escala nova: Yoju 4–8, Honju 6–15, Daikaiju 20–30 blocos. Valores escolhidos: Trichonephila 4,5 × 2,2;
  Primigenius 6 de altura; Honju (revivido/Titã) 9 de altura. — Etapa C
- Destruição: base antecipada para a 0.1 (fila com orçamento, resistência, crateras, áreas protegidas). — Etapa E

## Como um ataque é definido (fluxo)

Cada ataque é um JSON de habilidade (já existe: `bite`, `slam`, `charge`). Campos a acrescentar na Etapa D/E:

```json
{
  "type": "kn8:area_melee", "damage_multiplier": 1.5, "radius": 3.0,
  "windup_ticks": 20, "active_ticks": 4, "cooldown_ticks": 100,
  "animation": "action.slam", "heavy": true,
  "vfx": [{"effect": "kn8:shockwave", "intensity": 1.0}, {"effect": "kn8:dust", "intensity": 0.8}],
  "destruction": {"radius": 3.0, "power": 2, "crater": true},
  "camera_shake": 0.4,
  "sound": "kn8:kaiju.slam"
}
```

O servidor dispara tudo no tick de impacto (mesmo tick do dano); o cliente só desenha. Tipo novo de habilidade no
código só quando a mecânica é nova (ex.: projétil de energia).

## Sistema de efeitos (Etapa D) — ✅ escrito na 0.1-B (ver docs/ATUALIZACAO_0_1_B.md)

`VfxService` (servidor) envia `VfxS2C(efeito, posição, direção, intensidade)` para quem está perto; o cliente tem um
registro de efeitos (Impact, Shockwave, Projectile, Explosion, Slash, Energy, Roar, WeaponFire, SuitRelease) feitos
com partículas e código. Release e Heat alteram a intensidade (aura, brilho, partículas) lendo a `PowerView`/% pública
que já existem.

## Destruição (Etapa E) — ✅ escrita na 0.1-B (ver docs/ATUALIZACAO_0_1_B.md)

`DestructionService` em `KN8Server`: fila de pedidos (centro, raio, força, tipo); orçamento `destruction.blocksPerTick`;
resistência em 5 categorias (frágil, normal, resistente, muito resistente, indestrutível) por tags de bloco em JSON;
crateras irregulares (ruído no raio e na profundidade); áreas protegidas (comando/regiões salvas); registro do que foi
destruído para restauração futura (`rebuildBlocksPerTick`, `rebuildDelayTicks` já no config); destroços como
partículas de bloco (sem milhares de entidades).

## Prompts Meshy

Nenhum efeito precisa de modelo 3D (partículas e código resolvem). Pendentes — ASSET MESHY:

### Bainha da espada única (`bainha_espada`)
```text
Low-poly single straight katana-style sheath for a sci-fi military sword, matching a black blade with a black
wrapped grip. Matte dark olive-green body with black metal caps at both ends, two small belt clips on one side,
a thin cyan accent line along the length. Empty and closed, no blade inside.
Length about 0.75 meter, slim. Oriented along -Z with the opening toward +Z.
Origin at the opening of the sheath (where the blade enters).
500 to 1,000 triangles, one 512x512 base-color texture, no transparency. Export GLB with embedded texture.
```

### Espadas duplas soltas (`espadas_duplas`) — para a animação de sacar
```text
Low-poly PAIR of twin sci-fi military short swords for a Minecraft mod, same design as the reference: dark steel
blades with a lighter edge and a small yellow-green accent near the guard, black wrapped grips.
Two separate swords side by side in the same file, as two separate objects, NO sheaths.
Each sword about 0.8 meter long. Blades pointing toward -Z, edges facing down (-Y).
Origin of each sword at the center of its grip.
800 to 1,500 triangles in total, one 512x512 base-color texture, no transparency. Export GLB with embedded
texture.
```

## Pendências

- Sons próprios (.ogg) para slam, investida, mordida, tiro e rugido (hoje: sons vanilla como placeholder).
- Habilidade de rugido (`kn8:roar`, Honju chama o bando) — M16; o efeito visual `roar` já existe.

- Lista de ataques por kaiju e por arma (nome + uma frase do que faz) — enviar no arquivo de texto.
