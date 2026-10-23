# Atualizações do Plano Mestre (PM-0 a PM-8D) — resumo único

Tudo o que entrou no mod a partir do Plano Mestre de Ideias do Miguel (`docs/PLANO_MESTRE_IDEIAS_KAIJU_N8_REBORN.md`),
reunido num arquivo só. Todas as etapas são de 2026-10-23, estão no branch `claude/laughing-maxwell-tbl62q` e foram
testadas com servidor dedicado + 2 clientes (pt_br e en_us). Os testes automáticos (GameTests) passam: **270/270**.

Detalhes técnicos de cada etapa: `CLAUDE.md` (decisões e status), números em `docs/BALANCEAMENTO.md`, passo a passo
de teste em `docs/ROTEIRO_TESTE_0_1_B.md` (§86 a §96), imagens em `docs/img/pm*.jpg`.

**[SUPOSIÇÃO]** = número que eu escolhi porque ainda não havia decisão (raio, tempo, custo, bônus). O sistema
funciona com ele, mas fica marcado para o Miguel aprovar ou trocar; todos ficam em JSON ou no config
(`config/kn8-server.toml`), sem mexer no código.

## Índice

| Etapa | O que é | Roteiro | Imagem |
|---|---|---|---|
| PM-0 | Auditoria do Plano Mestre (só leitura) | — | — |
| PM-1 | Esquiva perfeita e funções das armas | §86 | `pm1_combate.jpg` |
| PM-2 | Ferimentos por parte dos kaiju | §87 | `pm2_ferimentos.jpg` |
| PM-3 | Kaiju inteligentes leem os hábitos do alvo | §88 | `pm3_habitos.jpg` |
| PM-4 | Terreno por espécie e papéis em grupo | §89 | `pm4_grupo_e_terreno.jpg` |
| PM-5 | Fases dos chefes | §90 | `pm5_fases_do_chefe.jpg` |
| PM-6 | Ordens Focar e Guarda do esquadrão | §91 | `pm6_esquadrao_ordens.jpg` |
| PM-7 | Provas de promoção e avaliação pós-missão | §92 | `pm7_provas_e_avaliacao.jpg` |
| PM-8 | Oficiais de missões/provas e estação de preparação | §93 | `pm8_oficial_e_estacao.jpg` |
| PM-8B | Provas dentro da base (diferentes das missões) | §94 | `pm8b_provas_na_base.jpg` |
| PM-8C | Pesquisa no laboratório | §95 | `pm8c_pesquisa.jpg` |
| PM-8D | Base modular do jogador | §96 | `pm8d_base_modular.jpg` |

---

## PM-0 — Auditoria

Os 16 sistemas do documento foram comparados com o código: 13 já existiam em parte, 3 não existiam (avaliação
pós-missão, evacuação, apoio militar), nenhum quebrado. Daí saiu a ordem PM-1 a PM-11
(`docs/PLANO_MESTRE_AUDITORIA.md`).

## PM-1 — Esquiva perfeita e funções das armas

- **Esquiva perfeita:** esquivar (tecla Z) nos primeiros 3 ticks antes do golpe anula o golpe e mostra
  "ESQUIVA PERFEITA!". Dá uma janela de crítico, devolve um pouco de stamina e atordoa Yoju no golpe de perto. Vale
  uma vez por esquiva.
- **Espada:** especial (tecla R) virou **corte em avanço**: o jogador avança e corta tudo numa faixa à frente, sem
  atravessar parede.
- **Faca:** especial virou **sequência de 4 cortes** rápidos.
- **Armas de fogo:** clique direito = **tiro preciso** (rifle ×2,2, pistola ×1,7 de dano; conta como golpe pesado).
- Visto em jogo: corte em avanço, 4 cortes, tiro preciso, "ESQUIVA PERFEITA!" contra um Primigenius.

## PM-2 — Ferimentos por parte dos kaiju

- O dano em cada parte acumula; passando de um limite, a parte fica **ferida** por um tempo e sara sozinha.
  - Pernas: kaiju mais lento, sem investida nem salto.
  - Cabeça: atordoa e fica mais fácil de atordoar; sem mordida/tiro da boca.
  - Cauda: sem rabada. Abdômen da aranha: sem teia. Da formiga: sem ácido. Chapéu do cogumelo: sem esporos.
