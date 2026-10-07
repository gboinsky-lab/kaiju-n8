# KAiju No. 8 --- Especificação de Habilidades e IA dos Mobs

## Documento de implementação para Claude Code

> Objetivo: usar este documento como especificação para implementar os
> mobs e suas habilidades no mod de Minecraft Kaiju No. 8.
>
> Stack assumida: Minecraft + GeckoLib.
>
> IMPORTANTE: separar claramente entidades, habilidades e estados. Não
> transformar todos os ataques em simples dano instantâneo. Sempre que
> possível, usar animações GeckoLib, partículas, hitboxes, cooldowns,
> telegraph e máquinas de estado.

------------------------------------------------------------------------

# 1. PRINCÍPIOS GERAIS DE IMPLEMENTAÇÃO

## 1.1 Sistema de habilidades

Cada mob deve possuir um sistema de habilidades com:

-   nome interno;
-   nome exibido;
-   animação GeckoLib;
-   cooldown;
-   duração;
-   alcance;
-   dano;
-   knockback;
-   prioridade;
-   condições de uso;
-   telegraph;
-   partículas/efeitos;
-   som;
-   possibilidade de interromper/cancelar;
-   interação com blocos;
-   interação com outras entidades.

Estrutura conceitual:

``` text
Mob
 ├── Attributes
 ├── AI State Machine
 ├── Ability Controller
 ├── Animation Controller
 ├── Target Controller
 ├── Cooldown Manager
 ├── Phase Manager
 └── Special Effects
```

------------------------------------------------------------------------

# 2. HOSHINA --- TRAJE PADRÃO

## Função

Hoshina normal deve ser um combatente extremamente rápido, especializado
em combate corpo a corpo com espadas.

Ele não deve lutar como um mob comum.

Prioridades:

1.  aproximar-se rapidamente;
2.  esquivar;
3.  atacar com combos;
4.  contra-atacar ataques pesados;
5.  reposicionar;
6.  executar finalizações quando o inimigo estiver vulnerável.

## Atributos sugeridos

-   velocidade: muito alta;
-   dano individual: médio/alto;
-   HP: humano/elite, não nível de Kaiju;
-   resistência: média;
-   knockback resistance: média;
-   alcance: curto/médio;
-   prioridade: mobilidade \> defesa \> ataque bruto.

------------------------------------------------------------------------

## 2.1 Kūuchi --- Corte à Distância

Tipo: - ataque ranged de curta/média distância.

Comportamento: - Hoshina realiza um corte; - cria um slash projectile; -
projétil viaja em linha reta; - pode atravessar entidades; - desaparece
depois de determinado alcance.

Sugestão: - alcance: 8--12 blocos; - dano: médio; - cooldown: 1--2 s.

Animação: `hoshina_kūuchi`

------------------------------------------------------------------------

## 2.2 Kōsa-uchi --- Corte Cruzado

Tipo: - ataque ranged.

Comportamento: - Hoshina cruza as duas espadas; - dispara dois cortes; -
trajetórias formam um X.

Sugestão: - 2 projéteis; - dano individual menor que Kūuchi; - dano
combinado maior; - cooldown médio.

Animação: `hoshina_kosa_uchi`

------------------------------------------------------------------------

## 2.3 Kaeshi-uchi --- Contra-Ataque

Tipo: - counter/dodge.

Condição: - inimigo entra em ataque pesado; - ataque está prestes a
atingir Hoshina.

Comportamento: 1. Hoshina detecta o ataque; 2. executa dash lateral; 3.
fica brevemente invulnerável; 4. passa pelo inimigo; 5. gira; 6. desfere
contra-ataque.

Importante: - não usar como ataque aleatório; - deve depender do
comportamento do alvo.

Animação: `hoshina_kaeshi_uchi`

------------------------------------------------------------------------

## 2.4 Ran-uchi --- Barragem de Cortes

Tipo: - combo melee de alto DPS.

Comportamento: - avanço rápido; - múltiplos cortes; - pequenos
deslocamentos; - finalização frontal.

Sugestão: - 8--12 hits; - duração aproximada: 1 s; - cooldown: 5--8 s.

Animação: `hoshina_ran_uchi`

------------------------------------------------------------------------

## 2.5 Kasumi-uchi --- Corte da Névoa

Tipo: - combo/delayed attack.

Sequência:

1.  primeiro corte;
2.  segundo corte usado como distração;
3.  pequeno reposicionamento;
4.  terceiro golpe forte.

Sugestão de dano: - hit 1 = 20%; - hit 2 = 20%; - hit 3 = 60%.

Animação: `hoshina_kasumi_uchi`

------------------------------------------------------------------------

## 2.6 Yae-uchi --- Oito Cortes

Tipo: - combo de burst.

Comportamento: - 8 golpes muito rápidos; - concentrados no alvo; - pode
reduzir temporariamente resistência/armadura do alvo; - excelente contra
Honju.

Sugestão: - 8 hits; - cooldown alto; - pequeno knockback no último
golpe.

Animação: `hoshina_yae_uchi`

------------------------------------------------------------------------

## 2.7 Dash / Evasão

Hoshina deve possuir uma habilidade de mobilidade separada.

Comportamento: - dash lateral; - dash para trás; - dash para frente; -
chance de evitar ataques.

Cooldown: - curto.

Não permitir spam infinito.

------------------------------------------------------------------------

## 2.8 Parry

