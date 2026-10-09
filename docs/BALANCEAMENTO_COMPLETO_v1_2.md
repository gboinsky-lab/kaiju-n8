# BALANCEAMENTO COMPLETO — KAIJU NO. 8 / MINECRAFT
## Versão 1.2 — matriz de probabilidades de vitória por personagem e Kaiju

**Objetivo:** consolidar os dois arquivos de balanceamento enviados, preservar os valores atuais que já foram testados/aprovados, identificar problemas de balanceamento e propor um novo balanceamento para **todos os personagens, soldados, Kaijus, formas, Numbers Weapons, bosses e sistemas principais** do mod.

### Fontes-base
- `BALANCEAMENTO_ATUAL_KAIJUS_PERSONAGENS.md`
- `BALANCEAMENTO.md`
- Biblioteca mestre do mod `Kaiju_N8_Biblioteca_Completa_Final_v22.md`

### Regra importante
Os números marcados como **PROPOSTA** não são números canônicos da obra. São valores de gameplay para Minecraft, derivados dos valores atuais, dos testes registrados e da hierarquia de poder definida para o mod.

---

# 0. PRINCÍPIOS DO NOVO BALANCEAMENTO

1. **Não balancear apenas por HP.**
2. Força deve aparecer também em velocidade, alcance, regeneração, mobilidade, resistência, destruição, IA e qualidade das habilidades.
3. Kaijus maiores devem ter impacto físico proporcional ao tamanho.
4. Personagens especializados devem vencer pelo estilo correto, não por possuir simplesmente mais dano.
5. Bosses devem ter fases e mudanças de comportamento.
6. Numbers Weapons devem mudar o estilo de combate, e não apenas multiplicar atributos.
7. O jogador deve conseguir chegar a níveis muito altos, mas ainda existir uma hierarquia clara entre soldado → elite → capitão → Kaiju numerado → Daikaiju/boss.
8. Formas alternativas do mesmo personagem devem possuir funções próprias.
9. As três formas novas do No. 9 — fusão com No. 10, fusão com Formiga e forma preta inicial — são **ADAPTAÇÕES ORIGINAIS DO MOD**.
10. O No. 9 que absorve Isao/Numbers Weapon 2 permanece separado das outras formas.
11. Qualquer valor que já foi medido e considerado bom deve ser preservado salvo quando entrar em conflito com uma nova forma/sistema.
12. Se um teste futuro mostrar que um valor está quebrado, corrigir a causa e registrar o novo resultado.

---

# 1. PROBLEMAS IDENTIFICADOS NOS VALORES ATUAIS

## 1.1 No. 9 está subespecificado

O arquivo atual possui o No. 9 em uma única linha de balanceamento:

- Fortitude 8,0
- 1.280 HP
- 33,6 dano base
- armadura 16
- velocidade 0,32
- garra ×1,3
- Finger Gun ×1,0
- regeneração escalonada
- fuga a 15%
- reviver até 3
- comando em 32 blocos.

Isso é suficiente para a forma atual, mas não é suficiente para as novas formas planejadas.

**Alteração obrigatória:** cada forma deve possuir um perfil independente.

---

## 1.2 No. 10 gigante é o atual teto físico, mas não deve ser o teto absoluto do jogo

O No. 10 gigante possui atualmente:
- 4.500 HP;
- Fortitude 9;
- 53,7 dano base;
- armadura 18;
- tamanho aproximado de 24 m;
- grande destruição.

Esse valor está bom como referência de **Daikaiju/Boss físico**, mas as formas superiores do No. 9 precisam superá-lo através de fases, adaptação e habilidades — não somente com números gigantes.

---

## 1.3 Hoshina

Os testes atuais são valiosos e devem ser preservados como referência:

- Hoshina normal vence Primigenius Honju rapidamente;
- vence No. 9 atual em aproximadamente 31–34 s;
- vence No. 10 pequeno em 40–64 s, com bastante variação;
- perde para No. 10 gigante;
- Hoshina + Numbers Weapon 10 vence No. 10 gigante em aproximadamente 1 minuto;
- Hoshina + No. 10 vence No. 9 atual em aproximadamente 25 s.

**Conclusão:** não aumentar Hoshina indiscriminadamente. As novas formas do No. 9 devem ser criadas acima do No. 9 atual para preservar essa progressão.

---

## 1.4 Dano do jogador

A regra atual de limitar golpe comum a aproximadamente 15% da vida máxima do Kaiju é útil para impedir one-shot, mas deve ser aplicada com cuidado.

**Proposta:**
- golpe comum: máximo de 12% da vida máxima;
- golpe pesado: máximo de 15%;
- especial/ultimate: pode ultrapassar o limite;
- ataques que expõem o core podem permitir dano crítico;
- boss phases podem alterar temporariamente a proteção do core.

Isso evita que uma arma forte trivialize um boss.

---

# 2. ESCALA GERAL DE AMEAÇA

| Tier | Categoria | Função |
|---|---|---|
| T0 | Humano comum | NPC não combatente |
| T1 | Recruta | soldado básico |
| T2 | Soldado normal | combate em grupo |
| T3 | Soldado alto/elite | ameaça a Yojus |
| T4 | Especialista | Hoshina/Kikoru/Reno etc. |
| T5 | Capitão/Numbers inicial | ameaça a Honjus e Kaijus fortes |
| T6 | Daikaiju | grande ameaça de campo |
| T7 | Kaiju Numerado | boss/ameaça excepcional |
| T8 | Forma Numerada avançada | boss superior |
| T9 | Forma final/adaptativa | ameaça de evento final |

**Importante:** tier não significa somente HP.

---

# 3. KAIJUS — BALANCEAMENTO PROPOSTO

| Entidade | Atual | Novo HP | Novo dano base | Armadura | Velocidade | Tier |
|---|---:|---:|---:|---:|---:|---|
| Trichonephila Yoju | 57 | **65** | **4,2** | 7 | 0,31 | T2 |
| Primigenius Yoju | 211 | **220** | **10,0** | 11 | 0,23 | T3 |
| Primigenius ressurgido | 299 | **310** | **12,8** | 12 | 0,24 | T3+ |
| Primigenius Honju | 320 | **360** | **13,8** | 12 | 0,25 | T4 |
| Primigenius revivido Honju | 422 | **460** | **16,5** | 13 | 0,25 | T4+ |
| Trichonephila Honju | 368/551 boss | **400/600 boss** | **15,0** | 13 | 0,29 | T4 |
| Preondactyl | 394 | **430** | **15,5** | 13 | 0,31 | T4 |
| No. 10 pequeno | 1.576 | **1.650** | **39,5** | 17 | 0,35 | T7 |
| No. 10 gigante | 4.500 | **4.600** | **54,5** | 18 | 0,27 | T7+ |
| No. 9 atual/base | 1.280 | **1.450** | **34,5** | 16 | 0,34 | T7 |

### Observação
Os aumentos dos Kaijus comuns são pequenos porque o objetivo não é inflar o jogo. A dificuldade adicional deve vir de IA, habilidades, terreno e composição das invasões.

---

# 4. KAIJU NO. 9 — SISTEMA COMPLETO DE FORMAS

## 4.1 No. 9 — Forma Base

**Status:** forma principal atual.

| Atributo | Novo |
|---|---:|
| HP | **1.450** |
| Dano base | **34,5** |
| Armadura | **16** |
| Velocidade | **0,34** |
| Regeneração <50% | **2,5% HP/s** |
| Regeneração <20% | **5% HP/s** |
| Pausa após dano | **10 ticks** |
| Fuga | **15% HP** |
| Raio de comando | **32 blocos** |
| Revividos simultâneos | **3** |

### Habilidades
- Garra;
- Charge;
- Finger Gun;
- regeneração;
- manipulação corporal;
- reviver cadáveres;
- comando de Kaijus;
- adaptação de combate;
- fuga inteligente.

---

# 5. NO. 9 — FORMA PRETA ORIGINAL

**Importante:** esta é a **primeira forma preta**, separada da forma resultante da absorção do Isao/Numbers Weapon 2.

**Tipo:** evolução intermediária.

