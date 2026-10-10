# Balanceamento v1.2 aplicado — todos os kaiju e personagens (2026-10-10)

Pedido do Miguel: "faça o balanceamento agora de todos que temos até agora no mod junto com as habilidades deles",
com `docs/BALANCEAMENTO_COMPLETO_v1_2.md` (números e matriz de vitória) e `docs/BIBLIOTECA_HABILIDADES_KAIJU.md`
(habilidades, poderes e sistemas; cópia do arquivo enviado).

- **Fichas de cada entidade** (vida, dano, armadura, velocidade, cada habilidade com dano, preparo, recarga, alcance,
  efeito e origem): `docs/FICHAS_HABILIDADES.md`, gerado dos JSON por `tools/docs/gen_fichas_habilidades.py`
  (rodar de novo sempre que mudar um JSON; o arquivo nunca fica diferente do jogo).
- Tabela geral com todos os números do jogo continua em `docs/BALANCEAMENTO.md`.

---

## 1. O que mudou (antes → agora)

### Kaiju

Os 10 kaiju da tabela 3 do v1.2 (aranha, 4 Primigenius, Tecedeira, Preondactyl, No. 10 pequeno e gigante, No. 9)
**já estavam** com os números do v1.2 (entraram com o Balanceamento v1.0, que tem os mesmos valores). Os kaiju novos
da 0.7 (Philinosoma, Diclonius, formigas, cogumelos, larva) não estão no v1.2: continuam com os números da tabela de
tiers [SUPOSIÇÃO], listados nas fichas.

Mudaram as **formas do No. 9** (v1.2 seções 5–7):

| Forma | Vida | Dano | Armadura | Velocidade | Numerado (regeneração, pele endurecida, análise) |
|---|---|---|---|---|---|
| Preta | 1.700 → **2.200** | 36 → **42** | 18 → **17** | 0,36 | regenera 3% / 6% → **3% / 5,5%** por s; pele 25% → **15%** dos golpes (−50%, 2 s); revive 3 → **4**; análise −3% por golpe repetido, até −25% |
| Fundida ao No. 10 | 3.200 → **5.400** | 46 → **58** | 19 | 0,36 → **0,38** | regenera 3% → **3,5%** (6% abaixo de 20%); pele 20% → **10%** (−40%); análise −3%, até −25% |
| Fundida à formiga | 2.400 → **3.200** | 38 → **46** | 17 → **18** | 0,42 → **0,45** | regenera 2,5% / 5% → **3,2%** fixo; pele 15% → **5%**; análise −2%, até −15% |
| No. 9 base | 1.450 | 34,5 | 16 | 0,34 | sem mudança nos números; **ganhou a análise** (−2% por golpe repetido, até −20%) |

### Habilidades novas (v1.2 + Biblioteca)

| Habilidade | Quem | O que faz | Origem |
|---|---|---|---|
| Análise / adaptação (`adaptation` no `numbered/*.json`) | No. 9 e as 3 formas | cada golpe repetido do mesmo tipo (tipo de dano + arma/projétil/atacante) tira menos: −2/−3% por golpe, até −15/−25%; esquece depois de 5–6 s sem esse golpe. Trocar de ataque zera | ADAPTAÇÃO (v1.2 §4: "analisa e se adapta") |
| Multi-Finger Gun | forma preta | 3 tiros em leque de 16° (×0,75 cada), 5–26 blocos, recarga 3,5 s | ADAPTAÇÃO (v1.2 §5) |
| Multi-Finger Cannon | fundida ao No. 10 | 5 tiros em leque de 30° (×0,9, explosão 2), 5–30 blocos, recarga 7,5 s | MOD ORIGINAL (v1.2 §6) |
| Finger Cannon herdado | fundida ao No. 10 | o canhão do No. 10 (×1,5, explosão 2) no lugar do Finger Gun | ADAPTAÇÃO (v1.2 §6: "herda") |
| Voo curto | forma preta | salto longo de 7–18 blocos (×1,0, raio 2,5), recarga 8 s | ADAPTAÇÃO (v1.2 §5) |
| Ant Rush | fundida à formiga | investida de 4–20 blocos (×1,4, fura bloqueio), recarga 4,5 s | MOD ORIGINAL (v1.2 §7) |

Rajada em leque vale para qualquer `kn8:projectile` com `hits` > 1 e `arc_degrees` < 360 (sem Java por kaiju).