Quando um ataque corpo a corpo estiver prestes a acertar:

-   chance de bloquear;
-   reduzir dano;
-   gerar partículas;
-   som metálico;
-   opcionalmente abrir janela para Kaeshi-uchi.

------------------------------------------------------------------------

# 3. HOSHINA + NUMBERS WEAPON 10

Esta entidade/configuração deve ser diferente do Hoshina normal.

## Características

-   duas espadas;
-   terceira lâmina manipulada pela cauda;
-   cauda do No. 10;
-   consciência do Numbers 10;
-   ataques coordenados;
-   Full Release;
-   ultimate Jūni-hitoe.

------------------------------------------------------------------------

# 3.1 Tail Slash

A cauda executa um corte independentemente das mãos.

Comportamento: - atacar alvo atrás ou ao lado; - detectar inimigos
próximos; - executar ataque mesmo enquanto Hoshina está atacando outro
alvo.

------------------------------------------------------------------------

# 3.2 Tail Guard

A cauda pode bloquear ataques direcionados ao Hoshina.

Condição: - projétil ou ataque pesado vindo pela retaguarda/lateral.

Efeito: - redução de dano; - animação defensiva; - partículas.

------------------------------------------------------------------------

# 3.3 Third Sword

A cauda pode segurar/manipular uma terceira espada.

A IA deve tratar essa espada como uma arma independente.

Isso permite:

``` text
mão esquerda = espada
mão direita = espada
cauda = espada
```

------------------------------------------------------------------------

# 3.4 Independent Tail AI

IMPORTANTE:

A cauda não deve simplesmente repetir a animação do corpo.

Criar um controlador independente:

``` text
TailController
 ├── detectThreat()
 ├── detectTarget()
 ├── attack()
 ├── defend()
 ├── counter()
 └── assist()
```

Prioridade:

1.  proteger Hoshina;
2.  atacar inimigo vulnerável;
3.  defender de projéteis;
4.  auxiliar combo.

------------------------------------------------------------------------

# 3.5 Synchronization

Criar atributo:

`numbers10_sync`

Faixa:

`0–100%`

A sincronização aumenta durante o combate.

Exemplo:

``` text
0–50%   = baixa sincronização
50–80%  = boa sincronização
80–99%  = alta sincronização
100%    = Full Release
```

------------------------------------------------------------------------

# 3.6 Full Release --- 100%

Ao atingir 100%:

-   aumentar velocidade;
-   aumentar dano;
-   aumentar velocidade da cauda;
-   desbloquear ataques avançados;
-   reduzir cooldowns;
-   liberar Jūni-hitoe.

------------------------------------------------------------------------

# 3.7 Jūni-hitoe --- Ultimate

Ultimate exclusiva de Hoshina + Numbers 10.

Sequência:

1.  Hoshina entra em postura;
2.  duas espadas preparadas;
3.  cauda assume terceira espada;
4.  sincronização = 100%;
5.  executar 12 golpes concentrados;
6.  grande impacto final;
7.  opcionalmente expor o núcleo do inimigo.

Sugestão: - 12 hits; - último golpe causa dano elevado; - ignora parte
da defesa; - cooldown muito longo; - não pode ser interrompido
facilmente.

Animação: `hoshina_junihitoe`

------------------------------------------------------------------------

# 4. KAIJU NO. 10 --- MOB BOSS

## Função

No. 10 deve ser um boss agressivo de combate físico.

Características:

-   força extrema;
-   grande resistência;
-   velocidade surpreendente;
-   cauda;
-   ataques de membros;
-   projéteis;
-   regeneração;
-   gigantificação;
-   inteligência.

------------------------------------------------------------------------

# 4.1 Heavy Punch

-   alto dano;
-   alto knockback;
-   pequena área de impacto;
-   quebra/destrói determinados blocos se o sistema do mod permitir.

------------------------------------------------------------------------

# 4.2 Heavy Smash

Ataque em área.

Sequência:

1.  levantar braço;
2.  telegraph;
3.  bater no chão;
4.  criar shockwave.

Efeitos: - dano em área; - knockback; - partículas; - possível
destruição de blocos configurável.

------------------------------------------------------------------------

# 4.3 Tail Attack

A cauda pode:

-   chicotear;
-   perfurar;
-   bater no chão;
-   atingir vários jogadores.

Criar variantes:

``` text
tail_sweep
tail_stab
tail_smash
```

------------------------------------------------------------------------

# 4.4 Finger Cannon

Ataque ranged.

-   projétil rápido;
-   dano alto;
-   pequena explosão;
-   cooldown médio.

------------------------------------------------------------------------

# 4.5 Multi-Appendage Assault

Criar múltiplos braços/membros.

Ataque: - vários golpes simultâneos; - área frontal ampla; - grande
pressão contra vários jogadores.

------------------------------------------------------------------------

# 4.6 Regeneration

Quando HP estiver abaixo de determinado limite:

``` text
HP < 50% → regeneração moderada
HP < 20% → regeneração forte
```

Não deixar regeneração infinita.

------------------------------------------------------------------------

# 4.7 Giant Form

Segunda fase.

Trigger: - HP abaixo de 50% ou evento específico.

Mudanças: - modelo aumenta; - escala aumenta; - dano aumenta; - alcance
aumenta; - velocidade pode diminuir ligeiramente; - knockback resistance
aumenta; - desbloqueia ataques de área.

Fortitude sugerida da fase: `9.0`

