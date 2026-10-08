# BALANCEAMENTO COMPLETO — KAIJU NO. 8 / MINECRAFT
## Versão 1.0 — proposta consolidada a partir dos balanceamentos atuais

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