### Personagens (dano contra kaiju e armas)

Vida, armadura e velocidade de todos os personagens já eram as da tabela 12 do v1.2. A calibragem mexeu só no que o
v1.2 manda mexer primeiro (dano e habilidade, não vida):

| O quê | Antes | Agora | Por quê (duelos abaixo) |
|---|---|---|---|
| Punho do No. 8 (`weapon/kaiju_no8_fist.json`) | 30 | **11** | com Release 100% (×5) o golpe base dá **55**, o "dano base equivalente 55" do v1.2 §11; com 30 dava 150 e o No. 8 matava o No. 10 pequeno em 5 s |
| No. 8 `kaiju_damage` | 1,0 | **0,6** | vencia o No. 10 gigante em 12–17 s |
| Kikoru `kaiju_damage` | 0,52 | **0,62** | 0/10 contra o No. 9 depois que ele ganhou a análise |
| Kikoru No. 4 | 0,7 | **0,5** | 10/10 contra o No. 9 |
| Narumi | 0,62 | **0,52** | 10/10 contra o No. 9 |
| Reno No. 6 | 0,8 | **0,75** | 9/10 contra o No. 9 |
| Mina: Anti-Giant Shot | ×4,0, recarga 20 s | **×10,0, recarga 15 s** | v1.2 §13: tiro anti-Daikaiju decisivo |
| Mina: canhão carregado | ×2,2 | **×3,0** | idem |

Hoshina e Hoshina + No. 10 **não mudaram** (v1.2 §33: "não alterar sem teste"; os duelos deles batem com o v1.0).

---

## 2. Matriz medida × meta do v1.2 (10 duelos por confronto)

Método: servidor dedicado, duelo solo em terreno aberto, os dois de vida cheia a 16 blocos (Mina a 40), `/tick rate`
acelerado, sem jogador por perto. As formas do No. 9 foram testadas **isoladas** (pacote de dados do mundo sem
`transform`/`absorb`), senão o No. 9 vira a forma preta no meio do duelo. Script: `duel12.py` (scratchpad da sessão).

- **Morte** = o kaiju morreu. **Fuga** = o kaiju fugiu com pouca vida (No. 9 e formas fogem com 15%, a fusão com 10%,
  o No. 10 com a regra dele). **Derrota** = o personagem morreu. **Tempo** = 5 min sem fim.
- O v1.2 §36.1 diz "vitória significa derrotar o Kaiju sem morrer". Na tabela, "vitória" conta **morte + fuga**
  (o No. 9 foge sempre antes de morrer: sem contar a fuga, ninguém vence o No. 9 nunca). **[DECIDIR]** se a fuga conta.