------------------------------------------------------------------------

# 4.8 Berserk Phase

Quando HP estiver muito baixo:

-   ataques mais rápidos;
-   combos maiores;
-   menor cooldown;
-   maior agressividade;
-   maior chance de usar ataques especiais.

------------------------------------------------------------------------

# 5. KAIJU NO. 9 --- BOSS ADAPTATIVO

No. 9 não deve ser apenas um mob com muito HP.

Sua principal característica é:

**inteligência + adaptação + mutação + regeneração + controle de outros
Kaijus.**

------------------------------------------------------------------------

# 5.1 AI State Machine

``` text
NO_9
 ├── IDLE
 ├── OBSERVE
 ├── COMBAT
 ├── ADAPT
 ├── MUTATE
 ├── SUMMON
 ├── CLONE
 ├── DEFEND_CORE
 ├── REGENERATE
 ├── RETREAT
 └── FINAL_FORM
```

------------------------------------------------------------------------

# 5.2 Regeneration

Sistema:

``` text
if health < 50%:
    activate regeneration

if health < 20%:
    activate fast regeneration
```

Durante regeneração: - reduzir agressividade; - procurar posição
segura; - proteger núcleo; - utilizar aliados como escudo.

------------------------------------------------------------------------

# 5.3 Body Manipulation

No. 9 pode alterar o próprio corpo.

Possibilidades:

-   criar braços;
-   criar tentáculos;
-   criar olhos;
-   criar membros defensivos;
-   aumentar massa;
-   criar estruturas de ataque.

Implementar como estados/variantes de animação, não necessariamente
trocar o modelo inteiro.

------------------------------------------------------------------------

# 5.4 Shapeshifting

Modo de infiltração.

No. 9 pode:

-   assumir aparência humana;
-   ocultar identidade;
-   reduzir efeitos visuais de Kaiju;
-   voltar para forma de combate.

Opcional para servidores: - permitir ou desativar por configuração.

------------------------------------------------------------------------

# 5.5 Finger Gun

-   longo alcance;
-   alta velocidade;
-   dano médio/alto;
-   cooldown curto/médio.

------------------------------------------------------------------------

# 5.6 Multi Finger Gun

Criar múltiplos braços e disparar em várias direções.

Modos:

``` text
focused
cone
360_degree
multi_target
```

------------------------------------------------------------------------

# 5.7 Phase Shift

No. 9 pode entrar brevemente em estado evasivo.

Durante esse estado:

-   baixa/zero colisão;
-   resistência elevada;
-   invisibilidade parcial;
-   partículas sutis.

Cooldown longo.

------------------------------------------------------------------------

# 5.8 Clones

No. 9 pode criar clones.

Limites configuráveis:

``` text
max_clones = 2–5
```

Clones: - possuem menos HP; - causam menos dano; - perseguem o alvo; -
podem disparar Finger Gun; - compartilham alvo com o original.

Quando o original muda de alvo: - clones atualizam o alvo.

------------------------------------------------------------------------

# 5.9 Flying Form

No. 9 pode criar asas.

Funções: - voo; - perseguição aérea; - retirada; - ataque aéreo; -
posicionamento para Finger Gun.

------------------------------------------------------------------------

# 5.10 Defensive Shell

Quando recebe muito dano:

-   endurecer corpo;
-   reduzir dano recebido;
-   proteger núcleo;
-   diminuir velocidade.

Pode ser ativado automaticamente.

------------------------------------------------------------------------

# 5.11 Body Hardening

Buff defensivo.

Efeitos: - redução de dano físico; - maior knockback resistance; - menor
dano de projéteis.

Duração curta.

------------------------------------------------------------------------

# 5.12 Corpse Manipulation

No. 9 pode usar corpos de Kaijus derrotados.

Possíveis usos:

-   escudo;
-   obstáculo;
-   projétil;
-   cobertura;
-   material para mutação.

------------------------------------------------------------------------

# 5.13 Kaiju Summoning

No. 9 pode invocar aliados.

Tabela sugerida:

``` text
fase 1 → Yojus
fase 2 → Yojus + Honjus
fase 3 → Honjus + elites
fase final → unidades especiais
```

Limitar quantidade simultânea.

------------------------------------------------------------------------

# 5.14 Kaiju Command

No. 9 deve conseguir dar ordens aos Kaijus aliados.

Estados:

``` text
ATTACK
DEFEND
SURROUND
HUNT
RETREAT
PROTECT_NO9
```

------------------------------------------------------------------------

# 5.15 Shared Detection

Criar uma rede simples:

Se qualquer Kaiju aliado detectar o jogador:

``` text
Kaiju A detectou jogador
        ↓
No. 9 recebe informação
        ↓
No. 9 atualiza target
        ↓
todos os aliados próximos podem reagir
```

Isso fará o No. 9 parecer um comandante.

------------------------------------------------------------------------

# 5.16 Resurrection

Opcional e configurável.

Quando um Kaiju aliado morrer:

-   No. 9 pode tentar revivê-lo;
-   cooldown longo;
-   consumo de energia;
-   quantidade máxima de ressurreições.

------------------------------------------------------------------------

# 6. NO. 9 --- ABSORÇÃO / EVOLUÇÃO

No. 9 deve ter sistema de evolução.

Atributo:

`absorption_power`

Quando absorve uma entidade especial:

-   desbloqueia novas habilidades;
-   aumenta atributos;
-   altera modelo;
-   muda IA;
-   entra em nova fase.