- No máximo 2 partes feridas ao mesmo tempo; a parte ferida leva mais dano; fica salvo no mundo.
- Aparece "FERIDO: Pernas" na barra de vida do kaiju, com lascas desenhadas.
- Visto em jogo: perna do Primigenius ferida por golpes de espada, aviso nos dois idiomas. Lentidão e habilidades
  bloqueadas só pelos testes automáticos.

## PM-3 — Kaiju inteligentes leem os hábitos

- Só o No. 9 (e formas) e o No. 10 percebem se o alvo **fica longe**, **fica colado**, **esquiva** ou **repete o
  mesmo ataque**, e respondem com o que já têm (aproximar, recuar, golpe de área, tiro).
- A leitura dura 5 s e espera 8 s antes de mudar de novo (não fica oscilando); troca de alvo esquece tudo.
- Visto em jogo: No. 10 lendo "repete", "fica longe" e "esquiva" contra a Mina e o Hoshina.

## PM-4 — Terreno por espécie e papéis em grupo

- **Terreno:** aranhas contornam em vez de quebrar (só quebram se travarem); Honju com força própria; custo da água
  no caminho por espécie.
- **Grupo:** agressor, apoio a distância (fica longe e atira) e flanco (vai por trás); **no máximo 3 kaiju colados**
  no mesmo alvo (os outros esperam a vez circulando); o **líder** passa o alvo aos próximos; quando o líder morre o
  grupo recua ou se enfurece (por espécie).
- Visto em jogo: Honju + 5 Primigenius + formiga contra o Hoshina, "espera a vez" no log, grupo recuando quando o
  Honju morreu.

## PM-5 — Fases dos chefes

- Cada fase do chefe muda o jogo: velocidade, ritmo dos ataques, distância que ele segura com o tiro pronto e
  **núcleo exposto** por um instante na troca, com aviso próprio.
- A fase fica salva; ao carregar o mundo não repete a troca.
- Corrigido: barra do chefe que ficava na tela depois da morte.
- Visto em jogo: Honju chefe trocando de fase contra a Kikoru e soldados, uma barra só.

## PM-6 — Ordens do esquadrão

- **Focar:** todo o esquadrão vai no kaiju que o jogador está mirando, até ele cair; depois voltam a seguir sozinhos.
- **Guarda:** o esquadrão protege uma área de raio 16 em volta de onde o jogador está, atacando primeiro o kaiju
  mais para dentro.
- **Funções:** Atirador (arma de fogo) ou Assalto (o resto), mostradas na aba Esquadrão.
- Visto em jogo: Focar num kaiju a 24 blocos derrubado em 9 s; anel da guarda medido.

## PM-7 — Provas de promoção e avaliação pós-missão

- Toda missão mede tiros e acertos, dano causado e recebido, aliados perdidos e tempo. Ao terminar dá **uma nota
  S–D** com resumo e dica no chat; S e A dão mérito extra. Precisão da carreira no Perfil, melhor nota no card.
- **Provas de patente:** Tiro (Oficial Sênior), Esquadrão (Líder de Pelotão), Comando (Vice-Capitão), Capitão. Têm
  requisitos (precisão, dano recebido, perdas, esquadrão mínimo, nota mínima). Reprovada: sem recompensa e espera
  para tentar de novo. Aprovada: não se repete.
- Visto em jogo: reprovação por precisão (0/20), aprovação com nota S e promoção.

## PM-8 — Oficiais e estação de preparação

- **Oficial de Missões** e **Oficial de Provas** (NPCs) no QG das bases, em Ariake e Tachikawa: falar com eles dá o
  briefing da próxima patente e abre o quadro de missões dele.
- **Estação de Preparação do Esquadrão:** cura o esquadrão por perto, tira efeitos ruins e completa a munição
  reserva, pagando com a munição do inventário do jogador; espera 1 min entre usos.
- Visto em jogo: os dois oficiais e o quadro de cada um; estação curando 2 soldados e repartindo as balas.

## PM-8B — Provas dentro da base (pedido do Miguel)

"Missões são missões que ocorrem nas cidades ou invasões em bases; prova acontece dentro da base e serve como curso
ou para fortificar o corpo do jogador."

- **Missões** continuam operações de campo (em volta do jogador ou invasões).
- **Provas** só se aceitam com o **Oficial de Provas** perto (no menu M aparecem com o "Aceitar" apagado).
- A prova acontece **onde foi aceita**: os kaiju surgem a 6–14 blocos, a base não pode ser quebrada durante a prova
  e sair a mais de 64 blocos reprova ("você saiu da base").
- Kaiju de prova: só contam para a prova de quem a aceitou, sem mérito de abate e sem carcaça; somem quando a prova
  acaba.