| Atributo | Novo |
|---|---:|
| HP | **2.200** |
| Dano base | **42** |
| Armadura | **17** |
| Velocidade | **0,36** |
| Regeneração | **3%/s abaixo de 50%; 5,5%/s abaixo de 20%** |
| Raio de comando | **40 blocos** |
| Revividos | **4** |
| Tier | **T8** |

### Novas capacidades
- manipulação corporal mais rápida;
- membros adicionais;
- Finger Gun aprimorado;
- Multi-Finger Gun;
- evasão corporal;
- endurecimento;
- voo curto;
- criação de membros defensivos;
- adaptação mais rápida.

### Regra de gameplay
Não deve parecer apenas “No. 9 com HP maior”. A IA deve:
- observar ataques;
- testar defesa do jogador;
- mudar distância;
- proteger o core;
- usar clones/iscas;
- regenerar quando tiver espaço.

---

# 6. NO. 9 — FUSÃO COM NO. 10

**Status:** FORMA ORIGINAL DO MOD.

Nome interno:
`NO9_FORM_NO10_FUSION`

| Atributo | Novo |
|---|---:|
| HP | **5.400** |
| Dano base | **58** |
| Armadura | **19** |
| Velocidade | **0,38** |
| Regeneração | **3,5%/s; 6%/s abaixo de 20%** |
| Tamanho | **intermediário entre No. 9 e No. 10** |
| Tier | **T8+** |

### Habilidades herdadas do No. 10
- cauda;
- Finger Cannon;
- Multi-Appendage;
- Tail Sweep;
- Tail Stab;
- Heavy Smash;
- comando de Kaijus;
- força corporal do No. 10.

### Habilidades exclusivas da fusão
**Adaptive Tail**
- cauda pode mudar de função;
- atacar;
- defender;
- agarrar;
- perfurar;
- lançar.

**Multi-Finger Cannon**
- vários disparos coordenados.

**Adaptive Appendages**
- cria braços/membros conforme a necessidade.

**No.9 Analysis**
- após receber o mesmo tipo de ataque várias vezes, reduz sua eficiência.

### Regra crítica
A fusão deve ter **duas identidades simultâneas**:
- inteligência/adaptação do No. 9;
- agressividade/combate do No. 10.

Não transformar em simples No. 9 vermelho.

---

# 7. NO. 9 — FUSÃO COM FORMIGA

**Status:** FORMA ORIGINAL DO MOD.

Nome interno:
`NO9_FORM_ANT_FUSION`

| Atributo | Novo |
|---|---:|
| HP | **3.200** |
| Dano base | **46** |
| Armadura | **18** |
| Velocidade | **0,45** |
| Regeneração | **3,2%/s** |
| Tier | **T8** |

### Identidade
Forma especializada em:
- velocidade;
- perseguição;
- mobilidade subterrânea;
- escalada;
- ataques de grupo;
- criação de pressão territorial.

### Habilidades
- Ant Rush;
- Wall Climb;
- Burrow;
- Underground Ambush;
- Mandible Strike;
- Multi-Limb Grab;
- pheromone/command network;
- swarm coordination;
- rapid reposition;
- defensive shell.

### Fraqueza
- menor poder bruto que a fusão No.10;
- maior vulnerabilidade durante escavação;
- ataques de área podem interromper o enxame.

### IA
Prioridade:
`isolar alvo → cercar → atacar de vários ângulos → recuar → reposicionar → atacar novamente`.

---

# 8. NO. 9 — FORMA VERMELHA / EVOLUÍDA

Esta é a forma associada à progressão do evento No.10 → No.9.

| Atributo | Novo |
|---|---:|
| HP | **6.200** |
| Dano base | **63** |
| Armadura | **20** |
| Velocidade | **0,40** |
| Regeneração | **4%/s; 7%/s abaixo de 20%** |
| Tier | **T9** |

### Alterações
- AI adaptativa avançada;
- ataques de área;
- múltiplos membros;
- Finger Cannon avançado;
- defesa do core;
- regeneração de emergência;
- clones;
- criação de Kaijus;
- voo;
- evasão corporal;
- hardening;
- mutação durante a luta.

### Fase de boss
**Fase 1:** análise.  
**Fase 2:** adaptação.  
**Fase 3:** mutação.  
**Fase 4:** proteção do core.  
**Fase 5:** berserk controlado.

Cada fase deve alterar comportamento, não apenas atributos.

---

# 9. NO. 9 — FORMA APÓS ABSORÇÃO DO NO. 2 / ISAO

Esta forma deve ser **separada da forma preta inicial**.

| Atributo | Novo |
|---|---:|
| HP | **7.500** |
| Dano base | **72** |
| Armadura | **20** |
| Velocidade | **0,38** |
| Regeneração | **4,5%/s; 7,5%/s abaixo de 20%** |
| Tier | **T9** |

### Habilidades
- poder adaptativo do No. 9;
- características derivadas do No. 2;
- ondas de energia;
- defesa pesada;
- ataques de área;
- combate corporal;
- mutação;
- regeneração;
- barreiras;
- ataques de alta potência.

### Regra
A absorção do No. 2 deve criar um salto qualitativo. Não é apenas “No. 9 vermelho + 20%”.

---

# 10. NO. 9 — FORMA FINAL ADAPTATIVA

**Uso:** boss final/evento de altíssimo nível.

| Atributo | Novo |
|---|---:|
| HP | **9.000** |
| Dano base | **82** |
| Armadura | **20** |
| Velocidade | **0,42** |
| Regeneração | **dinâmica** |
| Tier | **T9+** |

### Regeneração dinâmica
- dano repetido do mesmo tipo reduz a regeneração;
- dano diferente pode recuperar parte da regeneração;
- destruir órgãos/membros específicos reduz capacidades;
- destruir o core interrompe regeneração avançada.

### Regra de balanceamento
Não permitir que o boss se torne impossível de matar simplesmente por regenerar infinitamente.

---

# 11. NO. 8 — KAFKA

## Forma humana
| HP | Armadura | Velocidade | Tier |
|---:|---:|---:|---|
| 24 | 6 | 0,30 | T2 |

## Kaiju No. 8
| Atributo | Novo |
|---|---:|
| HP | **1.800** |
| Dano base equivalente | **55** |
| Armadura | **19** |
| Velocidade | **0,40** |
| Regeneração | **3%/s abaixo de 50%** |
| Tier | **T8** |

### Ataques
- Heavy Punch;
- Ground Smash;
- Roar;
- Grab/Throw;
- Dash Strike;
- Counter;
- Rage.

### Regra
No. 8 deve ser extremamente forte no corpo a corpo, mas não possuir a versatilidade adaptativa do No. 9.

---

# 12. PERSONAGENS PRINCIPAIS — BALANCEAMENTO

Os valores abaixo são **propostas de gameplay**, não afirmações de HP canônico.

| Personagem | HP | Armadura | Velocidade | Tier | Função |
|---|---:|---:|---:|---|---|
| Mina Ashiro | **420** | 20 | 0,30 | T6 | sniper/anti-Daikaiju |
| Hoshina normal | **460** | 20 | 0,30 | T5+ | espadachim |
| Hoshina + No.10 | **520** | 20 | 0,35 | T7 | combate híbrido |
| Reno normal | **360** | 17 | 0,31 | T5 | suporte/rifle |
| Reno + No.6 | **650** | 20 | 0,36 | T7 | criocinese |
| Kikoru normal | **390** | 18 | 0,34 | T5 | machado |
| Kikoru + No.4 | **580** | 20 | 0,44 | T7 | aéreo |
| Gen Narumi normal | **480** | 20 | 0,32 | T6 | comandante |
| Gen + No.1 | **650** | 20 | 0,36 | T7 | previsão |
| Isao normal | **520** | 20 | 0,29 | T6 | pesado |
| Isao + No.2 | **700** | 20 | 0,32 | T7+ | força/energia |
| Iharu | **300** | 14 | 0,33 | T4 | burst |
| Haruichi | **290** | 14 | 0,31 | T4 | suporte |
| Aoi | **330** | 16 | 0,28 | T4 | pesado |
| Eiji | **400** | 18 | 0,28 | T5 | tanque/líder |
| Rin | **300** | 14 | 0,31 | T4 | metralhadora |
| Kota | **310** | 15 | 0,30 | T4 | shotgun |
| Jugo | **430** | 18 | 0,29 | T5 | capitão |
| Toko | **390** | 18 | 0,30 | T5 | liderança |
| Soichiro | **450** | 19 | 0,31 | T5+ | espada |
| Hikari | **500** | 20 | 0,40 | T6 | aérea |
| Jura | **450** | 19 | 0,30 | T5+ | capitã |
| Akari | **280** | 13 | 0,30 | T3+ | soldado |
| Hakua | **300** | 14 | 0,31 | T4 | elite |
| Ryo | **310** | 15 | 0,31 | T4 | elite |
| Tae | **260** | 12 | 0,29 | T3 | suporte |
| Konomi | **220** | 10 | 0,28 | T2 | operadora |
| Keiji | **240** | 12 | 0,27 | T2 | comando |
| Juzo | **300** | 15 | 0,28 | T3+ | alto comando |
| Akira | **280** | 14 | 0,30 | T3+ | combate |