------------------------------------------------------------------------

# 6.1 NO. 2 / FORMA FINAL

Ao absorver o poder associado ao Kaiju No. 2:

desbloquear:

-   ataques de energia;
-   escudo energético;
-   maior força;
-   maior resistência;
-   ataques de área;
-   capacidades derivadas do conhecimento adquirido.

Fortitude sugerida:

`9.5`

------------------------------------------------------------------------

# 6.2 Energy Blast

Ataque energético de grande potência.

Características: - charge telegraph; - grande dano; - alcance longo; -
explosão/impacto; - cooldown longo.

------------------------------------------------------------------------

# 6.3 Energy Shield

Escudo energético.

Pode:

-   bloquear projéteis;
-   reduzir dano frontal;
-   proteger núcleo.

Criar escudo como hitbox/efeito separado quando possível.

------------------------------------------------------------------------

# 6.4 Final Form AI

Na fase final, No. 9 deve:

1.  observar o jogador;
2.  selecionar ataque apropriado;
3.  usar aliados;
4.  proteger o núcleo;
5.  regenerar quando necessário;
6.  mudar de forma;
7.  utilizar ataques de longo alcance;
8.  evitar ficar parado;
9.  usar clones;
10. tentar cercar o jogador.

------------------------------------------------------------------------

# 7. SISTEMA DE TELEGRAPH

Todos os ataques fortes devem possuir telegraph.

Exemplo:

``` text
0.0s — preparação
0.5s — partículas
1.0s — som
1.2s — ataque
1.3s — impacto
```

Isso permite que o jogador reaja.

Ataques muito fortes não devem aparecer instantaneamente sem aviso.

------------------------------------------------------------------------

# 8. SISTEMA DE COOLDOWN

Criar um gerenciador central:

``` text
AbilityCooldownManager
```

Exemplo:

``` text
Kūuchi: 20 ticks
Kōsa-uchi: 40 ticks
Yae-uchi: 120 ticks
Jūni-hitoe: 600 ticks
Finger Gun: 30 ticks
Regeneration: 400 ticks
Clone: 300 ticks
Energy Blast: 500 ticks
```

Os valores são sugestões e devem ser balanceados durante testes.

------------------------------------------------------------------------

# 9. SISTEMA DE PRIORIDADE DE ATAQUES

Cada habilidade deve possuir prioridade.

Exemplo Hoshina:

``` text
Ultimate         = 100
Counter          = 90
Combo Finisher   = 80
Yae-uchi         = 70
Ran-uchi         = 60
Kūuchi           = 40
Basic Attack     = 10
```

No. 9:

``` text
Protect Core     = 100
Regenerate       = 95
Energy Blast     = 90
Clone            = 75
Summon           = 70
Finger Gun       = 60
Melee            = 20
```

------------------------------------------------------------------------

# 10. SISTEMA DE DISTÂNCIA

As habilidades devem depender da distância.

``` text
0–3 blocos:
    melee

3–8 blocos:
    combo / dash

8–16 blocos:
    ranged

16+ blocos:
    movement / approach
```

No. 9 deve priorizar ataques ranged.

Hoshina deve priorizar aproximação.

No. 10 deve priorizar aproximação + área.

------------------------------------------------------------------------

# 11. SISTEMA DE NÚCLEO

Para Kaijus importantes, implementar núcleo como ponto fraco.

Estados:

``` text
CORE_HIDDEN
CORE_EXPOSED
CORE_DAMAGED
CORE_CRITICAL
```

Algumas habilidades podem:

-   expor o núcleo;
-   proteger o núcleo;
-   causar dano aumentado no núcleo.

Exemplo:

Hoshina: `Jūni-hitoe → CORE_EXPOSED`

No. 9: `Defensive Shell → CORE_PROTECTED`

------------------------------------------------------------------------

# 12. INTERAÇÃO COM DESTRUIÇÃO DO AMBIENTE

Ataques grandes podem produzir destruição configurável.

Criar configuração:

``` text
enable_environment_damage = true
environment_damage_radius = 3
max_blocks_per_attack = 50
```

Nunca destruir blocos indiscriminadamente.

Ataques de grande porte:

-   No. 10 Giant Smash;
-   No. 9 Energy Blast;
-   Hoshina Ultimate;
-   outros Daikaiju.

------------------------------------------------------------------------

# 13. GEOCKOLIB

Criar animações separadas para:

## Hoshina

``` text
idle
walk
run
dash
slash_1
slash_2
kūuchi
kosa_uchi
kaeshi_uchi
ran_uchi
kasumi_uchi
yae_uchi
parry
full_release
junihitoe
death
```

## No. 10

``` text
idle
walk
run
punch
heavy_smash
tail_attack
finger_cannon
multi_attack
regenerate
giant_transform
giant_idle
giant_attack
berserk
death
```

## No. 9

``` text
idle
walk
run
finger_gun
multi_finger_gun
mutate
clone
summon
fly
land
defend
regenerate
phase_shift
energy_charge
energy_blast
shield
final_transform
death
```

------------------------------------------------------------------------

# 14. PARTICLES

Criar efeitos próprios para:

### Hoshina

-   slash;
-   speed trail;
-   sword spark;
-   counter flash;
-   impact;
-   ultimate slash.

### No. 10

-   shockwave;
-   dust;
-   impact;
-   kaiju energy;
-   giant transformation.