- Toda prova dá **XP do corpo** (força, velocidade, resistência, agilidade). Três **cursos** repetíveis só de
  treino: Agilidade, Força e Resistência.
- Visto em jogo: quadro do Oficial de Provas com os cursos; Curso de Força concluído com "+160 XP em Força";
  reprovação ao sair da base.

## PM-8C — Pesquisa no laboratório

- **Terminal de Pesquisa** no QG da base pequena, no laboratório da base grande, no laboratório de armas numeradas
  de Ariake e na recepção de Tachikawa (também na bancada, patente Oficial).
- Abre a seção **Pesquisa** no Arsenal. O jogador entrega material de kaiju (do desmonte) e a pesquisa leva um
  tempo; termina sozinha, mesmo longe da base. Uma por vez.
- 6 projetos:

| Pesquisa | Patente | Tempo | Efeito |
|---|---|---|---|
| Análise de Tecido Yoju | — | 1 min | +10% de dano contra Yoju |
| Estudo dos Artrópodes | — | 2 min | +15% contra aranhas e formigas |
| Mecânica Muscular | Oficial | 3 min | Libera o Mk1 Reforçado antes da patente |
| Ressonância do Núcleo | Oficial | 4 min | Libera o Catalisador de Release antes da patente |
| Anatomia Honju | Oficial Sênior | 6 min | +12% contra Honju |
| Teoria das Armas Numeradas | Líder de Pelotão | 10 min | Libera a Espada do Hoshina antes da patente, +5% contra numerados |

- A bancada mostra "ou a pesquisa: X" nos itens travados. É o caminho natural das armas numeradas do jogador.
- Comando para testes: `/kn8 research start|finish|reset|list`.
- Visto em jogo: pesquisa iniciada pelo botão, concluída sozinha depois de 1 min; Mk1 Reforçado liberado pela
  pesquisa. O dano maior só pelos testes automáticos.

## PM-8D — Base modular do jogador

- **Núcleo da Base** (bancada, patente Oficial): colocar funda a sua base, uma por jogador, salva no mundo e
  protegida contra a destruição dos kaiju.
- Nova aba **BASE** no menu: 8 lotes em volta do núcleo, cada um recebe um módulo pagando material:

| Módulo | Patente | Tem |
|---|---|---|
| Oficina | — | 2 bancadas, ferraria, bigorna, baús |
| Laboratório | Oficial | 2 terminais de pesquisa |
| Alojamento | Oficial Sênior | Estação de preparação, 4 camas |
| Comando (precisa da Oficina) | Oficial Sênior | Oficial de Missões e Oficial de Provas (provas na sua base) |
| Pátio de Treino | — | 3 bonecos de treino, alvos |

- Demolir: dois cliques (DEMOLIR e CONFIRMAR); não devolve o material, baús soltam o conteúdo.
- Quebrar o núcleo acaba com a base (as construções ficam no mundo).
- Visto em jogo: base fundada, 4 módulos construídos pelo menu, Comando com os oficiais abrindo o quadro de provas.
  Demolir, segundo núcleo e outro dono só pelos testes automáticos.

---

## Teclas e comandos novos

| O quê | Como |
|---|---|
| Esquiva perfeita | Z no momento certo |
| Especial da espada/faca | R |
| Tiro preciso | Clique direito com arma de fogo |
| Ordens Focar e Guarda | Menu M → Esquadrão |
| Quadro de missões/provas | Botão direito nos oficiais |
| Pesquisa | Botão direito no Terminal de Pesquisa (ou Menu M → Arsenal → Pesquisa) |
| Base | Botão direito no Núcleo da Base (ou Menu M → Base) |
| Testes | `/kn8 research ...`, `/kn8 mission accept ...` |

## O que espera decisão do Miguel

- **Números [SUPOSIÇÃO]** de todas as etapas (tabelas em `docs/BALANCEAMENTO.md`).
- **PM-9** (próxima): alerta por região nos locais da obra e rastro de destruição como pistas.
- **PM-10:** evacuação com civis (entidade civil nova, modelo provisório) e apoio militar (3 tipos, só em missão e
  invasão). Missões nas cidades da obra entram aqui.
- **PM-11:** armas numeradas do jogador (todas, decisão de 2026-10-23); a pesquisa já serve de caminho.
- Modelo próprio dos oficiais (hoje usam o modelo do soldado).

Protocolo de rede atual: **27** (cliente e servidor precisam da mesma versão do mod).