---

# 13. MINA — BALANCEAMENTO ESPECIAL

Mina não deve competir com Hoshina no corpo a corpo.

### Novo perfil
- HP: 420
- armadura: 20
- velocidade: 0,30
- dano de rifle: moderado
- dano de canhão: alto
- dano Anti-Giant: extremamente alto
- mobilidade: baixa/média
- precisão: máxima.

### Anti-Giant Shot
- preparação longa;
- linha de visão obrigatória;
- cooldown grande;
- dano alto;
- bônus contra Daikaiju;
- menor eficiência contra inimigos pequenos.

Isso mantém Mina forte sem transformá-la em personagem universal.

---

# 14. HOSHINA — AJUSTE FINAL

**Não alterar radicalmente os números já testados.**

Manter:
- HP 460;
- armadura 20;
- velocidade 0,30;
- Release máximo 92%;
- técnicas existentes;
- parry;
- Kaeshi-uchi;
- esquiva.

### Ajuste recomendado
O ganho de velocidade/dano por escalada deve ser progressivo, mas não permitir que Hoshina mantenha permanentemente o pico fora de combate.

### Hoshina + No.10
Manter:
- HP 520;
- Release máximo 100%;
- cauda;
- Full Release;
- Jūni-hitoe.

**Não aumentar muito mais esse personagem**, porque ele já vence o No.10 gigante nos testes atuais.

---

# 15. RENO + NO.6

A forma deve ganhar força principalmente por controle de campo.

### Prioridade
1. congelar;
2. controlar terreno;
3. separar inimigos;
4. atacar;
5. proteger aliados.

### Limite
Não permitir congelamento permanente de bosses.

Bosses devem possuir:
- resistência progressiva;
- break de congelamento;
- cooldown interno;
- redução de duração.

---

# 16. KIKORU + NO.4

O diferencial é mobilidade aérea.

### Não aumentar excessivamente HP.

O poder vem de:
- voo;
- velocidade;
- dash aéreo;
- mergulho;
- esquiva;
- machado;
- ataques verticais.

Proposta:
- HP 580;
- armadura 20;
- velocidade 0,44;
- dano alto em mergulho;
- menor resistência quando aterrissada sem mobilidade.

---

# 17. GEN + NO.1

O diferencial é informação.

Não transformar a previsão em invulnerabilidade.

### Pseudo-Foresight
- prever ataques;
- aumentar chance de esquiva;
- detectar preparação de ataques;
- revelar weak points.

Bosses e ataques imprevisíveis devem reduzir sua eficácia.

---

# 18. SOLDADOS

## Valores atuais preservados como base

| Nível | Release | Dano vs Kaiju |
|---|---:|---:|
| Baixo | 5% | ×0,25 |
| Normal | 10% | ×0,35 |
| Alto | 20% | ×0,80 |
| Elite | 30% | ×1,00 |

Esses números devem permanecer inicialmente porque os testes atuais mostram que soldados normais funcionam melhor em grupo.

### Proposta de ajuste de HP

| Soldado | HP | Armadura | Velocidade |
|---|---:|---:|---:|
| Recruta | 20 | 5 | 0,29 |
| Normal | 24 | 6 | 0,30 |
| Alto | 28 | 8 | 0,31 |
| Elite | 34 | 10 | 0,32 |
| Especial | 40–55 | 12–18 | 0,32–0,36 |

---

# 19. ARMAS

## Valores-base recomendados

| Arma | Base | Função |
|---|---:|---|
| Faca | 6 | rápida |
| Espada | 8 | equilibrada |
| Machado | 11 | pesada |
| Espada Hoshina | 7 | técnica |
| Pistola | 3,5 | suporte |
| Rifle | 5 | distância |

**Manter os valores atuais inicialmente.**

O balanceamento deve vir de:
- postura;
- alcance;
- velocidade;
- recuo;
- animação;
- stamina;
- especiais;
- precisão.

---

# 20. RELEASE DO PLAYER

Manter a estrutura atual:

- limite inicial comum: 5–10%;
- talento raro: 15–30%;
- limite aumenta com treinamento;
- máximo: 100%;
- acima do limite gera calor/desgaste;
- percentual não cai automaticamente.

### Ajuste recomendado

Não deixar Release alto resolver todos os problemas.

Release deve aumentar:
- força;
- velocidade;
- resistência;
- stamina;
- eficiência.

Mas habilidades, técnica e arma continuam importantes.

---

# 21. TREINAMENTO DO PLAYER

O treinamento deve melhorar atributos, mas com retornos progressivamente menores.

### Nível 100

| Atributo | Bônus máximo |
|---|---:|
| Força | +30% dano corpo a corpo |
| Velocidade | +15% |
| Resistência | -20% dano |
| Agilidade | -30% stamina de dash/esquiva |

**Manter como teto inicial.**

---

# 22. BOSS BALANCE

## Honju do Exame
- 160 HP
- 1 Primigenius auxiliar
- dificuldade: introdutória.

## Primigenius Honju Boss
- 480 HP
- fase em 50%;
- 2–4 auxiliares.

## Honju revivido
- 844 HP;
- fases 60% e 30%;
- 2–4 ressurgidos.

### Proposta
Não aumentar muito esses bosses. O foco deve ser:
- fases;
- padrões;
- arena;
- invocações;
- destruição;
- weak points.

---

# 23. NO. 10 — BALANCEAMENTO

## Pequeno

Manter aproximadamente:
- HP 1.650;
- dano 39,5;
- velocidade 0,35;
- regeneração abaixo de 50%;
- transformação para gigante.

### Transformação
Não deve simplesmente restaurar HP sem consequência.

Proposta:
- entrar em transformação após condição;
- sequência de transformação;
- invulnerabilidade apenas durante a animação;
- limpar estados negativos;
- HP da forma gigante definido separadamente;
- criar onda de choque.

## Gigante

Manter aproximadamente:
- HP 4.600;
- dano 54,5;
- velocidade 0,27;
- armadura 18.

### Destruição
O tamanho deve ser o principal diferencial.

---

# 24. PREONDACTYL

Manter identidade de voador.

### Proposta
- HP 430;
- dano 15,5;
- velocidade terrestre 0,31;
- voo 0,55;
- frente ×0,35;
- costas ×1,3;
- autodestruição <15%.

Não aumentar muito o HP. A dificuldade deve vir de:
- voo;
- mergulho;
- ângulo;
- mobilidade;
- ataque surpresa.

---

# 25. TRICHONEPHILA

### Yoju
- HP 65;
- velocidade 0,31;
- teia;
- múltiplas patas;
- salto.

### Honju
- HP 400;
- chefe 600;
- invoca aranhas;
- explosão de teia;
- fúria abaixo de 30%.

A aranha deve ser perigosa por controle de área, não por dano bruto.

---

# 26. KAIJUS NUMERADOS 1–15

Quando houver informação insuficiente para uma versão completa, usar **perfil provisório**, sem inventar que é canônico.

| Kaiju | Perfil inicial |
|---|---|
| No. 1 | percepção/predição |
| No. 2 | força/energia/ondas de choque |
| No. 3 | placeholder |
| No. 4 | voo/mobilidade |
| No. 5 | placeholder |
| No. 6 | criocinese |
| No. 7 | placeholder |
| No. 8 | força/regeneração |
| No. 9 | adaptação/absorção/ressurreição |
| No. 10 | combate/comando |
| No. 11 | hidrocinese |
| No. 12 | evolução de combate |
| No. 13 | velocidade/musculatura |
| No. 14 | forma/ameaça específica conforme referência |
| No. 15 | pressão psicológica |