### No. 9

-   mutation particles;
-   regeneration;
-   energy charge;
-   shield;
-   clone spawn;
-   teleport/phase;
-   absorption.

------------------------------------------------------------------------

# 15. SOM

Cada habilidade importante deve possuir som próprio.

Estrutura:

``` text
sounds/
 ├── hoshina/
 ├── numbers10/
 ├── kaiju9/
 └── kaiju10/
```

Evitar usar o mesmo som para todos os ataques.

------------------------------------------------------------------------

# 16. BALANCEAMENTO

Não definir apenas HP.

Cada mob deve ter identidade:

## Hoshina

-   extremamente rápido;
-   pouco HP;
-   alto DPS;
-   evasão;
-   counter;
-   combos.

## Hoshina + No. 10

-   velocidade alta;
-   DPS muito alto;
-   terceira arma;
-   defesa automática;
-   ultimate.

## No. 10

-   HP alto;
-   força alta;
-   resistência alta;
-   área de efeito;
-   transformação.

## No. 9

-   HP alto;
-   regeneração;
-   inteligência;
-   invocação;
-   clones;
-   adaptação;
-   múltiplas fases.

------------------------------------------------------------------------

# 17. CONFIGURAÇÕES DO MOD

Todas as habilidades devem poder ser configuradas.

Exemplo:

``` toml
[hoshina]
enable_advanced_techniques=true
counter_chance=0.35
dash_cooldown=20
yae_uchi_cooldown=120

[numbers10]
full_release_enabled=true
tail_ai_enabled=true
junihitoe_enabled=true
junihitoe_cooldown=600

[kaiju10]
giant_form_enabled=true
environment_damage=true
regeneration_enabled=true

[kaiju9]
clones_enabled=true
summoning_enabled=true
resurrection_enabled=true
adaptive_ai=true
environment_damage=true
```

------------------------------------------------------------------------

# 18. REGRAS IMPORTANTES PARA O CLAUDE CODE

1.  Não remover as habilidades atuais do mod sem verificar dependências.
2.  Não substituir modelos/animadores existentes sem preservar animações
    funcionais.
3.  Reutilizar sistemas existentes quando possível.
4.  Criar classes separadas para habilidades complexas.
5.  Evitar colocar toda a lógica dentro da classe principal do mob.
6.  Criar Ability/Skill Controllers.
7.  Criar Cooldown Manager.
8.  Criar State Machine.
9.  Usar GeckoLib para animações.
10. Manter nomes das animações organizados.
11. Não quebrar entidades existentes.
12. Fazer alterações incrementais.
13. Compilar depois de cada grupo importante de alterações.
14. Corrigir erros de compilação antes de avançar.
15. Não remover recursos existentes apenas para implementar uma
    habilidade.
16. Verificar a versão exata do Minecraft, loader e GeckoLib do projeto
    antes de escolher APIs.
17. Se uma habilidade não puder ser implementada exatamente como
    descrita, manter a intenção visual/mecânica e documentar a
    adaptação.
18. Criar configurações para habilidades pesadas.
19. Evitar loops de IA que causem queda de TPS.
20. Limitar quantidade de clones, summons, partículas e destruição de
    blocos.

------------------------------------------------------------------------

# 19. ORDEM RECOMENDADA DE IMPLEMENTAÇÃO

Implementar nesta ordem:

### FASE 1

Sistema geral:

-   Ability interface;
-   Cooldown Manager;
-   State Machine;
-   Target Controller.

### FASE 2

Hoshina normal:

-   slash;
-   Kūuchi;
-   Kōsa-uchi;
-   dash;
-   parry;
-   Kaeshi-uchi;
-   Ran-uchi;
-   Kasumi-uchi;
-   Yae-uchi.

### FASE 3

Numbers 10:

-   Tail Controller;
-   Tail Slash;
-   Tail Guard;
-   Third Sword;
-   Synchronization;
-   Full Release;
-   Jūni-hitoe.

### FASE 4

No. 10:

-   melee;
-   tail;
-   Finger Cannon;
-   regeneration;
-   Giant Form;
-   Berserk.

### FASE 5

No. 9:

-   regeneration;
-   body manipulation;
-   Finger Gun;
-   clones;
-   summons;
-   Kaiju Command;
-   Phase Shift;
-   Defensive Shell.

### FASE 6

No. 9 final:

-   absorption;
-   No. 2 power;
-   Energy Blast;
-   Energy Shield;
-   final AI.

### FASE 7

Polimento:

-   GeckoLib;
-   partículas;
-   sons;
-   telegraphs;
-   efeitos;
-   destruição;
-   balanceamento;
-   otimização.

------------------------------------------------------------------------

# 20. OBJETIVO FINAL

O resultado deve fazer os personagens parecerem diferentes mesmo quando
possuem HP semelhante.

**Hoshina = velocidade + técnica + precisão + counter**

**Hoshina/Numbers 10 = velocidade + três armas + sincronização +
ultimate**

**Kaiju No. 10 = força + resistência + gigantificação + combate
agressivo**

**Kaiju No. 9 = inteligência + adaptação + mutação + regeneração +
invocação + controle**

Não implementar os quatro simplesmente como entidades que caminham até o
jogador e usam um ataque básico.

A IA, as animações, os cooldowns, os telegraphs, as fases e a seleção
contextual de habilidades são parte essencial da implementação.

------------------------------------------------------------------------