| Personagem × kaiju | Meta | Vitória (morte + fuga) | Morte / Fuga / Derrota / Tempo | Tempo da luta | Vida do personagem ao vencer | Situação |
|---|---|---|---|---|---|---|
| Hoshina × No. 9 | 70% | **100%** | 0 / 10 / 0 / 0 | 40–48 s | 26–71% | +30, mantido (§33) |
| Hoshina × No. 10 pequeno | 58% | **80%** | 7 / 1 / 2 / 0 | 43–55 s | 6–42% | +22, mantido (§33) |
| Hoshina × No. 10 gigante | 10% | **0%** | 0 / 0 / 10 / 0 | 81–110 s | — (gigante com 11–31%) | ok |
| Hoshina + No. 10 × No. 9 | 90% | **100%** | 0 / 10 / 0 / 0 | 26–32 s | 66–81% | ok |
| Hoshina + No. 10 × No. 10 gigante | 60% | **100%** | 10 / 0 / 0 / 0 | 54–60 s | 20–72% | +40, mantido (§33) |
| Hoshina + No. 10 × No. 9 preto | 45% | **40%** | 0 / 4 / 6 / 0 | 40–61 s | 4–24% | ok |
| Hoshina + No. 10 × No. 9 formiga | 35% | **20%** | 0 / 2 / 7 / 1 | 37–60 s | 2–21% | ok (−15) |
| Hoshina + No. 10 × fusão No. 9 + No. 10 | 18% | **0%** | 0 / 0 / 10 / 0 | 26–57 s | — (fusão com 64–91%) | −18 |
| Kaiju No. 8 × No. 10 pequeno | 75% | **90%** | 7 / 2 / 1 / 0 | 23–30 s | 76–83% | ok |
| Kaiju No. 8 × No. 10 gigante | 45% | **70%** | 3 / 4 / 3 / 0 | 64–103 s | 50–73% | +25 |
| Kaiju No. 8 × No. 9 | 40% | **100%** | 0 / 10 / 0 / 0 | 23–30 s | 84–93% | +60 só por fuga (0% de morte) |
| Kaiju No. 8 × No. 9 preto | 20% | **70%** | 0 / 7 / 0 / 3 | 40–71 s | 50–99% | +50 só por fuga |
| Kaiju No. 8 × No. 9 formiga | 15% | **100%** | 0 / 10 / 0 / 0 | 53–191 s | 48–57% | +85 só por fuga |
| Kaiju No. 8 × fusão | 8% | **20%** | 0 / 2 / 4 / 4 | 107–300 s | 49–64% | ok |
| Kikoru × No. 9 | 25% | **50%** | 0 / 5 / 5 / 0 | 37–46 s | 1–20% | +25 (no limite: ver 3) |
| Kikoru × No. 10 pequeno | 35% | **0%** | 0 / 0 / 10 / 0 | 20–42 s | — (No. 10 com 13–63%) | −35 (ver 3) |
| Kikoru + No. 4 × No. 9 | 75% | **80%** | 0 / 8 / 2 / 0 | 58–81 s | 1–45% | ok |
| Mina × No. 10 gigante (a 40 blocos) | 30% | **0%** | 0 / 0 / 10 / 0 | 36–50 s | — (gigante com 61–71%) | −30 (IA, ver 3) |
| Reno × No. 9 | 12% | **0%** | 0 / 0 / 10 / 0 | 32–59 s | — (No. 9 com 65–79%) | ok |
| Reno + No. 6 × No. 9 | 70% | **60%** | 0 / 6 / 4 / 0 | 57–101 s | 1–61% | ok |
| Narumi × No. 9 | 35% | **60%** | 0 / 6 / 4 / 0 | 40–52 s | 2–14% | +25 (no limite: ver 3) |
| Narumi + No. 1 × No. 9 | 85% | **100%** | 0 / 10 / 0 / 0 | 26–31 s | 54–65% | ok |

Resumo: **10 na meta** (±15 pontos), **3 do Hoshina acima** mantidos de propósito (§33) e **9 fora**, com a causa na
seção 3.

Confrontos da matriz que **não dá para medir ainda**: Isao/No. 2, os 20 personagens secundários (Iharu, Eiji, Rin...)
e as formas K14–K16 do No. 9 (vermelha, No. 2/Isao, final): não existem no jogo (sem modelo). Contra os kaiju comuns
(K1–K7) os personagens especiais vencem com folga em todos os testes anteriores (Kikoru vence o Honju marrom em
11–12 s; Mina derruba o Primigenius em ~9 s); não foram repetidos 10 vezes.

---

## 3. Por que alguns confrontos ficaram fora da meta (regra 36.7.4: achar a causa antes de mexer em número)

1. **A fuga decide quase tudo contra o No. 9.** Ele foge com 15% em todas as formas; por isso o resultado vira
   "chegar a 15% antes de morrer". Kikoru (vence com 1–20% de vida) e Narumi (2–14%) estão no fio da navalha: com 10
   duelos, ±15 pontos são ruído. Com o No. 8 é o contrário: ele leva qualquer forma do No. 9 a 15% e o No. 9 sempre
   escapa (0% de morte). Se a fuga **não** contar, o No. 8 fica com 0% contra o No. 9 (meta 40%). **[DECIDIR]**.
2. **Regeneração do No. 8 (3%/s abaixo de 50%, v1.2 §11) = 54 de vida por segundo.** Ele "estaciona" em 50%: só a
   fusão (que também regenera a 50%) tem dano para passar disso, e 4 dos 10 duelos com ela deram 5 min de empate. A
   Biblioteca §5 diz que a regeneração "não deve tornar o combate interminável". Proposta **[DECIDIR]**: regeneração do
   No. 8 só fora de combate ou 1,5%/s em combate. Não mudei sem você aprovar (é número do v1.2).
3. **Kikoru: as duas metas brigam.** O No. 10 pequeno é mais forte que o No. 9 contra ela (golpes pesados, que furam o
   bloqueio, e regeneração). Com `kaiju_damage` 0,52 ela fazia 0% nos dois; com 0,62, 50% e 0%. Para chegar a 35%
   contra o No. 10 ela passaria muito do No. 9. Valor escolhido: 0,62 (ela "pode vencer ocasionalmente", v1.2 §36.9).