### Regra
No. 3, No. 5 e No. 7 não devem receber habilidades apresentadas como canônicas se o projeto não tiver uma referência suficientemente estabelecida.

---

# 27. FORMAS DO NO. 9 — HIERARQUIA FINAL

| Forma | Tier | HP | Dano | Função |
|---|---|---:|---:|---|
| No. 9 base | T7 | 1.450 | 34,5 | adaptativo |
| No. 9 preto inicial | T8 | 2.200 | 42 | evolução/adaptação |
| No. 9 + Formiga | T8 | 3.200 | 46 | velocidade/controle |
| No. 9 + No.10 | T8+ | 5.400 | 58 | combate/adaptação |
| No. 9 vermelho/evoluído | T9 | 6.200 | 63 | boss adaptativo |
| No. 9 + No.2/Isao | T9 | 7.500 | 72 | poder/energia |
| No. 9 final adaptativo | T9+ | 9.000 | 82 | boss final |

**A ordem acima é de gameplay do mod, não uma afirmação de canon.**

---

# 28. MATCHUPS IMPORTANTES

| Combate | Resultado esperado |
|---|---|
| Soldado normal × Trichonephila | grupo necessário |
| Soldado elite × Trichonephila | vence sozinho |
| Hoshina × Honju | Hoshina claramente superior |
| Hoshina × No.9 base | Hoshina vence com dificuldade |
| Hoshina × No.10 pequeno | luta difícil |
| Hoshina × No.10 gigante | Hoshina normal perde |
| Hoshina + No.10 × No.10 gigante | Hoshina vence com risco |
| Hoshina + No.10 × No.9 base | Hoshina vence relativamente bem |
| Hoshina + No.10 × No.9 preto | luta difícil |
| Hoshina + No.10 × No.9 + Formiga | depende de mobilidade/controle |
| Hoshina + No.10 × No.9 + No.10 | Hoshina não deve vencer sozinho |
| Kafka No.8 × No.10 gigante | luta de boss |
| Kafka No.8 × No.9 avançado | No.9 deve ter vantagem adaptativa |
| Mina × Daikaiju | excelente matchup |
| Mina × inimigo pequeno | eficiência reduzida |
| Reno + No.6 × grupos | excelente controle |
| Kikoru + No.4 × inimigos terrestres | grande vantagem de mobilidade |
| Gen + No.1 × boss previsível | excelente |
| Isao + No.2 × Daikaiju | extremamente forte |
| No.9 + No.2 × Hoshina | Hoshina precisa de apoio/evento especial |

---

# 29. REGRAS DE REBALANCEAMENTO

Quando um personagem/Kaiju estiver muito forte:

1. verificar IA;
2. verificar cooldown;
3. verificar alcance;
4. verificar regeneração;
5. verificar hitbox;
6. verificar dano;
7. verificar HP;
8. somente depois reduzir atributos.

Quando estiver fraco:

1. verificar comportamento;
2. verificar habilidades;
3. verificar animação/hitbox;
4. verificar posicionamento;
5. verificar regeneração;
6. somente depois aumentar dano/HP.

---

# 30. TEXTURAS, MODELOS E ANIMAÇÕES — PARTE DO BALANCEAMENTO

O balanceamento visual e funcional deve incluir uma auditoria obrigatória de todos os assets.

Verificar:
- texturas faltando;
- UVs quebradas;
- partes brancas/pretas/roxas;
- materiais errados;
- textura de outro personagem;
- partes internas sem textura;
- mãos/pés/rosto/cauda/chifres/garras;
- armaduras;
- armas;
- formas transformadas;
- transparência;
- atlas;
- referências de textura;
- problemas do GeckoLib;
- problemas do renderer.

### Testar em movimento
Cada modelo deve ser testado:
- parado;
- andando;
- correndo;
- pulando;
- atacando;
- recebendo dano;
- sofrendo knockback;
- usando arma;
- transformando;
- regenerando;
- voando;
- usando Release;
- em boss phase.

**Critério:** nenhum asset pode terminar a implementação com áreas sem textura ou artefatos visuais perceptíveis.

---

# 31. PERFORMANCE DO BALANCEAMENTO

Não permitir que balanceamento cause:
- centenas de entidades de invocação;
- destruição ilimitada;
- partículas infinitas;
- clones infinitos;
- loops de regeneração;
- cálculos de IA por tick sem necessidade.

Limites recomendados:
- revividos do No.9: 3–4;
- clones: máximo 3 ativos;
- invocações de boss: limite por fase;
- destruição: orçamento por tick;
- VFX: LOD/distância;
- IA complexa: atualização adaptativa.

---

# 32. PROCEDIMENTO DE TESTE

Para cada entidade:

### Teste A — 1 contra 1
Medir:
- tempo de luta;
- HP restante;
- habilidades usadas;
- mortes;
- regeneração;
- frequência de ataques.

### Teste B — grupo
Medir:
- 2, 4 e 8 soldados;
- sobrevivência;
- tempo de eliminação;
- aggro.

### Teste C — ambiente
Verificar:
- destruição;
- pathfinding;
- obstáculos;
- casas;
- árvores;
- paredes;
- terrenos.

### Teste D — performance
Verificar:
- TPS;
- FPS;
- entidades;
- partículas;
- memória;
- loops de IA.

---

# 33. O QUE NÃO ALTERAR SEM TESTE

Os seguintes valores possuem referência de testes atuais e devem ser preservados inicialmente:

- Hoshina normal: 460 HP;
- Hoshina normal: máximo 92% Release;
- Hoshina + No.10: 520 HP;
- Hoshina + No.10: máximo 100%;
- No.10 gigante: aproximadamente 4.500 HP;
- soldados: multiplicadores atuais de dano 0,25 / 0,35 / 0,8 / 1,0;
- armas-base atuais;
- estrutura de Release;
- sistema de calor/sobrecarga.

O objetivo é mudar somente o que realmente precisa ser corrigido.

---

# 34. RESULTADO ESPERADO

O mod deve produzir uma hierarquia em que:

**soldado comum < soldado elite < especialista < capitão < Numbers Weapon < Daikaiju < Kaiju Numerado < forma avançada < boss final**

sem transformar essa hierarquia em uma simples escala de HP.

O jogador deve perceber diferença entre:
- força;
- velocidade;
- técnica;
- inteligência;
- regeneração;
- alcance;
- mobilidade;
- destruição;
- controle de terreno.

---

# 35. INSTRUÇÃO FINAL PARA O CLAUDE CODE

Antes de alterar qualquer número:

1. auditar os JSONs atuais;
2. comparar com este documento;
3. identificar o valor atual;
4. identificar o valor proposto;
5. aplicar somente as alterações aprovadas;
6. compilar;
7. testar;
8. registrar resultados;
9. recalibrar somente após teste.

**Não apagar sistemas existentes que já funcionam.**
**Não substituir modelos/texturas existentes sem necessidade.**
**Não transformar habilidades distintas em um único multiplicador.**
**Não tratar formas do No. 9 como simples skins.**
**Não tratar Numbers Weapons como simples bônus de dano.**

Este documento é uma proposta de balanceamento abrangente para servir como base de implementação e teste.

---

# 27. BALANCEAMENTO COMPLETO DE PROGRESSÃO DO JOGADOR

> **Status:** proposta de gameplay para o mod. Os valores abaixo são pontos de partida para testes, não números canônicos do anime. O sistema deve funcionar junto com o combate, as patentes, o treinamento corporal, o traje, a stamina, as armas e as missões.

## 27.1 Princípios obrigatórios