# 21. NOVOS KAIJUS --- PRIMIGENIUS, TRICHONEPHILA E PREONDACTYL

Esta seção complementa a especificação anterior. Implementar os
seguintes mobs mantendo a mesma arquitetura de Ability Controller,
Cooldown Manager, State Machine, GeckoLib e sistema de telegraph.

------------------------------------------------------------------------

# 21.1 PRIMIGENIUS YOJU

## Identidade

-   Classe: Yoju
-   Fortitude de referência: 2.5
-   Função: atacante corpo a corpo + criatura sensorial.
-   Característica principal: visão extremamente limitada/ausente e
    audição extremamente desenvolvida.

## Habilidades

### Enhanced Hearing

O mob deve detectar entidades através de sons e eventos próximos.

Eventos detectáveis: - passos; - ataques; - disparos; - explosões; -
entidades correndo; - blocos quebrados.

Não dar detecção perfeita através de paredes sem que exista evento
sonoro.

### Blind / Poor Vision

A IA deve ter dificuldade para localizar um alvo que não produza som.

### Hooved Strike

Ataque corpo a corpo usando mãos/dedos com estruturas semelhantes a
cascos.

-   dano médio;
-   knockback;
-   curto cooldown.

### Tail Swipe

Golpe lateral com a cauda.

-   alcance superior ao ataque básico;
-   dano moderado;
-   knockback lateral;
-   bom contra múltiplos jogadores.

### Charge

Investida curta.

-   telegraph;
-   avanço rápido;
-   impacto;
-   knockback.

### Weak Belly

O abdômen deve possuir multiplicador de dano configurável.

Exemplo: `belly_damage_multiplier = 1.35`

### Sonic/Stun Vulnerability

Como adaptação de gameplay: - ataques sonoros fortes podem causar
stun; - durante stun, reduzir velocidade e impedir habilidades.

Esta vulnerabilidade deve ser tratada como mecânica do mod, não como um
novo ataque canônico.

------------------------------------------------------------------------

# 21.2 PRIMIGENIUS HONJU

## Identidade

-   Classe: Honju
-   Fortitude de referência: 5.4
-   Função: bruiser/tanque + atacante de energia.
-   Mantém características sensoriais do Primigenius.

## Habilidades

### Heavy Hooved Punch

-   dano alto;
-   knockback alto;
-   pequena área de impacto.

### Charge

-   corrida frontal;
-   dano alto;
-   knockback;
-   telegraph;
-   pode destruir blocos frágeis se a configuração de destruição estiver
    ativa.

### Tail Strike

-   golpe lateral;
-   alcance médio;
-   dano moderado.

### Uni-organ

Órgão especial responsável pelo ataque energético.

Criar estado:

`uni_organ_ready`

e:

`uni_organ_destroyed`

### Energy Blast

Sequência:

``` text
uni-organ charge
      ↓
energy buildup
      ↓
projectile
      ↓
explosion
```

Características: - alcance longo; - dano alto; - cooldown longo; -
grande telegraph.

### Energy Blast Cooldown

Não permitir disparo contínuo.

### Weak Points

-   núcleo;
-   abdômen;
-   Uni-organ.

Se o Uni-organ for destruído: - desabilitar Energy Blast; - iniciar
regeneração apenas quando a variante permitir.

------------------------------------------------------------------------

# 21.3 RESURRECTED PRIMIGENIUS YOJU

## Identidade

-   Fortitude de referência: 3.8
-   Variante criada pela ressurreição associada ao No. 9.
-   Visual e atributos devem ser diferentes do Yoju normal.

## Habilidades

Manter:

-   Enhanced Hearing;
-   Hooved Strike;
-   Tail Swipe;
-   Charge;
-   Poor Vision.

Adicionar:

### Revived Body

Buff permanente: - HP aumentado; - dano aumentado; - resistência
aumentada; - velocidade aumentada; - agressividade aumentada.

### No Fear

A variante ressuscitada não deve recuar facilmente.

### No. 9 Alliance

Receber automaticamente a tag:

`revived_by_no9 = true`

O No. 9 deve reconhecer a criatura como aliada.

------------------------------------------------------------------------

# 21.4 RESURRECTED PRIMIGENIUS HONJU

## Identidade

-   Fortitude de referência: 6.4
-   Variante elite ressuscitada.
-   Deve ser significativamente mais perigosa que o Honju normal.

## Habilidades

Manter:

-   Heavy Hooved Punch;
-   Charge;
-   Tail Strike;
-   Enhanced Hearing;
-   Energy Blast.

Adicionar:

### Regrown Uni-organ

O Uni-organ pode regenerar.

Estados:

``` text
ACTIVE
DESTROYED
REGENERATING
ACTIVE_AGAIN
```

### Revived Energy Blast

Após regenerar o Uni-organ: - restaurar Energy Blast; - permitir nova
sequência de ataques.

### Revival Regeneration

Quando o Uni-organ for destruído: 1. interromper Energy Blast; 2. entrar
em estado defensivo; 3. iniciar regeneração; 4. restaurar órgão; 5.
retornar ao combate.

### Revived Rage

Mecânica de gameplay:

Quando HP \< 30%: - dano aumentado; - velocidade aumentada; - cooldowns
reduzidos; - agressividade aumentada.

------------------------------------------------------------------------

# 21.5 TRICHONEPHILA

## Identidade

