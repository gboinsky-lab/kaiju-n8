# PLANO_SOLDADOS_ESPECIAIS.md — Ideia do Miguel (2026-10-07), a implementar quando os modelos chegarem

Nada daqui está implementado ainda, exceto o que está marcado como **feito**. O Miguel vai criar os modelos (soldados
especiais, armas especiais, trajes numerados e novos kaiju); cada um entra numa etapa própria, testada com 2 clientes.

## 1. Soldados comuns — **feito (0.4)**

Variantes só com as armas comuns, sorteadas por peso (`soldier/soldier_1.json`):

| Variante | Papel | Arma | Apoio | Peso |
|---|---|---|---|---|
| `rifle` | Atirador | rifle | faca (troca com o kaiju a < 3,5 blocos) | 4 |
| `pistol` | Patrulheiro | pistola | faca | 2 |
| `sword` | Espadachim | espada | — | 2 |
| `knife` | Batedor | faca | — | 2 |
| `unarmed` | Recruta | — | — | 0 (só pedindo pelo nome) |

Regras: soldados comuns **nunca** usam armas especiais nem trajes numerados. O **machado** saiu dos soldados comuns:
ele é a arma especial de um soldado especial (o jogador continua podendo usá-lo).

## 2. Soldados especiais (próxima frente, com os modelos do Miguel)

- **Nome próprio** cada um (mostrado na barra/nome acima da cabeça e na aba Esquadrão), modelo próprio.
- Mais fortes que os comuns: podem **rivalizar com kaiju numerados**.
- **Habilidades especiais** e **efeitos especiais** (VFX), ligados às armas deles.
- **Variantes por especial**:
  1. traje comum dele (que em alguns é diferente do traje dos soldados comuns) + arma especial;
  2. depois, a variante com o **traje numerado** (feito de kaiju numerado), com ainda mais habilidades.

Arquitetura pensada (a confirmar quando chegar o primeiro modelo):

| Peça | Onde | O que guarda |
|---|---|---|
| `special_soldier/<id>.json` | dados | nome (chave de tradução), modelo, Release, vida, arma especial, variantes (traje comum / numerado), habilidades |
| Entidade | `SpecialSoldierEntity` (ou o `SoldierEntity` com perfil "especial") | IA própria por habilidade, nome fixo, não sorteia variante |
| Habilidades | `ability/*.json` (o mesmo formato dos kaiju: preparo, impacto, recarga, área, VFX, som) | reaproveita a `ActionTimeline` e o `VfxService` |

## 3. Armas especiais

- Usadas pelos soldados especiais **e pelo jogador**.
- Cada uma com **ataque especial** (tecla própria ou golpe carregado) e **efeito especial** (VFX + som).
- Dados no `weapon/<id>.json` (campo novo `special`: habilidade, custo de stamina/calor, recarga) — números em JSON.
- Liberadas pela patente (`unlocks`) e/ou por missão; fabricação na bancada com materiais raros (núcleo intacto).
- Primeira: **o machado atual** (vira arma especial quando o dono dele tiver modelo e nome).

## 4. Trajes numerados

- Feitos a partir de kaiju numerados (ex.: No. 9 → traje numerado 9 [SUPOSIÇÃO de exemplo]).
- Vestidos por alguns soldados especiais como variante e, depois, pelo jogador.
- Dão habilidades especiais próprias (além da armadura e da resistência a calor).
- Render: modelo do Meshy preso aos ossos do jogador (`GeoArmorRenderer`, mesmo plano dos trajes Mk1 —
  `docs/PROMPTS_MESHY_0_3.md`).

## 5. Novos kaiju

- Modelos do Miguel (já combinado: **Trichonephila Honju**). Cada espécie nova: rig, `kaiju/<id>.json`, habilidades,
  bestiário, invasões/missões onde aparece.
- Numerados com efeitos e habilidades especiais (o No. 9 já revive e comanda; os próximos ganham o deles).

## O que eu preciso de cada modelo

- GLB com o nome = id; triângulos e pose como nos prompts (`docs/PROMPTS_MESHY_0_2.md`, `_0_3.md`).
- Para soldado especial: **nome**, papel, arma especial e uma frase sobre a habilidade e o efeito que você imagina.
- Para arma especial: o ataque especial (o que ele faz, alcance, se é área, se tem projétil) e o efeito visual.