1. **Patente não deve ser apenas um título.** Ela libera responsabilidades, acesso a equipamentos, missões e treinamento; não deve conceder poder absurdo automaticamente.
2. **Treinamento corporal e Release são progressões diferentes.** O corpo melhora força, velocidade, resistência, controle e eficiência. O traje libera poder de combate, limitado pela capacidade individual do usuário.
3. **O limite pessoal de Release não é o percentual ativo.** O limite indica o quanto o jogador consegue usar com segurança. O jogador pode ativar menos ou ultrapassar esse limite e sofrer sobrecarga.
4. **Não dar 100% de Release de graça ao subir de patente.** O teto deve depender principalmente de treinamento, avaliações e progresso; a patente dá acesso a instrutores, testes e equipamentos melhores.
5. **Retornos decrescentes.** Os primeiros níveis de treino devem ser perceptíveis; os últimos exigem mais tempo e dão ganhos menores.
6. **Não permitir que a progressão transforme o jogador em mais forte que todos os personagens especiais sem esforço.** Um jogador muito bem treinado pode enfrentar ameaças de alto nível, mas ainda precisa de técnica, equipamento, esquiva, leitura de padrões e preparação.
7. Toda mudança deve ser sincronizada entre atributos, animação, VFX, áudio, stamina, calor e dano recebido.

## 27.2 Patentes e progressão de carreira

A patente controla acesso e papel dentro da Força de Defesa. A progressão deve exigir **XP de serviço + avaliações + objetivos de missão**, e não apenas matar Kaijus repetidamente.

| Patente | Requisito sugerido | Benefícios principais | Limite de progressão recomendado |
|---|---|---|---|
| Candidato | Concluir admissão inicial | Treino básico, traje de treinamento, missões introdutórias | Acesso limitado a missões perigosas |
| Oficial | Concluir o exame e 3 missões | Equipamentos padrão, treino corporal completo básico | Pode entrar em missões de ameaça baixa/média |
| Oficial Sênior | 8 missões, avaliação de combate e sobrevivência | Treino avançado, armas especializadas comuns | Acesso a ameaças médias/altas com esquadrão |
| Líder de Esquadrão | 15 missões e avaliação tática | Comandar soldados, distribuir setores, pedir reforços | Responsabilidade de equipe; não é bônus bruto de dano |
| Vice-Capitão | 25 missões, domínio de arma/Release e prova especial | Comando ampliado, treino de elite, missões críticas | Pode liderar operações de alto risco |
| Capitão | Campanha importante concluída, avaliação de liderança e combate | Formar pelotão próprio, coordenar setores e eventos de invasão | Acesso a operações de Daikaiju e chefes; poder ainda depende do personagem |

Os números de missões são parâmetros iniciais. Não devem ser a única condição: incluir avaliação de desempenho, objetivos secundários, sobrevivência, precisão, resgate de aliados e eficiência de combate.

### Bônus de patente — manter pequeno

| Patente | Bônus passivo sugerido |
|---|---|
| Candidato | Nenhum bônus de atributo |
| Oficial | +2% eficiência de stamina |
| Oficial Sênior | +3% eficiência de stamina adicional |
| Líder de Esquadrão | +2% resistência a stagger/empurrão |
| Vice-Capitão | +3% eficiência de stamina adicional |
| Capitão | +2% resistência a stagger/empurrão adicional |

Esses bônus são cumulativos, mas deliberadamente modestos. A maior recompensa por patente é o acesso a sistemas, equipamentos, comando e missões — não uma multiplicação de dano.

## 27.3 Treinamento corporal

Usar quatro atributos treináveis, cada um com nível de 0 a 100. O nível deve crescer por sessões e desafios, não apenas por ficar parado repetindo uma ação. O jogador ganha XP de treino ao completar exercícios válidos, com limite diário/por sessão para impedir farming automático.

| Atributo | O que melhora | Bônus no nível 100 |
|---|---|---:|
| Força | Dano corpo a corpo, empurrão e uso de armas pesadas | +30% dano corpo a corpo |
| Velocidade | Corrida, aceleração e recuperação de movimento | +15% velocidade de movimento |
| Resistência | Redução de dano e tolerância ao esforço | Até -20% dano recebido de fontes comuns |
| Agilidade | Esquiva, dash, parkour e recuperação | Até -30% custo de stamina de esquiva/dash |

**Regra contra excesso:** os bônus não devem ser multiplicados novamente por cada patente. Aplicar os bônus do corpo uma única vez no cálculo final, antes dos modificadores temporários de Release.

### Etapas visíveis do treinamento

| Nível do atributo | Etapa | Efeito de gameplay |
|---|---|---|
| 0–19 | Iniciante | Controles básicos, pouca eficiência |
| 20–39 | Condicionado | Primeira melhoria perceptível; menos gasto ou melhor controle |
| 40–59 | Avançado | Movimento e combate mais consistentes |
| 60–79 | Elite | Melhor recuperação, eficiência e execução de manobras |
| 80–99 | Excepcional | Ganhos pequenos, mas importantes; exige desafios difíceis |
| 100 | Limite humano treinado do mod | Recebe o bônus máximo da tabela, sem se tornar invulnerável |

### Como treinar

- **Força:** circuitos de combate, golpes em alvos de treino, armas pesadas e provas de potência.
- **Velocidade:** sprints cronometrados, percurso com obstáculos e deslocamento sob pressão.
- **Resistência:** circuitos longos, defesa, sobrevivência e manutenção de desempenho.
- **Agilidade:** esquivas, parries, parkour, mudanças de direção e percursos de reação.

Cada exercício precisa validar movimento/execução real. Repetir a mesma ação sem cumprir o desafio dá XP reduzido ou nenhum XP. O treino deve ter animações, feedback e resultados claros.

## 27.4 Capacidade pessoal de Release

Separar três valores:

- `releaseCurrent`: percentual que o jogador está usando neste instante.
- `releaseSafeLimit`: percentual máximo que o corpo suporta sem sobrecarga contínua.
- `releasePotential`: teto que pode ser alcançado com progressão, talento e avaliações.

O jogador precisa estar usando um traje compatível para ativar Release. O percentual não cai automaticamente porque o jogador recebeu dano; ele deve reduzir manualmente ou sofrer consequências de sobrecarga, sem alterar silenciosamente o valor escolhido.

### Limite inicial e evolução

| Perfil inicial | Chance sugerida | Limite inicial seguro |
|---|---:|---:|
| Comum | 90% | 5–10% |
| Talento acima da média | 9% | 11–20% |
| Talento raro | 1% | 21–30% |

A geração de personagem pode ser substituída por uma avaliação inicial de desempenho se o mod não tiver criação de personagem. O talento inicial define o ponto de partida, não determina sozinho o teto final.

### Marcos de limite seguro

| Progresso corporal e avaliações | Faixa de limite seguro | Significado |
|---|---:|---|
| Admissão concluída | 5–15% | Controle básico do traje |
| Treino inicial concluído | 15–25% | Uso consistente em combate curto |
| Treino avançado + missões médias | 25–40% | Operador competente |
| Treino de elite + avaliação especial | 40–60% | Combatente de alto nível |
| Missões críticas + domínio corporal elevado | 60–80% | Elite excepcional |
| Provas finais, domínio avançado e progressão longa | 80–100% | Potencial máximo; extremamente difícil |

Essas faixas são metas de progressão, não uma concessão automática ao atingir certa patente. O jogador deve cumprir os requisitos de treino e controle. Para evitar grind excessivo, cada marco deve ter uma prova única que demonstre controle real do Release.

### Ganho por treinamento

- Treino corporal aumenta a eficiência de uso e contribui para o limite seguro, mas não concede percentual ilimitado por repetição.
- Sessões normais: ganhos pequenos de XP corporal.
- Treino supervisionado de Release: progresso de controle e limite seguro.
- Avaliações de patente e missões críticas: desbloqueiam o próximo teto de progresso.
- Acima de 60%, os requisitos aumentam muito; acima de 80%, cada avanço exige domínio de vários atributos e provas especiais.
- O limite seguro nunca sobe durante uma luta só porque o jogador está causando dano. A progressão deve ocorrer em treinamento/avaliação e ser persistente.

## 27.5 Efeito do Release em combate

O Release amplifica o traje, mas deve ter ganhos fortes sem fazer todos os ataques escalarem linearmente até ficarem quebrados. Aplicar o seguinte multiplicador de referência aos atributos amplificados pelo traje:

| Release ativo | Dano corpo a corpo | Velocidade | Redução adicional de dano recebido | Custo de stamina |
|---:|---:|---:|---:|---:|
| 0% | ×1,00 | ×1,00 | 0% | ×1,00 |
| 10% | ×1,10 | ×1,04 | 2% | ×1,02 |
| 30% | ×1,30 | ×1,10 | 6% | ×1,08 |
| 50% | ×1,55 | ×1,16 | 10% | ×1,16 |
| 70% | ×1,80 | ×1,22 | 14% | ×1,28 |
| 90% | ×2,05 | ×1,28 | 18% | ×1,42 |
| 100% | ×2,20 | ×1,32 | 20% | ×1,50 |

Os valores são propostas para o cálculo do traje; não multiplicar dano, velocidade e resistência por várias tabelas diferentes ao mesmo tempo. Armas de fogo devem receber benefícios de estabilidade, recuo/recuperação e capacidade de perfuração conforme o tipo de arma, não o mesmo multiplicador de dano corpo a corpo.

**Importante:** o bônus de resistência do Release não deve permitir somar reduções ilimitadas com armadura, treino corporal e habilidades. Usar um teto de redução total recomendado de 60% contra dano comum; ataques especiais, weak points e mecânicas de boss podem contornar parte dessa defesa.

## 27.6 Sobrecarga acima do limite seguro

A sobrecarga começa quando `releaseCurrent > releaseSafeLimit`. O percentual ativo permanece no valor escolhido até o jogador reduzir manualmente, mas o custo fisiológico aumenta com a diferença entre o uso e o limite.

| Excesso sobre o limite seguro | Estado | Efeito sugerido |
|---:|---|---|
| 1–5 pontos percentuais | Aviso | Calor e aura instável leves; sem dano contínuo significativo |
| 6–10 pontos | Sobrecarga leve | Calor crescente, stamina máxima temporariamente reduzida |
| 11–20 pontos | Sobrecarga alta | Dano gradual, tremor/recuperação pior, custo de stamina aumentado |
| 21–30 pontos | Sobrecarga crítica | Dano mais rápido, mira e movimentos menos estáveis, risco de colapso após prolongar o uso |
| Mais de 30 pontos | Zona extrema | Dano severo e risco de incapacitação; permitido apenas como decisão de alto risco |

Fórmula de partida para dano contínuo:

`overloadDamagePerSecond = 0.06 × excessPercentagePoints`

Aplicar a fórmula somente depois de uma janela curta de tolerância e combiná-la com o sistema de calor. O valor 0,06 é o dano por segundo por ponto percentual acima do limite, não por cada 1% de Release total. Ajustar em testes para que ultrapassar o limite por poucos segundos seja arriscado, mas não instantaneamente fatal.

- 40% de limite seguro usando 40%: uso seguro.
- 40% de limite seguro usando 50%: 10 pontos de excesso; começa a acumular calor e sobrecarga.
- 40% de limite seguro usando 70%: 30 pontos de excesso; estado crítico.
- 40% de limite seguro usando 100%: 60 pontos de excesso; zona extrema.

O jogador deve receber feedback claro por cor/intensidade da aura, efeitos sonoros moderados, indicador de calor e sinais de animação. Não usar flashes excessivos nem áudio ensurdecedor.

## 27.7 Trajes e compatibilidade

| Traje | Defesa-base | Controle de calor | Compatibilidade sugerida |
|---|---:|---:|---|
| Traje de treinamento | 8 | 0% de redução de calor | Treino e primeiras missões; recomendado até 20% de Release |
| Traje padrão Mk I | 12 | 15% de redução de calor | Uso geral; adequado a operadores treinados |
| Traje reforçado Mk I | 15 | 30% de redução de calor | Missões perigosas e Release elevado, desde que desbloqueado |
| Trajes especiais/Numbers | Definido por modelo | Perfil individual | Não devem ser simplesmente versões com mais armadura; precisam de requisitos e comportamento próprios |

Um traje melhor pode reduzir calor e melhorar a eficiência, mas **não aumenta automaticamente o limite fisiológico do jogador**. Equipamentos especiais podem oferecer controle, proteção ou funções específicas; ainda exigem treino.

## 27.8 Integração entre patente, corpo e Release

O cálculo de poder deve seguir esta ordem para evitar bônus duplicados:

1. Estatísticas-base do jogador e da arma.
2. Bônus permanente do treinamento corporal.
3. Bônus modestos de patente, se aplicável.
4. Modificador de Release ativo.
5. Modificador específico de arma/habilidade.
6. Penalidades de stamina, calor, ferimentos e sobrecarga.
7. Resistência, armadura e regras especiais do alvo.

A patente não multiplica novamente os bônus de treino. O Release não aumenta o limite seguro durante o combate. Ferimentos não reduzem automaticamente o percentual de Release escolhido, mas podem causar penalidades de movimento, stamina e estabilidade conforme o sistema de combate.

## 27.9 Ritmo de progressão recomendado

- Primeiras horas: concluir admissão, aprender combate e atingir 10–20% de limite seguro.
- Início/meio do jogo: Oficial/Oficial Sênior, corpo entre níveis 20–50 e Release seguro entre 20–40%.
- Meio/final: Líder/Vice-Capitão, corpo entre 50–80 e Release seguro entre 40–70%.
- Final de jogo: Capitão e missões críticas, corpo entre 80–100 e possibilidade de 70–100% de limite seguro.

O tempo real depende da duração das missões e do estilo de jogo; usar esses intervalos como metas relativas, não como horas rígidas. O jogador não deve precisar repetir centenas de tarefas iguais. Variar objetivos, provas e recompensas.

## 27.10 Regras anti-exploit e equilíbrio

- XP de treino tem limite por exercício e retorna menos XP quando repetido sem variação.
- Não conceder XP por ataques contra aliados, entidades invulneráveis ou alvos sem risco.
- Missões repetidas dão recompensa reduzida quando farmadas em sequência.
- Morrer ou falhar não deve apagar toda a progressão; pode reduzir recompensa da missão, não os níveis já conquistados.
- Evitar que um jogador com 100% de Release e corpo nível 100 derrote qualquer boss com ataques básicos. Manter weak points, padrões, stamina, janelas de vulnerabilidade e mecânicas de fase.
- Bônus de patente, traje, corpo e Release precisam de um único lugar centralizado de cálculo, com logs de debug para identificar multiplicadores duplicados.
- Exibir no menu: patente, XP de serviço, nível de cada atributo, limite seguro, Release atual, excesso, calor, stamina e requisitos do próximo marco.

## 27.11 Testes obrigatórios antes de aprovar

1. Jogador recém-admitido com 10% de Release contra Yoju.
2. Oficial com 25% de limite seguro contra Honju acompanhado de esquadrão.
3. Oficial Sênior com 40% contra ameaça média sem ajuda.
4. Vice-Capitão com 70% contra boss, exigindo esquiva e exploração de weak points.
5. Jogador com limite seguro de 40% usando 50%, 70% e 100% para validar sobrecarga.
6. Jogador com corpo nível 100, traje reforçado e 100% de Release contra No. 10 gigante e formas avançadas do No. 9.
7. Comparar build focada em força, velocidade, resistência e agilidade; nenhuma deve ser a melhor em absolutamente tudo.
8. Testar multiplayer/servidor para garantir que Release, stamina, calor, dano e animações sejam sincronizados.
9. Confirmar que mudar de patente não duplica bônus de atributos.
10. Medir tempo de progressão e ajustar requisitos para que o avanço seja perceptível sem virar grind repetitivo.

**Critério de aprovação:** o jogador sente uma evolução clara desde Candidato até Capitão, mas o poder final vem da combinação de treino, controle do traje, equipamento, habilidade e decisões de combate — não apenas de subir de nível.


---

# 36. MATRIZ DE PROBABILIDADE DE VITÓRIA — PERSONAGENS × KAIJUS

## 36.1 Como interpretar os percentuais

**Todos os percentuais desta seção são metas estimadas de balanceamento, não resultados medidos nem estatísticas canônicas.** São hipóteses iniciais para orientar testes automatizados e manuais. Onde já existem duelos medidos, eles são anotados separadamente e têm prioridade como evidência sobre a estimativa.