-   Classe: Yoju
-   Fortitude de referência: 2.5
-   Função: predador ágil/emboscador.
-   Aproximadamente 10 m.
-   Não usar altura como critério rígido para definir classe.

## Habilidades

### Super Agility

-   velocidade alta;
-   aceleração alta;
-   mudanças rápidas de direção.

### Wall Climb

Permitir: - subir paredes; - andar verticalmente; - permanecer em
paredes; - atacar a partir de superfícies verticais.

### Ceiling Position

Se existir bloco acima: - subir; - permanecer no teto; - esperar alvo.

### Building Leap

Salto entre superfícies.

### Ambush

AI:

``` text
search
 ↓
climb
 ↓
hide
 ↓
wait
 ↓
target enters range
 ↓
leap attack
```

### Leg Swipe

Golpe horizontal com as pernas.

### Leg Stab

Golpe perfurante.

### Multi-Leg Assault

Várias pernas atacam em sequência rápida.

### Bite

Ataque de curta distância.

Opcional: - pequeno efeito de bleeding; - usar somente se houver sistema
de status apropriado.

### Web Shot

Disparar teia.

Efeito: - slow; - impedir sprint; - reduzir velocidade; - duração
configurável.

### Web Trap

Criar armadilha de teia no chão.

### Web Anchor

Criar ligação entre a criatura e uma superfície.

Uso: - subir; - descer; - reposicionar; - realizar leap.

### Building Break

Permitir atravessar/danificar blocos durante uma investida.

OBRIGATÓRIO: - limite de blocos destruídos; - limite por segundo; -
whitelist de blocos destruíveis; - proteção contra destruição infinita.

### Core Weakness

O núcleo recebe dano aumentado.

------------------------------------------------------------------------

# 21.6 PREONDACTYL

## Identidade

-   Tipo: wyvern-type Kaiju.
-   Fortitude de referência: aproximadamente 6.3.
-   Função: unidade aérea ranged.
-   Pode operar sozinho.
-   Pode ser comandado pelo No. 10.

## Habilidades

### Flight

Estados:

``` text
FLY
CIRCLE
DIVE
RETREAT
LAND
ATTACK_AIR
```

### Aerial Dive

1.  ganhar altitude;
2.  mirar alvo;
3.  mergulhar;
4.  impacto;
5.  knockback.

### Bite

Ataque próximo.

### Claw Attack

Ataque com garras.

### Tail Strike

Ataque com cauda.

### Energy Beam

Ataque energético disparado pela boca.

Sequência:

``` text
MOUTH_CHARGE
 ↓
ENERGY_BUILDUP
 ↓
BEAM
 ↓
IMPACT
```

Características: - alcance muito alto; - dano alto; - telegraph forte; -
cooldown longo.

### Armored Front

Aplicar redução de dano frontal configurável.

Exemplo:

``` text
front_damage_multiplier = 0.35
```

### Weak Back

Ataques na região traseira recebem dano normal/aumentado.

Isso cria uma mecânica de posicionamento.

### Freeze Vulnerability

Projéteis/ataques de congelamento podem: - slow forte; - aplicar stun; -
interromper voo; - derrubar temporariamente o Preondactyl.

### Self Destruct

Quando HP estiver baixo ou por comando especial:

``` text
WARNING
3
2
1
EXPLOSION
```

Características: - dano em área; - grande knockback; - grande
telegraph; - destruir blocos somente se configuração permitir.

------------------------------------------------------------------------

# 21.7 PREONDACTYL FORMATION BOMB

Esta não deve ser tratada como um mob comum.

É um estado especial de múltiplos Preondactyls.

## Condições

-   múltiplos Preondactyls próximos;
-   comando do No. 10 ou condição configurável;
-   espaço suficiente para formação.

## AI

``` text
SEARCH_FORMATION
 ↓
JOIN_FORMATION
 ↓
LOCK_POSITION
 ↓
CHARGE
 ↓
FINAL_WARNING
 ↓
BOMB
```

## Formação

Os Preondactyls devem ocupar posições ao redor de um ponto central.

## Interrupção

O jogador pode: - matar um dos participantes; - aplicar stun; -
congelar; - afastar um participante; - atacar o grupo antes da
detonação.

Se a formação perder participantes: - cancelar ou reduzir a explosão,
conforme configuração.

------------------------------------------------------------------------

# 21.8 PREONDACTYL + NO. 10

No. 10 deve possuir uma habilidade de comando específica.

## Kaiju Command: Preondactyl

No. 10 pode ordenar:

-   attack;
-   surround;
-   dive;
-   energy beam;
-   retreat;
-   formation;
-   bomb.

## Comportamento

Sem No. 10: - Preondactyl age individualmente.

Com No. 10: - recebe ordens; - compartilha alvo; - pode formar grupos; -
pode realizar ataques coordenados.

Criar interface:

``` text
PreondactylCommandController
```

------------------------------------------------------------------------

# 21.9 SISTEMA DE RELAÇÃO ENTRE OS KAIJUS

## No. 9

Pode: - reconhecer Primigenius ressuscitados; - ressuscitar Primigenius
mortos, se o sistema estiver habilitado; - ordenar aliados; - marcar
aliados como parte de sua rede.

## No. 10

Pode: - comandar Preondactyls; - coordenar ataques; - ordenar
formação-bomba.

## Hoshina + Numbers 10

Pode: - detectar entidades; - usar cauda independentemente; - coordenar
ataques; - tratar a cauda como terceiro membro ofensivo/defensivo.

