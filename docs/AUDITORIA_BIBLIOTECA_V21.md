# Auditoria do projeto para a Biblioteca v21 (Prioridade 0)

Documento do Miguel: `docs/BIBLIOTECA_KAIJU_N8_v21.md` (2026-10-08). Ele manda seguir a ordem das prioridades,
uma por vez, auditando antes e reaproveitando o que ja existe. Esta e a **Prioridade 0**: o que o mod tem hoje, o que
falta em cada prioridade, os conflitos com decisoes ja tomadas e o plano de etapas. Nenhum codigo foi alterado.

Estado de partida: 0.6-A a 0.6-F prontas (GameTests 76/76), 17 entidades registradas, 216 classes Java.

---

## Prioridade 1 — Fundacao do jogador

**Existe**

- Release em `PowerData` (attachment `kn8:power`, salvo e copiado na morte; `PowerService`, `PowerMath`):
  % efetiva = treinado (0-100, sobe por XP) + surto (so por comando) + desespero com vida baixa (ate +15).
- Treino: XP por dano em kaiju, abates, desmonte, boneco de treino (limite por minuto), catalisador, missoes.
- Stamina (corrida, golpes, esquiva, dash), calor por surto com estagios WARM / OVERLOAD / CRITICAL / PANIC.
- Release da: dano x(1 + R/25), velocidade +0,4%/ponto, reducao de dano ate 40%, empurrao, parry melhor com R >= 60.
- Vida maxima pela patente. Sync privado `PowerView` (so na mudanca) e publico `release_visual` + `aura`.

**Falta (pedido da v21)**

- **Release so com traje da Forca de Defesa**: hoje nao exige traje (`PowerService` trata como vestido).
- **Limite pessoal baixo no comeco (10-20%)**: hoje o treinado comeca em 0 e sobe ate 100 sem teto pessoal separado.
- **O jogador escolher a %**: nao ha tecla nem payload; o Release fica sempre ligado no valor treinado.
- **~1 minuto de uso continuo na faixa segura**: nao ha cronometro.
- **Sobrecarga acima do limite pessoal, com dano, sem baixar a %**: hoje o PANIC **forca o Release para 1%**, o que
  contraria a regra da v21.
- **Atributos separados** (forca, velocidade, resistencia, agilidade): hoje tudo sai da % de Release.
- Codigo sem uso: `energy`, `control`, `aptitude`, bloco `transform` do config, `release_cap_bonus` do traje.

## Prioridade 2 — Modelos, armaduras, ossos e animacoes

**Existe**

- 13 entidades com malha do Meshy presa a ossos GeckoLib (rig por script em `tools/art`), animacoes geradas por
  script. Pecas rigidas por osso (sem deformacao suave).
- Traje 3D (`SuitLayer`): malha presa as ModelParts do jogador (tronco, bracos, pernas), segue as animacoes da PAL,
  braco em 1a pessoa. Itens de traje sao so **peitoral**.

**Falta**

- Armadura de **corpo inteiro**: hoje nao cobre **cabeca**; maos e pes so no que a malha do braco/perna tiver.
  O Mk1 do Miguel (antigo e o refeito) nao tem cabeca: a cabeca so entra se o asset tiver.
- Trocar os modelos refeitos (soldado, Mk1, Hoshina, No. 10 pequeno e gigante: `docs/MODELOS_RECEBIDOS_2026_10_08.md`).
- Perfil de locomocao por personagem/criatura; 1a pessoa nas animacoes de arma (hoje so o braco do traje).

## Prioridade 3 — Armas e empunhadura

**Existe**: `weapon/*.json` (dano, alcance, estilo blade/heavy/firearm/cannon, golpes leve/pesado, combo, sons,
especial). Pose de jogador so para rifle/pistola (`CROSSBOW_HOLD`); animacoes do jogador por arma na PAL
(`player.<arma>.light/heavy/special/shoot`); soldado com pose por arma.

**Falta**: `WeaponAnimationProfile` por familia/personagem (saque, guarda, ataque, recuperacao, recarga por etapas,
recuo do corpo, bainha), 1a pessoa, perfis proprios de Hoshina, Mina, Kikoru e Narumi (referencias do Miguel ja em
`docs/MODELOS_RECEBIDOS_2026_10_08.md`).