Condições padrão para a matriz:
- duelo individual, sem aliados, sem intervenção de jogadores e sem consumíveis externos;
- IA em dificuldade padrão, terreno relativamente aberto e equipamento previsto no perfil da personagem;
- cada personagem começa com vida cheia e cooldowns prontos;
- “vitória” significa derrotar o Kaiju sem morrer; não inclui objetivos de missão, resgate ou defesa de setor;
- a estimativa deve ser recalibrada após pelo menos 10 testes por confronto. Se a variação de resultados for alta, usar 20 testes;
- probabilidades não devem ser implementadas diretamente como sorteio. Elas são um alvo observado a partir de atributos, IA, habilidades, cooldowns, mobilidade e matchup.

**Tier é uma referência, não uma fórmula absoluta.** Especialização, voo, ataques à distância, resistência, adaptação e capacidade de punir o estilo do adversário podem alterar o resultado.

## 36.2 Tiers usados para a estimativa

| Tier | Interpretação de gameplay |
|---|---|
| T2–T3 | operadores, recrutas e personagens de suporte |
| T4 | combatentes experientes/especialistas limitados |
| T5 | especialista de alto nível, como Kikoru normal nesta configuração |
| T5+–T6 | espadachim de elite, atiradora anti-Daikaiju, comandante ou tanque avançado |
| T7 | personagem com Numbers Weapon/forma especial de alto nível |
| T8 | Kaiju No. 8 e formas superiores equivalentes |
| T9+ | formas finais/adaptativas do No. 9 |

## 36.3 Kaijus cobertos

| Código | Kaiju | Tier de referência |
|---|---|---:|
| K1 | Trichonephila Yoju | T2 |
| K2 | Primigenius Yoju | T3 |
| K3 | Primigenius ressurgido | T3+ |
| K4 | Primigenius Honju | T4 |
| K5 | Primigenius Honju revivido | T4+ |
| K6 | Trichonephila Honju (versão normal/boss conforme configuração) | T4+ |
| K7 | Preondactyl | T4+ |
| K8 | Kaiju No. 10 pequeno | T7 |
| K9 | Kaiju No. 10 gigante | T7+ |
| K10 | Kaiju No. 9 base | T7 |
| K11 | No. 9 preto inicial | T8 |
| K12 | No. 9 + Formiga (adaptação original do mod) | T8 |
| K13 | No. 9 + No. 10 (adaptação original do mod) | T8+ |
| K14 | No. 9 vermelho/evoluído | T9 |
| K15 | No. 9 + No. 2/Isao | T9 |
| K16 | No. 9 final adaptativo | T9+ |

**Não há percentuais atribuídos aos Kaijus Numerados 3, 5 e 7, nem aos Nos. 11–15, porque o documento atual não contém atributos completos e testáveis para eles.** Para esses confrontos, preencher a matriz só depois de definir HP, dano, defesa, habilidades, IA e tier. Isso evita fabricar precisão com dados insuficientes.

## 36.4 Probabilidade estimada de vitória — personagens principais

Cada célula é uma meta percentual estimada de vitória em duelo solo. Valores com “†” são confrontos calibrados parcialmente por testes anteriores; ainda não representam amostra estatística robusta.

| Personagem / forma | Tier | K1 | K2 | K3 | K4 | K5 | K6 | K7 | K8 | K9 | K10 | K11 | K12 | K13 | K14 | K15 | K16 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Kafka humano | T2 | 65 | 35 | 25 | 12 | 8 | 10 | 8 | 1 | 1 | 1 | 1 | 1 | 1 | 1 | 1 | 1 |
| Kafka / Kaiju No. 8 | T8 | 99 | 99 | 99 | 98 | 97 | 96 | 95 | 75 | 45 | 40 | 20 | 15 | 8 | 5 | 3 | 2 |
| Mina Ashiro | T6 | 99 | 98 | 97 | 95 | 93 | 94 | 95 | 35 | 30 | 25 | 15 | 20 | 12 | 8 | 5 | 3 |
| Hoshina normal | T5+ | 99 | 98 | 96 | 95 | 90 | 90 | 82 | 58† | 10† | 70† | 30 | 20 | 8 | 5 | 2 | 1 |
| Hoshina + Numbers Weapon 10 | T7 | 99 | 99 | 98 | 97 | 95 | 94 | 90 | 90 | 60† | 90† | 45 | 35 | 18 | 10 | 5 | 3 |
| Reno normal | T5 | 98 | 95 | 92 | 85 | 78 | 82 | 75 | 18 | 5 | 12 | 4 | 6 | 2 | 1 | 1 | 1 |
| Reno + No. 6 | T7 | 99 | 99 | 98 | 97 | 96 | 96 | 95 | 75 | 55 | 70 | 40 | 45 | 25 | 15 | 8 | 5 |
| Kikoru normal | T5 | 99 | 97 | 95 | 88 | 82 | 85 | 78 | 35 | 8 | 25† | 12 | 12 | 5 | 3 | 1 | 1 |
| Kikoru + No. 4 | T7 | 99 | 99 | 98 | 97 | 95 | 95 | 96 | 75 | 45 | 75 | 45 | 50 | 25 | 15 | 8 | 5 |
| Gen Narumi normal | T6 | 99 | 98 | 97 | 94 | 90 | 92 | 88 | 40 | 15 | 35 | 20 | 20 | 10 | 6 | 3 | 2 |
| Gen + No. 1 | T7 | 99 | 99 | 99 | 98 | 96 | 97 | 96 | 82 | 50 | 85 | 55 | 50 | 30 | 18 | 10 | 6 |
| Isao normal | T6 | 99 | 99 | 98 | 96 | 94 | 92 | 88 | 45 | 18 | 30 | 12 | 15 | 8 | 4 | 2 | 1 |
| Isao + No. 2 | T7+ | 99 | 99 | 99 | 98 | 97 | 95 | 92 | 85 | 60 | 75 | 40 | 45 | 25 | 15 | 8 | 5 |
| Iharu | T4 | 98 | 90 | 85 | 65 | 50 | 55 | 45 | 5 | 1 | 3 | 1 | 1 | 1 | 1 | 1 | 1 |
| Haruichi | T4 | 98 | 88 | 80 | 60 | 45 | 50 | 40 | 4 | 1 | 2 | 1 | 1 | 1 | 1 | 1 | 1 |
| Aoi | T4 | 98 | 92 | 86 | 70 | 55 | 60 | 48 | 6 | 1 | 3 | 1 | 1 | 1 | 1 | 1 | 1 |
| Eiji | T5 | 99 | 96 | 92 | 80 | 70 | 75 | 65 | 12 | 3 | 8 | 2 | 3 | 1 | 1 | 1 | 1 |
| Rin | T4 | 99 | 93 | 88 | 68 | 52 | 62 | 60 | 10 | 2 | 7 | 2 | 3 | 1 | 1 | 1 | 1 |
| Kota | T4 | 99 | 93 | 87 | 68 | 52 | 58 | 55 | 8 | 2 | 5 | 2 | 2 | 1 | 1 | 1 | 1 |
| Jugo | T5 | 99 | 96 | 93 | 82 | 72 | 75 | 66 | 15 | 4 | 10 | 3 | 4 | 1 | 1 | 1 | 1 |
| Toko | T5 | 99 | 95 | 91 | 80 | 68 | 73 | 65 | 13 | 3 | 9 | 3 | 3 | 1 | 1 | 1 | 1 |
| Soichiro | T5+ | 99 | 97 | 94 | 88 | 80 | 82 | 72 | 30 | 7 | 22 | 8 | 10 | 3 | 2 | 1 | 1 |
| Hikari | T6 | 99 | 98 | 97 | 94 | 90 | 91 | 94 | 35 | 12 | 30 | 15 | 20 | 8 | 5 | 2 | 1 |
| Jura | T5+ | 99 | 97 | 94 | 87 | 78 | 82 | 72 | 25 | 6 | 18 | 6 | 8 | 2 | 1 | 1 | 1 |
| Akari | T3+ | 95 | 80 | 72 | 45 | 30 | 35 | 25 | 2 | 1 | 1 | 1 | 1 | 1 | 1 | 1 | 1 |
| Hakua | T4 | 98 | 90 | 84 | 65 | 50 | 55 | 45 | 5 | 1 | 3 | 1 | 1 | 1 | 1 | 1 | 1 |
| Ryo | T4 | 98 | 91 | 85 | 67 | 52 | 58 | 48 | 6 | 1 | 4 | 1 | 1 | 1 | 1 | 1 | 1 |
| Tae | T3 | 90 | 70 | 60 | 30 | 18 | 22 | 15 | 1 | 1 | 1 | 1 | 1 | 1 | 1 | 1 | 1 |
| Konomi | T2 | 75 | 40 | 30 | 12 | 6 | 8 | 5 | 1 | 1 | 1 | 1 | 1 | 1 | 1 | 1 | 1 |
| Keiji | T2 | 78 | 42 | 32 | 14 | 8 | 10 | 7 | 1 | 1 | 1 | 1 | 1 | 1 | 1 | 1 | 1 |
| Juzo | T3+ | 95 | 80 | 72 | 45 | 30 | 35 | 25 | 2 | 1 | 1 | 1 | 1 | 1 | 1 | 1 | 1 |
| Akira | T3+ | 95 | 80 | 72 | 45 | 30 | 35 | 25 | 2 | 1 | 1 | 1 | 1 | 1 | 1 | 1 | 1 |

