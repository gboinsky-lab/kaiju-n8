# Fichas de IA dos personagens (IA-3)

Plano de IA, Partes 2.2, 4.3 e 9. Cada ficha separa **CANON** (o que a obra mostra), **INFERÊNCIA** (leitura para
gameplay) e **MOD ORIGINAL** (decisão do Miguel ou do mod). Os números ficam no bloco `tactics` (e `heavy_guard`) do
`special_soldier/<id>.json`; nada aqui muda vida, dano ou armadura.

**Fontes consultadas (2026-10-16):** o site oficial (`kaiju-no8.net`) não abriu a partir do servidor desta sessão
(falha de DNS), então o CANON abaixo é o resumo amplamente conhecido da obra, conferido em fontes secundárias:
[Sportskeeda — 10 strongest characters](https://sportskeeda.com/anime/10-strongest-kaiju-no-8-characters-ranked),
[Sportskeeda — First Division Captain](https://sportskeeda.com/anime/who-is-the-first-division-captain-of-kaiju-no-8),
[Beebom — best characters](https://beebom.com/kaiju-no-8-best-characters/amp/),
[Dexerto FR — personagens](https://www.dexerto.fr/anime/kaiju-no-8-tous-les-personnages-principaux-vus-jusqua-present-dans-lanime-1554616/).
**[DECIDIR]** conferir no site oficial e no mangá quando houver acesso; tudo que não é CANON está marcado.

| Campo do `tactics` | O que faz |
|---|---|
| `aggression` | depois de uma técnica, chance de continuar pressionando (senão reposiciona por `reposition_ticks`) |
| `preferred_distance`, `lateral_step` | para onde reposiciona: passo para o lado em volta do alvo, ou de volta à distância preferida |
| `damage_tolerance`, `safety_distance`, `retreat_ticks` | com a vida abaixo da fração, recua até a distância de segurança por um tempo |
| `heavy_vs_big`, `fast_vs_small` | prioridade extra das técnicas pesadas contra kaiju grande e das rápidas contra Yoju |
| `dodge_chance` | chance de esquivar do golpe que vem nele |
| `melee_counter` | usa o contra-ataque corpo a corpo (atiradores não) |
| `protect_ally`, `big_target` | bônus (blocos) na escolha de alvo: kaiju que mira um aliado; kaiju grande |
| `heavy_guard` | defende golpe pesado de frente, mas leva `factor` do dano (Miguel, 2026-10-16) |

## Soshiro Hoshina (`hoshina`, `hoshina_no10`)
- **CANON:** vice-capitão da 3ª Divisão; duas lâminas, velocidade e precisão; família de caçadores de kaiju com
  espadas; mais forte contra kaiju pequenos e médios do que contra os gigantes; Release limitado com armas de fogo.
- **INFERÊNCIA:** distância curta, sequências rápidas, passos laterais entre os golpes (`lateral_step`, `aggression`
  0,6), técnicas rápidas preferidas contra Yoju (`fast_vs_small` 2), recua com 25% da vida.
- **MOD ORIGINAL:** guarda contra golpe pesado (½ do dano); No. 10 com a cauda que guarda os lados (`[combat]
  tailGuardBlocksHeavy`, ligado por decisão do Miguel).

## Kikoru Shinomiya (`kikoru`, `kikoru_no4`)
- **CANON:** prodígio, machado grande, agressiva e confiante; filha de Isao; usa o traje numerado 4 (asas, voo).
- **INFERÊNCIA:** avança com decisão (`aggression` 0,85; No. 4 0,9), alterna golpes rápidos e pesados (pesado contra
  grande, rápido contra pequeno), passo para o flanco, esquiva um pouco menos que o Hoshina (0,9).
- **MOD ORIGINAL:** guarda contra golpe pesado; quebra blocos com os golpes pesados (IA-2).

## Mina Ashiro (`mina`)
- **CANON:** capitã da 3ª Divisão; canhão de grosso calibre; derruba kaiju gigantes de longe; fraca com lâmina.
- **INFERÊNCIA:** prioriza alvo grande (`big_target` 12, `heavy_vs_big` 2), não usa o contra-ataque de espada
  (`melee_counter` false: bug #16), recua para 24 blocos com 30% da vida, esquiva menos (0,6). Não atira através de
  paredes (técnica de tiro exige linha de visão).
- **MOD ORIGINAL:** Anti-Giant contra qualquer kaiju com 4 tiros (Miguel, IA-2).

## Reno Ichikawa (`reno`, `reno_no6`)
- **CANON:** recruta disciplinado, atirador, aprende rápido, apoia a equipe; usa o traje numerado 6 (gelo).
- **INFERÊNCIA:** protege aliados (`protect_ally` 10: prefere o kaiju que está atacando alguém), sem contra-ataque de
  espada, recua com 30% da vida (No. 6: 25%).
- **MOD ORIGINAL:** gelo expõe o núcleo e deixa o kaiju vulnerável (2026-10-15).

## Gen Narumi (`narumi`, `narumi_no1`)
- **CANON:** capitão da 1ª Divisão; baioneta com estilo que mistura cortes, estocadas e tiros; com o No. 1 lê os
  movimentos do inimigo.
- **INFERÊNCIA:** prioriza ameaças de alto risco (`big_target` 6; No. 1 8), alterna pressão e reposicionamento
  (`aggression` 0,75; No. 1 0,8; passo lateral), pesado contra grande. A leitura não é perfeita: a esquiva continua
  com o tempo de reação do perfil.
- **MOD ORIGINAL:** guarda contra golpe pesado (Miguel citou o Narumi).

## Kafka Hibino (`kafka`) e Kaiju No. 8 (`kaiju_no8`)
- **CANON:** Kafka é limpador de kaiju que entra na Força de Defesa; conhece a anatomia dos kaiju; vira o Kaiju No. 8
  (força explosiva, socos, combate de perto).
- **INFERÊNCIA (Kafka humano):** evita enfrentar sozinho (`aggression` 0,4, recua com 60% da vida para 14 blocos),
  protege aliados (6), sem contra-ataque de espada.
- **INFERÊNCIA (No. 8):** fecha distância e pressiona (`aggression` 0,95), pesado contra grande (2), rápido contra
  pequeno (1), esquiva pouco (0,5: aguenta o golpe).
- **MOD ORIGINAL:** guarda contra golpe pesado no No. 8 (Miguel citou o No. 8); troca de forma pela vida.

## Isao Shinomiya (`isao`)
- **CANON:** diretor-geral da Força de Defesa, força pesada, arma numerada 2 (manoplas com ondas de choque).
- **INFERÊNCIA:** postura estável, golpes comprometidos, pressão de perto (`aggression` 0,85), pesado contra grande
  (2), protege aliados (10).
- **MOD ORIGINAL:** guarda contra golpe pesado (45%); quebra blocos com o Thunder Emperor e o Force Blast (IA-2).
  Ficha completa, fontes e as duas criaturas novas da 0.8-A em `docs/FICHAS_0_8_A.md`.