## Prioridade 4 — Combate e mobilidade

**Existe**: golpe leve/pesado/carregado, combo, bloqueio, parry com janela, esquiva (Z, invulneravel), dash (Alt),
especial (R), hitbox por raycast no tick do JSON, empurrao, atordoamento.

**Falta**: **parkour inteiro** (salto longo, vault, escalada curta, wall-run/slide/jump, ledge grab, rolamento,
slide); troca de alvo; reacao corporal ao impacto no jogador.

## Prioridade 5 — IA de personagens e kaiju

**Existe**: kaiju escolhem habilidade por alcance, setor e prioridade; furia; chefes com fases; No. 9/No. 10 comandam;
Preondactyl voa; soldados com distancia por arma e arma de apoio; Hoshina com tecnicas, esquiva, contra-ataque.

**Falta**: `CreatureMovementProfile` (escalar, saltar, voar, fuga, interacao com o ambiente) — hoje so altura de
degrau proporcional; kaiju grandes andam reto ate o alvo. Soldados escolhem sempre o kaiju mais perto.

## Prioridade 6 — Personagens e kaiju da biblioteca

**Existe**: soldado comum, Hoshina, Hoshina + No. 10; kaiju Trichonephila (Yoju/Honju), Primigenius (4 versoes),
No. 9, No. 10 (2 formas), Preondactyl.

**Modelos ja recebidos e guardados**: Kafka, Kaiju No. 8, larva, Mina + canhao, Reno, Reno + No. 6, Kikoru,
Kikoru + No. 4 (+ asas), Gen Narumi + baioneta, Hoshina refeito, Phaneroplasmodium (cogumelo Yoju), Myxogasterocarp
(cogumelo Honju), Philinosoma (lagarto).

**Sem modelo**: Isao, Hikari, Iharu, Haruichi, Aoi, Eiji, Rin, Kota, Jugo, Toko, Soichiro, Jura, Akari, Hakua, Ryo,
Tae, Konomi, Itami, Nogizaka, Monster Sweeper, Sagan, Sebasu, Bakko; kaiju numerados 1-7 e 11-15 e as outras especies.

## Prioridade 7 — Destruicao e ataques de area

**Existe**: `DestructionService` (esfera, faixa da frente, caminho ao andar; 32 blocos/tick; areas protegidas;
`mobGriefing`), quebra ao andar para todo kaiju, ataques `area_melee`/`sweep`/`projectile`/`leap`/`multi_hit`.

**Falta**: `AreaAttackController` unico com os 9 formatos (cone, arco, linha, feixe, impacto no chao, cauda, corpo);
empurrar fisicamente entidades menores; recalcular o caminho logo depois de quebrar (hoje refaz a cada 10 ticks).

## Prioridade 8 — Treinamento

**Existe**: boneco de treino (XP limitado por minuto). **Falta**: zona de treino (cidade evacuada/area de teste) com
kaiju controlados e exercicios por atributo.

## Prioridade 9 — Prova de admissao

**Existe**: missao `exam` (ir ao portao, matar 3 Primigenius, vencer o chefe `exam_honju`; promove a Oficial).
**Falta**: avaliacao por varios criterios (velocidade, sobrevivencia, eficiencia, decisao), como pede a v21.

## Prioridade 10 — Missoes e invasoes dinamicas

**Existe**: 12 missoes numa lista do menu (aceitar, maximo 3 ativas, cooldown); 8 invasoes niveis 1-6 com ondas,
defensores, recompensa por contribuicao; invasao natural 1x por dia (20%) em volta de um jogador.

**Falta**: missoes como **eventos do mundo**; invasoes em **cidades e bases** (hoje so em volta do jogador ou da
area da missao); bases pequena/media/grande/fortificada (hoje so o posto avancado); defesa de setores/pontos;
cooldown anti-repeticao; consequencias e cadeia de eventos.

## Prioridade 11 — Esquadrao, Capitao e Vice-Capitao

**Existe**: patentes ate Capitao; aba Esquadrao so mostra os soldados por perto (botoes de ordem desativados).
**Falta**: entrar em esquadrao, funcoes, criar pelotao, recrutar, distribuir por setores, ordens, reforcos.

## Prioridade 12 — Chefes e eventos especiais