### Leitura importante da linha da Kikoru normal

A Kikoru normal permanece em **T5, 390 HP e velocidade 0,34**. O alvo proposto contra o No. 9 base T7 é **25% de vitórias**: ela pode vencer em condições favoráveis, mas deve perder na maioria dos duelos individuais. Contra No. 10 pequeno, o alvo é 35%, também com alta dificuldade. Esses valores não contradizem a existência de uma vitória lenta observada: um único resultado demonstra possibilidade, não a frequência de vitória.

Kikoru + No. 4 é uma configuração distinta de T7; não usar os percentuais da Kikoru normal para balancear a forma equipada.

## 36.5 Probabilidades dos soldados por classe

| Classe | K1 | K2 | K3 | K4 | K5 | K6 | K7 | K8 | K9 | K10 | K11–K16 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Recruta T1 | 30% | 8% | 5% | 2% | 1% | 1% | 1% | 1% | 1% | 1% | 1% cada |
| Soldado normal T2 | 55% | 20% | 12% | 5% | 2% | 3% | 2% | 1% | 1% | 1% | 1% cada |
| Soldado alto T3 | 75% | 35% | 25% | 10% | 5% | 8% | 5% | 1% | 1% | 1% | 1% cada |
| Elite T3+ | 85% | 45% | 35% | 15% | 8% | 12% | 8% | 1% | 1% | 1% | 1% cada |
| Especialista T4 | 90% | 55% | 45% | 20% | 12% | 18% | 15% | 2% | 1% | 1% | 1% cada |

Os percentuais de soldados pressupõem duelo solo. Em grupo, o resultado pode aumentar substancialmente por foco de fogo, distração, suporte e revives; esse é um cenário separado e precisa de outra bateria de testes.

## 36.6 Evidências de testes já registrados

Os dados abaixo são resultados relatados nos testes existentes; não os confundir com as estimativas das tabelas:

| Confronto já testado | Resultado relatado | Como usar |
|---|---|---|
| Hoshina normal × No. 9 base | 3 vitórias em 3 testes, 31–34 s, termina com 63–75% de vida | Amostra pequena; preservar como referência atual e repetir com 10+ testes antes de calibrar a estimativa final. |
| Hoshina normal × No. 10 pequeno | 5/5 vitórias, 40–64 s, termina com 4–45% de vida | Alta variação de vida restante; medir dano recebido, técnicas e RNG. |
| Hoshina normal × No. 10 gigante | 0/3 vitórias; No. 10 termina com 11–19% de vida | Confronto muito próximo, mas o gigante é favorito. |
| Hoshina + No. 10 × No. 10 gigante | 4/4 vitórias, cerca de 1 min, termina com 22–51% de vida | Forma híbrida tem vantagem, mas precisa de amostra maior. |
| Hoshina + No. 10 × No. 9 base | vitórias relatadas em cerca de 25 s, termina com 71–88% de vida | Pode estar excessivamente favorável; repetir com mais testes e conferir se o No. 9 usa todas as habilidades. |
| Kikoru normal × Honju marrom | vitórias em 11–12 s | Teste relatado pelo usuário; número de repetições não especificado. |
| Kikoru normal × No. 9 base | vitórias em 38–42 s, termina com 45–49% de vida | Demonstra que a Kikoru pode vencer; não estima a probabilidade sem amostra de derrotas/vitórias. Meta nova: 25% de vitória em série de testes. |
| Kikoru normal × No. 10 pequeno | vitórias em 28–34 s, termina com 10–51% de vida | Variação elevada; repetir pelo menos 10 vezes. Meta inicial: 35% de vitória. |

## 36.7 Regras para transformar estimativas em balanceamento real

1. Executar 10 duelos por matchup prioritário, registrando vitórias, tempo, vida restante, dano causado/recebido, habilidades usadas, número de esquivas e regeneração efetiva.
2. Para resultados muito variáveis ou bosses adaptativos, executar 20 duelos.
3. Calcular taxa observada: `vitórias / total de duelos × 100`.
4. Não mudar HP se o problema for IA que não usa habilidade, hitbox errada, pathfinding, ataque sem sincronização, falha de voo ou regeneração excessiva.
5. Se a taxa observada estiver a mais de 15 pontos percentuais da meta em pelo menos 10 testes, identificar a causa antes de alterar os números.
6. Para personagens T5 contra Kaiju T7, alvo geral de 15–35% conforme especialização; para personagem T7 contra Kaiju T7, 40–65%; para personagem T8 contra Kaiju T7, 60–85%. Ajustar exceções por matchup.
7. Não impor uma probabilidade exata no código nem manipular resultados com sorteio artificial. A probabilidade emerge da simulação do combate.
8. Repetir os testes em terreno aberto e em cenário de invasão. Um personagem pode ter baixa chance em duelo solo e alto valor estratégico em equipe.
9. Para Mina, separar cenário com distância/linha de tiro de combate corpo a corpo. Para Kikoru + No.4, separar combate aéreo de combate terrestre. Para Reno + No.6, considerar grupos e controle de área. Para Gen + No.1, ativar corretamente a previsão. Para Hoshina + No.10, verificar cauda independente e Full Release.
10. Não preencher os confrontos de No. 3, 5, 7, 11, 12, 13, 14 e 15 com percentuais confiáveis até seus perfis implementados terem atributos e comportamento de combate definidos.

## 36.8 Testes prioritários para a próxima rodada

1. **Kikoru T5 × No. 9 T7:** alvo 25% de vitória; 10 testes iniciais, manter 390 HP e 0,34 de velocidade até conhecer a taxa real.
2. **Kikoru T5 × No. 10 pequeno T7:** alvo 35%; verificar a variação entre 10% e 51% de vida final relatada.
3. **Hoshina T5+ × No. 9 T7:** repetir os 3/3 resultados atuais em 10–20 testes para verificar se o No. 9 usa adaptação, Finger Gun, regeneração e invocação corretamente.
4. **Hoshina + No. 10 × No. 9 base:** verificar se a vitória em aproximadamente 25 s com 71–88% de vida é consequência de habilidades ou de um erro de IA.
5. **Mina T6 × No. 10 gigante:** testar com linha de tiro aberta e obstruída; o resultado deve refletir seu papel anti-Daikaiju.
6. **Reno + No. 6, Kikoru + No. 4, Gen + No. 1 e Isao + No. 2:** testar separadamente de suas formas normais; Numbers Weapons não são simples multiplicadores.

## 36.9 Regra final de hierarquia

A hierarquia T5 → T7 deve ser sentida, mas não virar uma regra de vitória automática. O No. 9 T7 deve vencer a Kikoru T5 na maioria dos duelos solo; a Kikoru pode vencer ocasionalmente se a IA, o terreno e a execução das habilidades favorecerem seu estilo. Em invasões, aliados, objetivos e controle de setor mudam o resultado e precisam de métricas separadas.

**Esta seção complementa o balanceamento anterior. Se uma estimativa aqui conflitar com um resultado medido, preservar o resultado medido como evidência, aumentar a amostra e revisar a meta; não sobrescrever o teste com um percentual teórico.**