------------------------------------------------------------------------

# 21.10 SISTEMA DE RESSURREIÇÃO

Criar um sistema genérico:

``` text
RevivalController
```

Fluxo:

``` text
Kaiju dies
    ↓
Corpse / Remains
    ↓
No. 9 detects remains
    ↓
Check revival cooldown
    ↓
Revival animation
    ↓
New entity
    ↓
revived_by_no9 = true
```

A variante ressuscitada deve manter: - posição; - relação com o No. 9; -
identificação da espécie; - efeitos visuais próprios.

------------------------------------------------------------------------

# 21.11 SISTEMA DE FRAQUEZAS

Cada mob pode possuir pontos fracos configuráveis.

Exemplo:

``` text
Primigenius:
    belly = vulnerable

Primigenius Honju:
    belly = vulnerable
    uni_organ = critical

Trichonephila:
    core = critical

Preondactyl:
    back = vulnerable
    core = critical
```

Não fazer a fraqueza simplesmente depender de coordenada fixa. Usar
hitboxes ou bones GeckoLib quando possível.

------------------------------------------------------------------------

# 21.12 TABELA FINAL DE IMPLEMENTAÇÃO

  -----------------------------------------------------------------------
  Mob               Classe            Função            Habilidades
                                                        prioritárias
  ----------------- ----------------- ----------------- -----------------
  Primigenius Yoju  Yoju              Sensorial/melee   Hearing, Hooved
                                                        Strike, Tail,
                                                        Charge

  Primigenius Honju Honju             Bruiser/ranged    Charge, Heavy
                                                        Punch, Uni-organ,
                                                        Energy Blast

  Revived Yoju      Yoju              Elite melee       Hearing, Charge,
                                                        Revival Buff, No
                                                        Fear

  Revived Honju     Honju             Elite             Energy Blast,
                                      boss/miniboss     Regrown
                                                        Uni-organ,
                                                        Regeneration,
                                                        Rage

  Trichonephila     Yoju              Ambusher          Wall Climb, Leap,
                                                        Web, Bite, Legs,
                                                        Ambush

  Preondactyl       Honju/air unit    Flying ranged     Flight, Dive,
                                                        Energy Beam,
                                                        Claws, Self
                                                        Destruct

  Preondactyl       Special state     Raid weapon       Formation,
  Formation                                             Charge, Bomb

  Hoshina           Human/elite       Speed melee       Sword Arts,
                                                        Counter, Dash,
                                                        Parry

  Hoshina + No. 10  Numbers Weapon    Elite             Tail AI, Third
                                                        Sword, Full
                                                        Release,
                                                        Jūni-hitoe

  Kaiju No. 10      Boss              Bruiser           Heavy attacks,
                                                        Tail, Cannon,
                                                        Giant Form

  Kaiju No. 9       Boss              Adaptive          Mutation, Clones,
                                      commander         Summons,
                                                        Regeneration,
                                                        Command
  -----------------------------------------------------------------------

------------------------------------------------------------------------

# 21.13 PRIORIDADE PARA O CLAUDE CODE

Implementar os novos mobs nesta ordem:

### Primeiro

1.  Primigenius Yoju
2.  Primigenius Honju
3.  Trichonephila
4.  Preondactyl

### Depois

5.  Revived Primigenius Yoju
6.  Revived Primigenius Honju
7.  Preondactyl Formation Bomb

### Integração

8.  No. 9 → Resurrection Controller
9.  No. 9 → Kaiju Command
10. No. 10 → Preondactyl Command
11. No. 10 → Formation Bomb

### Polimento

12. GeckoLib animations
13. particles
14. sounds
15. telegraphs
16. hitboxes
17. weak points
18. environmental damage
19. performance limits
20. balance testing

------------------------------------------------------------------------

# 21.14 REGRA DE SEGURANÇA DE PERFORMANCE

Como estes mobs podem existir em grandes quantidades, nunca permitir:

-   clones ilimitados;
-   summons ilimitados;
-   partículas ilimitadas;
-   destruição ilimitada de blocos;
-   scans de entidades a cada tick para todos os mobs;
-   pathfinding pesado sem cooldown.

Preferir: - cache de alvos; - intervalos de busca; - limites de
entidades; - cooldowns; - áreas de busca configuráveis; - eventos em vez
de loops contínuos sempre que possível.

------------------------------------------------------------------------

# 21.15 REGRA FINAL

Todos os mobs devem ter uma identidade mecânica própria:

**Primigenius Yoju** = sentidos + força + ataque simples.

**Primigenius Honju** = tanque + charge + Uni-organ + energia.

**Revived Primigenius** = versões fortalecidas +
regeneração/ressurreição.

**Trichonephila** = mobilidade vertical + emboscada + teia.

**Preondactyl** = voo + beam + dive + fraqueza traseira +
autodestruição.

**Preondactyl Formation** = ataque de raid coordenado.

**Hoshina** = velocidade + espada + esquiva + contra-ataque.

**Hoshina/Numbers 10** = três armas + sincronização + cauda
independente + ultimate.

**No. 10** = força + resistência + gigantificação + comando aéreo.

**No. 9** = inteligência + mutação + regeneração + clones + invocação +
ressurreição + comando.

O objetivo é que o jogador consiga reconhecer qual criatura está
enfrentando apenas pelo seu comportamento, antes mesmo de olhar sua
barra de HP.