**Existe**: chefes com fases, invulnerabilidade entre fases e invocacao; No. 10 vira gigante (tempo ou vida).
**Falta**: evento **No. 10 -> No. 9**: o corpo do No. 10 fica no campo, o No. 9 aparece, absorve e vira a forma
vermelha com IA, ataques, regeneracao, VFX e audio novos.

## Prioridade 13 — Aura

**Existe**: `aura/*.json` (cor, secundaria, estilo, Release minimo, tamanho); jogador, Hoshina e Hoshina + No. 10.
**Falta**: intensidade, densidade, velocidade, pulsacao, efeito no chao, trilha, reacao a ataque/sobrecarga/
transformacao; aura por kaiju; aura da Kikoru (amarela com raios).

## Prioridade 14 — Audio

**Existe**: 28 sons gerados por `tools/audio/gen_sounds.py`; sirene de 3 s a cada 5 s no aviso da invasao.
**Falta**: limite de volume por categoria, limite de sons simultaneos, variacoes, ducking; volume do kaiju cresce
com a altura **sem teto**; sirene nova em camadas por gravidade.

## Prioridade 15 — Polimento e desempenho

**Existe**: limite de blocos por tick, particulas por nivel, rate limit de pacotes, sync so na mudanca.
**Falta**: limite de kaiju para invasao/missao/chefe (o limite so vale no spawn natural, que esta desligado),
limite de soldados e projeteis; configs declaradas e sem uso (limpeza).

---

## Conflitos com decisoes ja tomadas ([DECIDIR])

1. **Release (Prioridade 1).**
   - Antes, por decisao do Miguel em 2026-10-06, o Release ia ate 100% so por treino, sem traje e sempre ligado.
   - A v21 pede traje obrigatorio, limite pessoal de 10-20% no comeco, cerca de 1 minuto na faixa segura e
     sobrecarga sem baixar a %.
   - Proposta [SUPOSICAO]:
     - uma tecla para subir/baixar a % alvo;
     - o limite pessoal e o valor treinado, comecando em 15%;
     - acima do limite, dano por segundo proporcional ao excesso, sem baixar a %;
     - dentro do limite, um cronometro de ~60 s de uso continuo; depois disso, cansaco (stamina e calor);
     - o PANIC deixa de forcar 1%.
   - Falta definir o que acontece no fim do minuto seguro.
2. **Cauda do Hoshina + No. 10.** A v21 diz que a cauda pode segurar uma terceira espada quando a tecnica pedir. O
   Miguel mandou tirar a espada da cauda em 2026-10-08. Manter sem espada, ou mostrar a espada so em uma tecnica
   (por exemplo, o Juni-hitoe)?
3. **Modelos refeitos.** A v21 diz para nao substituir assets sem necessidade. O Miguel mandou trocar soldado, Mk1,
   Hoshina e No. 10 no chat. Vale o chat (pedido mais recente e explicito).
4. **No. 10 -> No. 9 (Prioridade 12).** A forma gigante continua? Proposta: pequeno -> gigante (como esta) -> quando
   o gigante cai, o No. 9 absorve. Falta o modelo do No. 9 vermelho.
5. **Missoes (Prioridade 10).** Lista no menu vs. eventos do mundo. Proposta: o exame e as missoes de patente ficam
   no menu; as outras viram eventos (alerta no mundo, que o jogador aceita ao chegar).
6. **Kafka / Kaiju No. 8.** E um personagem NPC aliado (como o Hoshina), ou o jogador tambem se transforma
   (M12 da Fase 4)? A v21 lista o Kafka como "jogavel/combativo".
7. **Pesquisa de referencias.** A v21 pede pesquisa na obra antes de cada animacao. Vou usar as referencias que o
   Miguel mandou; o que faltar fica marcado como MOD.
8. **Personagens sem modelo** (lista na Prioridade 6): pular ate chegar o modelo, ou fazer com o modelo do soldado
   comum trocando a textura?

---

## Decisoes do Miguel sobre os conflitos (2026-10-08)

1. **Release:**
   - **Tecla** para subir o Release. A aura aparece junto enquanto sobe e fica enquanto ele estiver ativo.
   - **So funciona com o traje**, como no anime.
   - **Limite pessoal aleatorio** por jogador:
     - comum: 5-10%;
     - raro: 15-30% inicial.
   - **Acima do limite** a % nao cai. O dano so vem se exceder o limite, junto com o calor: quanto mais usa, mais o
     traje sobrecarrega e mais o corpo desgasta.