4. **Mina × No. 10 gigante: falta IA de atiradora.** Mesmo começando a 40 blocos, o gigante chega nela em ~10 s e ela
   não recua enquanto atira (a IA do soldado especial só fecha distância). O Anti-Giant Shot já subiu para ×10 (sai
   de 2 a 3 vezes por luta); subir mais o dano só esconderia a falta de recuo. Pendência: recuar/manter distância
   (kiting) na IA da Mina e do Reno.
5. **Hoshina + No. 10 × fusão (0%).** A fusão tem 5.400 de vida (v1.2 §6) e o Hoshina tira ~15–35% dela antes de
   morrer. A meta é 18%: falta ~1 vitória em 5. Não mexi no Hoshina (§33) nem na vida da fusão (é do v1.2).
   Pendência: medir com a cauda do Hoshina mirando o núcleo.
6. **No. 8 × No. 9 preto: 3 empates de 5 min com os dois quase de vida cheia** (92% e 98%): os dois pararam de lutar.
   Causa ainda não achada (suspeita: a forma preta longe depois do voo curto e o No. 8 sem alvo). Pendência de IA,
   não de número.

---

## 4. Auditoria da Biblioteca de habilidades (o que ela lista × o que o jogo tem)

| Sistema / habilidade da Biblioteca | No jogo | Status |
|---|---|---|
| Núcleo (core) com dano extra e exposição | `PartEntity` núcleo, técnicas que expõem o núcleo | existe |
| Uni-organ / órgãos especiais | — | ausente (precisa de definição) |
| Fortitude como escala de ameaça | `fortitude` + `overrides` em todo `kaiju/*.json` | existe |
| No. 8: força, velocidade, dash, soco pesado, golpe no chão, rugido, regeneração | `special_soldier/kaiju_no8.json` | existe |
| No. 8: agarrar e arremessar (Grab/Throw) | — | ausente |
| No. 8: detecção de kaiju no HUD | — | ausente |
| No. 8: quebrar estruturas com o soco | golpe no chão explode sem quebrar blocos | incompleto [DECIDIR] |
| No. 1 (previsão) | só como arma numerada do Narumi (aparar/contra-golpe previsto) | adaptação |
| No. 2 (destruição e energia) / Isao | — | ausente (sem modelo) |
| No. 9: análise e adaptação | `adaptation` (esta etapa) | existe |
| No. 9: absorver o No. 10 e virar outra forma | `absorb` (vivo ou carcaça, por tentáculos) → fusão | existe |
| No. 9: forma vermelha/evoluída depois do No. 10 | — | ausente (sem modelo) |
| No. 10: comanda kaiju, forma gigante, invasão nível 6 | `No10Service`, `no10_assault` | existe |
| Evento No. 10 derrotado → No. 9 aparece e o absorve | No. 9 de surpresa no nível 6 + absorção | parcial (sem a forma vermelha) |
| Kaiju ressuscitados (revividos pelo No. 9, mais fortes) | ressurgido/revivido + `mass_revive` | existe |
| IA: preparo visível, prioridade por distância, fúria | `behavior`, `particles`, `rage` | existe |
| Destruição ambiental por tamanho | `breakWhileWalking`, explosões de habilidade | existe |
| Daikaiju | — | ausente (nenhum ainda) |

---

## 5. Pendências

- **[DECIDIR]** fuga do No. 9 conta como vitória? (muda a leitura de 9 confrontos da matriz)
- **[DECIDIR]** regeneração do No. 8 em combate (proposta: 1,5%/s ou só fora de combate).
- IA de atirador (Mina, Reno): recuar e manter distância.
- No. 8 × No. 9 preto: achar por que param de lutar; agarrar/arremessar; detecção de kaiju; soco que quebra blocos.
- Medir os confrontos com 20 duelos onde ficou no limite (Kikoru, Narumi × No. 9), como pede o §36.7.2.
- Personagens e formas sem modelo (Isao/No. 2, secundários, No. 9 vermelho/No. 2/final): só depois dos modelos.
- Hoshina + No. 10: velocidade 0,30 no jogo, 0,35 no v1.2 §12 (não mudei: "não alterar sem teste").
