# MEGA_ATUALIZACAO_0_2.md — Plano da atualização 0.2 (aprovado pelo Miguel em 2026-10-06)

Feita em etapas, uma por vez: cada etapa compila, passa JUnit + GameTests e é vista em jogo na nuvem antes da
próxima; o Miguel testa e aprova (regra da Fase 5). Itens marcados [SUPOSIÇÃO] podem ser trocados no teste.

## Decisões do Miguel (2026-10-06)

| Assunto | Decisão |
|---|---|
| Pacote | Patentes/crafting, missões, chefe Honju, alerta de invasão, corrida com stamina, sons, **machado** |
| Release | Limite máximo **100%** para todos, alcançado por **treino** (não por um teto fixo da patente). Vale também para os soldados: o nível de força deles é uma faixa de Release; soldados especiais (futuro) chegam a 90% |
| Teto duplicado | Some o `rankCaps` do config; o teto vem do treino (ver Etapa 2) |
| Honju | Invoca só Yoju da própria espécie |
| Kaiju futuros | Numerados (os mais fortes, várias formas: meio humanoide até gigante) e Daikaiju — depois da 0.2 |
| Sons | Gerados por código (sintéticos, sem direitos); trocáveis por arquivos melhores sem mexer no código |

## Etapas

| # | Etapa | Conteúdo |
|---|---|---|
| 1 | Sons e corrida | `tools/art/gen_sounds.py` gera os `.ogg` (rugido, mordida, slam, investida, dano/morte de kaiju, passos pesados, tiro de rifle/pistola, corte, Release, sobrecarga, dash, sirene, desmonte); `sounds.json` + `SoundEvent`s; trocar os sons vanilla de placeholder. Corrida gasta stamina (GDD §7) |
| 2 | Release por treino + patentes | Teto pessoal = 100%; cada ponto custa mais XP (fórmula do M5). Fontes de XP de treino: **boneco de treino** (bloco), dano em kaiju, desmonte, missões. Patente salva por jogador (`merit`), promoção ao juntar mérito; a patente dá vida, tamanho do esquadrão e desbloqueios (não mais o teto). HUD mostra patente e mérito. `/kn8 rank` |
| 3 | Crafting | Bancada da Força de Defesa; receitas com tecido, fibra, fragmento e núcleo: traje Mk1, munição/armas, melhorias do traje. Receitas presas à patente (`unlocks`) |
| 4 | Machado ✅ | `axe.glb` → item (estilo `heavy`), na mão do jogador e do soldado, animações de golpe, quebra guarda / expõe núcleo [SUPOSIÇÃO], variante de soldado `axe` |
| 5 | Missões | Exame de admissão, patrulha, primeiro desmonte (JSON já existem): aceitar, rastrear, recompensa em mérito + XP de treino + itens; tela simples de missões |
| 6 | Chefe Honju | `primigenius_honju` como chefe: barra de chefe, fases por vida, invoca `primigenius` (limites no config), recompensa grande; mesma regra para `primigenius_revived` → `primigenius_resurrected` [SUPOSIÇÃO] |
| 7 | Alerta de invasão | Evento: sirene, barra no topo, ondas de kaiju chegando numa vila/área, soldados de defesa, recompensa no fim; destruição ligada; `/kn8 invasion start|stop` e chance natural configurável |
| H | **HUD nova** ✅ | Fiel à referência do Miguel (arte em textura, `tools/art/gen_hud.py`) |
| M | **Menu da Força de Defesa** ✅ (layout e dados atuais; abas se completam nas etapas) | Tela com abas no estilo da HUD: Status (patente, mérito, Release e treino), Missões, Alertas (invasões e kaiju avistados), Bestiário; cada aba é preenchida pela etapa correspondente |

Depois da 0.2: M12 transformação, NPCs e base (M13/M17), kaiju numerados e Daikaiju, soldados especiais (até 90%).

## Perguntas em aberto (com o padrão usado se não houver resposta)

1. Patentes continuam limitando algo do Release? Padrão: **não** — só treino; a patente libera equipamento e esquadrão.
2. Morrer perde XP de treino? Padrão: **não** (já é copiado na morte).
3. Boneco de treino: XP por golpe com limite por minuto. Padrão: 5 XP por golpe, máx. 60 XP/min.