2. **Hoshina com a No. 10 em fases:**
   - fase 1: forma normal (duas espadas);
   - fase 2: quando comeca a perder, empunha uma **katana de duas maos**;
   - fase 3: duas espadas + a **terceira espada na cauda**.
3. **Invasao especial No. 10 + No. 9:** a invasao do No. 10 normal continua. Ha tambem uma versao especial em que o
   No. 9 assume o corpo do No. 10 e revive os kaiju em volta. O modelo dos dois fundidos chegou:
   `kaiju_no9_fusion`, 16.624 triangulos, cauda curvada para o lado.
4. **Missoes e admissao:**
   - O **exame de admissao** aprova o jogador na Forca de Defesa. Nele o jogador recebe traje, arma etc., elimina
     kaiju e e admitido.
   - O resto das missoes acontece **nas cidades ou postos de defesa**, com objetivos: salvar aliados, proteger
     civis ou a cidade e eliminar kaiju.
5. **Kafka:**
   - E um **aliado** com duas formas (normal e Kaiju No. 8), cada uma com spawn proprio.
   - O **jogador tambem pode virar o Kaiju No. 8**, mas so depois de achar o **inseto (larva)**:
     - ele e raro de encontrar;
     - infecta o corpo do jogador;
     - o jogador recebe um aviso de que foi infectado.
6. **Personagens sem modelo** ficam para o futuro. Agora, so o que ja tem modelo.

Itens 3 (modelos refeitos: vale o chat) e 7 (referencias: usar as do Miguel, o resto marcado como MOD) seguem a
proposta acima.

## Plano de etapas (ordem da v21)

Versao (Miguel, 2026-10-08): esta atualizacao e a **0.5.0** do mod; a **1.0.0** vem depois que tudo da 0.5.0 estiver
corrigido. Os numeros 0.2-0.6 usados ate aqui eram etapas internas de desenvolvimento; as etapas desta versao se
chamam **0.5.0-A, 0.5.0-B...** para nao confundir com elas.

Uma etapa por vez. Cada uma tem compilacao, GameTests e teste em jogo, e espera a aprovacao do Miguel.

| Etapa | Prioridade | Conteudo |
|---|---|---|
| **0.5.0-A** ✅ feita (2026-10-08) | 1 | Release novo do jogador (traje obrigatorio, limite pessoal, tecla de %, cronometro, sobrecarga sem baixar a %), atributos (forca, velocidade, resistencia, agilidade) e limpeza de `energy`/`control`/`aptitude` |
| **0.5.0-B** | 2 | Traje de corpo inteiro com o Mk1 refeito (e o Reforcado); troca do soldado e do Hoshina pelos modelos refeitos |
| **0.5.0-C** | 2 | No. 10 pequeno e gigante refeitos (rig novo) e perfis de locomocao |
| **0.5.0-D** | 3 | `WeaponAnimationProfile` (saque, guarda, ataque, recuperacao, recarga, recuo), 1a pessoa, NPCs com o mesmo perfil |
| **0.5.0-E** | 4 | Parkour do jogador (salto longo, vault, escalada curta, wall-jump/slide, ledge grab, rolamento, slide), troca de alvo, reacao ao impacto |
| **0.5.0-F** | 5 | `CreatureMovementProfile` para kaiju e soldados (escalar, saltar, fugir, prioridade de alvo) |
| **0.5.0-G...** | 6 | Personagens na ordem da v21, uma etapa cada: Kafka/No. 8 (+ larva), Mina, Hoshina (postura nova), Hoshina + No. 10, Reno, Reno + No. 6, Kikoru (machado `axe` e aura amarela), Kikoru + No. 4 (voo), Narumi, Narumi + No. 1; depois os kaiju novos (Phaneroplasmodium, Myxogasterocarp, Philinosoma) |
| depois | 7-15 | Destruicao e area, zona de treino, prova de admissao, missoes e invasoes em cidades/bases, esquadrao, evento No. 10 -> No. 9, aura, audio, polimento |

A proxima etapa e a **0.5.0-A** (Prioridade 1). Ela depende do conflito 1 acima.
