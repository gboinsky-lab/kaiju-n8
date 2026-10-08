# KAIJU NO. 8 — BIBLIOTECA COMPLETA DE PERSONAGENS, KAIJUS, ARMAS, HABILIDADES E IMPLEMENTAÇÃO
## Documento mestre para Minecraft + GeckoLib + Claude Code

**Objetivo:** servir como especificação central para começar a implementar modelos, entidades, IA, ataques, animações, armas, armaduras, bosses, VFX e comportamentos do mod.

**Importante:** este documento separa:
- **CANON:** habilidade, função ou característica mostrada/estabelecida na obra.
- **ADAPTAÇÃO DO MOD:** mecânica criada para transformar a característica em gameplay de Minecraft.
- **DESIGN/IMPLEMENTAÇÃO:** instruções para modelo, bones, GeckoLib, hitboxes, IA e código.

A obra original deve ser usada como referência de linguagem corporal, função e identidade, mas as mecânicas do mod podem ser adaptadas para funcionar bem em Minecraft.

---

# 0. ESCOPO

Esta biblioteca foi ampliada para cobrir:

## Personagens jogáveis/combativos prioritários
1. Kafka Hibino / Kaiju No. 8
2. Mina Ashiro
3. Soshiro Hoshina — traje normal
4. Soshiro Hoshina — Numbers Weapon 10
5. Reno Ichikawa — traje normal
6. Reno Ichikawa — Numbers Weapon 6
7. Kikoru Shinomiya — traje normal
8. Kikoru Shinomiya — Numbers Weapon 4
9. Gen Narumi — traje normal
10. Gen Narumi — Numbers Weapon 1
11. Isao Shinomiya — traje normal
12. Isao Shinomiya — Numbers Weapon 2
13. Iharu Furuhashi
14. Haruichi Izumo
15. Aoi Kaguragi
16. Eiji Hasegawa
17. Rin Shinonome
18. Kota Tachibana
19. Jugo Ogata
20. Toko Kirimori
21. Soichiro Hoshina
22. Hikari Shinomiya
23. Jura Igarashi
24. Akari Minase
25. Hakua Igarashi
26. Ryo Ikaruga
27. Tae Nakanoshima
28. Konomi Okonogi
29. Keiji Itami
30. Juzo Nogizaka
31. Akira Kurusu

## Personagens de suporte / NPC
- Monster Sweeper Inc.: Mizoguchi, Masahide Tokuda, Ichitaka Mitsuike, Hiroto Mori
- Sagan Shinomiya
- Sebasu
- Bakko
- demais operadores e soldados secundários como variantes de NPC.

## Kaijus Numerados
- No. 1
- No. 2
- No. 3 — registro/arma conhecida, corpo completo não estabelecido de forma suficiente
- No. 4
- No. 5 — registro/arma conhecida, corpo completo não estabelecido de forma suficiente
- No. 6
- No. 7 — registro/arma conhecida, corpo completo não estabelecido de forma suficiente
- No. 8
- No. 9
- No. 10
- No. 11
- No. 12
- No. 13
- No. 14
- No. 15

## Kaijus e espécies não numerados
- Mysterious Larva
- Philinosoma
- Trichonephila
- Primigenius Honju
- Primigenius Yoju
- Primigenius Honju/Yoju ressuscitados
- Preondactyl
- Myxogasterocarp / Phaneroplasmodium
- Polyonax / Mole Type 67
- Lizard-type
- Crustacean-type
- Plant-type
- Ant-type
- Diclonius
- Anguilla Ambulans
- Maxilla Cornuta
- Lacerta Fratris
- Imitatio Mantidis
- Phaneroplus
- Supergiant-class
- Third Wave
- outras variantes genéricas.

---

# ORDEM MESTRA DE IMPLEMENTAÇÃO — VERSÃO PARA CLAUDE CODE

## OBJETIVO
Este arquivo deve ser tratado como **plano de implementação do mod**, não como uma lista para implementar tudo de uma vez.

O Claude Code deve seguir a ordem abaixo, respeitando dependências. Antes de criar qualquer sistema, deve auditar o que já existe no projeto e **reutilizar/corrigir sistemas existentes**, evitando duplicação.

### REGRA DE PRIORIDADE
**Fidelidade a Kaiju No. 8 → viabilidade dentro do Minecraft → coerência entre sistemas → estabilidade/performance → polimento.**

---

# PRIORIDADE 0 — AUDITORIA DO PROJETO E SEGURANÇA

Antes de modificar código:
- analisar arquitetura, pacotes, entidades, itens, renderers, modelos, GeckoLib, IA, HUD, VFX, áudio e sistemas já existentes;
- localizar implementações duplicadas ou antigas;
- identificar quais modelos/assets já estão prontos;
- preservar tudo que funciona;
- não substituir modelos existentes sem necessidade;
- criar um plano de migração quando um sistema precisar ser refeito;
- compilar e testar após cada grande etapa;
- evitar memory leaks, entidades duplicadas, loops de IA, destruição excessiva e problemas de sincronização servidor/cliente.

**Regra:** se o projeto já possui um sistema funcional, melhorar esse sistema em vez de criar outro concorrente.

---

# PRIORIDADE 1 — FUNDAÇÃO DO PLAYER

Implementar/corrigir primeiro o núcleo do jogador:
- atributos;
- stamina;
- força;
- velocidade;
- resistência;
- agilidade;
- progressão;
- estado de combate;
- inventário/equipamentos;
- sincronização cliente/servidor.

## Release
- jogador só pode usar Release com traje da Força de Defesa;
- começa com limite individual baixo, aproximadamente 10–20%;
- treinamento aumenta a capacidade do corpo de suportar Release;
- o limite é individual e persistente;
- cerca de 1 minuto de uso contínuo dentro da faixa segura;
- acima do limite pessoal começa a sobrecarga;
- a sobrecarga causa dano/penalidade enquanto o Release permanece no valor utilizado;
- dano de sobrecarga **não reduz automaticamente o percentual de Release**;
- 100% representa potência máxima e risco extremo.

---

# PRIORIDADE 2 — MODELOS, ARMADURAS, BONES E ANIMAÇÕES

Antes de aprofundar combate:
- integrar os modelos já existentes no mod;
- corrigir bones, pivôs, escala e renderer;
- GeckoLib para animações quando aplicável;
- animações de corpo inteiro;
- primeira, segunda e terceira pessoa quando aplicável;
- locomotion profiles diferentes por personagem/criatura;
- evitar clipping entre corpo, armadura e armas.

## Armaduras do jogador — REGRA OBRIGATÓRIA
O modelo de armadura **já existente no mod deve ser reutilizado**.

A tarefa é corrigir sua integração para que ela apareça como **armadura de corpo inteiro**, cobrindo corretamente as partes correspondentes do personagem:
- cabeça;
- tronco;
- braços;
- mãos quando o asset possuir essa parte;
- cintura;
- pernas;
- pés quando o asset possuir essa parte.

Não criar uma nova armadura nem pedir ao usuário para modelar outra.

A armadura deve:
- acompanhar todas as animações do corpo;
- respeitar escala e proporções do asset atual;
- seguir os bones correspondentes;
- não ficar presa apenas ao torso;
- não flutuar ou penetrar no corpo;
- funcionar com armas e empunhaduras;
- manter variantes de textura/equipamento;
- ser corrigida no renderer existente quando possível.

---

# PRIORIDADE 3 — SISTEMA DE ARMAS E EMPUNHADURA

Criar um `WeaponAnimationProfile` ou equivalente para cada família de armas.

Toda arma deve definir:
- mão/mãos utilizadas;
- postura;
- saque;
- guarda;
- ataque;
- recuperação;
- recarga/manuseio;
- recuo;
- inspeção quando aplicável;
- bainha/fundilho quando aplicável;
- animações em primeira/terceira pessoa;
- comportamento de NPC.

## Regras específicas
- Hoshina: empunhadura e movimentação próprias de seu estilo de espada;
- machados pesados: duas mãos quando a referência exigir;
- facas: saque, guarda, ataques rápidos, combos e movimentos de corrida/agachamento quando aplicável;
- armas de fogo: mira, disparo, recuo corporal, muzzle flash, fumaça, recuperação e recarga por etapas;
- Numbers Weapons: comportamento próprio, não simples skin/dano maior;
- NPCs devem respeitar a arma que estão segurando, sem animação genérica incompatível.

---

# PRIORIDADE 4 — COMBATE E MOVIMENTAÇÃO

Implementar:
- AttackController;
- hitboxes sincronizadas com animações;
- dano sincronizado;
- knockback;
- parry;
- esquiva;
- dash;
- combos;
- troca de alvo;
- recuperação;
- reação corporal ao impacto;
- combate de corpo inteiro.

## Parkour e mobilidade
Player:
- sprint;
- salto longo;
- vault;
- escalada curta;
- wall-slide/wall-run quando apropriado;
- wall-jump;
- ledge grab;
- queda controlada;
- rolamento;
- slide;
- dash;
- mudança rápida de direção.

Soldados comuns e especiais devem possuir perfis próprios.

Kaijus não devem usar parkour genérico: criaturas ágeis podem escalar/saltar; criaturas gigantes devem usar massa, destruição e atravessamento de obstáculos.

---

# PRIORIDADE 5 — IA DE PERSONAGENS E KAIJUS

Cada entidade deve possuir `CreatureMovementProfile`/perfil equivalente com:
- locomoção;
- aceleração;
- agilidade;
- salto;
- escalada;
- voo;
- combate;
- esquiva;
- ataque em movimento;
- inteligência;
- reação a dano;
- perseguição;
- fuga;
- interação ambiental;
- destruição.

**Não usar uma IA genérica para todos os seres.**

A IA deve considerar:
`alvo → ameaça → distância → terreno → ação apropriada → animação → hitbox → dano → VFX → áudio → recuperação`.

---

# PRIORIDADE 6 — PERSONAGENS E KAIJUS

Integrar os personagens e habilidades da biblioteca abaixo, preservando o que já existe.

Ordem sugerida dentro da biblioteca:
1. Kafka / No.8
2. Mina
3. Hoshina normal
4. Hoshina + No.10
5. Reno normal
6. Reno + No.6
7. Kikoru normal
8. Kikoru + No.4
9. Gen normal
10. Gen + No.1
11. Isao + No.2
12. Hikari
13. Iharu
14. Haruichi
15. Aoi
16. Eiji
17. Rin
18. Kota
19. Jugo
20. Toko
21. Soichiro
22. demais personagens/NPCs
23. Kaijus numerados
24. Kaijus não numerados.

Cada entidade deve manter identidade visual, postura, movimentação, ataques, fraquezas, escala e comportamento próprios.

---

# PRIORIDADE 7 — DESTRUIÇÃO E ATAQUES DE GRANDE ÁREA

Criar/reutilizar `AreaAttackController` com:
- RadialAttack;
- ConeAttack;
- ArcAttack;
- LineAttack;
- BeamAttack;
- ExplosionAttack;
- GroundImpact;
- TailSweep;
- BodyImpact.

A escala deve depender de tamanho/potência.

Kaijus grandes devem:
- quebrar árvores, paredes, casas, telhados e estruturas apropriadas;
- empurrar entidades menores fisicamente;
- não ficar presos em construções;
- destruir obstáculos quando realmente bloquearem o caminho;
- recalcular pathfinding depois da destruição.

Fluxo:
`PATH BLOCKED → CAN DESTROY? → DESTROY → RECALCULATE → CONTINUE`.

Aplicar limites de blocos por tick/segundo e outras proteções de performance.

---

# PRIORIDADE 8 — TREINAMENTO E PROGRESSÃO

Criar zona de treinamento controlada, inspirada no universo da obra:
- cidade evacuada/área de teste;
- Kaijus controlados;
- treinamento de armas;
- combate;
- esquiva/parry;
- movimentação;
- Release;
- resistência;
- força;
- velocidade;
- controle.

Treinamento deve alimentar diretamente atributos e capacidade de Release.

---

# PRIORIDADE 9 — PROVA DE ADMISSÃO

Criar prova inspirada na avaliação inicial da obra.

Não transformar em simples “mate X Kaijus”. Avaliar:
- combate;
- velocidade;
- movimentação;
- sobrevivência;
- eficiência;
- uso de armas;
- reação;
- resistência;
- objetivos secundários;
- tomada de decisão.

Pesquisar a referência da obra antes da implementação.

---

# PRIORIDADE 10 — MISSÕES E INVASÕES DINÂMICAS

Missões devem surgir como **eventos do mundo**, e não como uma lista permanente de quests disponíveis.

Invasões devem ocorrer principalmente em:
- cidades;
- construções;
- bases;
- instalações da Força de Defesa.

Tipos de base:
- pequena;
- média;
- grande;
- fortificada.

Implementar:
- composição variável;
- ondas;
- objetivos diferentes;
- defesa de setores/pontos;
- esquadrões distribuídos por ameaça;
- consequências no mundo;
- cooldown anti-repetição;
- eventos raros;
- cadeia de eventos posteriores.

---

# PRIORIDADE 11 — SQUAD, CAPITÃO E VICE-CAPITÃO

Reformular o menu Squad.

O jogador deve poder:
- entrar em esquadrão;
- receber funções;
- tornar-se Vice-Capitão;
- tornar-se Capitão;
- criar seu próprio pelotão;
- recrutar/gerenciar soldados;
- distribuir membros por setores;
- chamar reforços;
- emitir ordens;
- definir objetivos.

A hierarquia deve influenciar as invasões e operações.

---

# PRIORIDADE 12 — BOSSES E EVENTOS ESPECIAIS

Bosses devem ser fases de combate completas, não apenas inimigos com muito HP.

## Estrutura de invasão especial No.10 → No.9
1. No.10 inicia a invasão;
2. comanda Kaijus;
3. combate a Força de Defesa;
4. entra em estado crítico/é derrotado;
5. corpo permanece no campo;
6. No.9 aparece;
7. analisa o corpo;
8. absorve No.10;
9. transforma-se em forma vermelha/evoluída;
10. inicia nova fase.

A transformação deve alterar:
- IA;
- ataques;
- velocidade;
- regeneração;
- VFX;
- áudio;
- animações;
- destruição;
- padrões de alvo.

Não implementar apenas multiplicadores de HP/dano.

---

# PRIORIDADE 13 — AURA E IDENTIDADE VISUAL

Criar `CharacterAuraProfile`/equivalente.

Parâmetros:
- cor;
- cor secundária;
- intensidade;
- densidade;
- velocidade;
- raio;
- frequência de pulsação;
- efeito no chão;
- trilha de movimento;
- ataque;
- Release;
- sobrecarga;
- transformação.

A aura deve ser diferente por personagem/Kaiju.

O player também precisa de aura própria e dinâmica.

---

# PRIORIDADE 14 — ÁUDIO E MIXAGEM

O áudio deve transmitir escala através de design, não de volume excessivo.

Implementar:
- `KaijuAudioEngine`;
- passos;
- rugidos;
- ataques;
- impactos;
- destruição;
- regeneração;
- transformação;
- armas;
- ambiente;
- distância;
- reverb;
- áudio 3D;
- variações;
- sincronização com markers do GeckoLib.

## Regra de volume
- normalizar arquivos;
- evitar clipping/distorção;
- limitar volume por categoria;
- atenuar por distância;
- limitar sons simultâneos;
- evitar excesso de graves;
- aplicar ducking quando necessário;
- manter sons críticos audíveis sem tornar o jogo insuportável.

## Áudio de invasão
O som atual de invasão, se estiver alto/insuportável, deve ser **refeito**, não apenas amplificado ou repetido.

Novo áudio:
- cinematográfico;
- controlado;
- alerta/sirene em camadas;
- grave sem clipping;
- variações por gravidade;
- distância 3D;
- integração com ambiente;
- sem loop irritante;
- redução dinâmica durante combate intenso;
- volume configurável.

---

# PRIORIDADE 15 — POLIMENTO, TESTES E PERFORMANCE

Depois que os sistemas acima estiverem funcionando:
- testes de compilação;
- testes servidor/cliente;
- testes de multiplayer quando aplicável;
- teste de invasões;
- teste de bosses;
- teste de parkour;
- teste de armas;
- teste de Release/sobrecarga;
- teste de destruição;
- teste de animações;
- teste de áudio;
- limpeza de imports/código órfão;
- otimização de partículas/entidades/blocos;
- documentação das mudanças.

---

# DEPENDÊNCIAS PRINCIPAIS

`AUDITORIA → PLAYER → MODELOS/ARMADURAS → ARMAS → COMBATE/MOVIMENTO → IA → KAIJUS/PERSONAGENS → DESTRUIÇÃO → TREINAMENTO/PROGRESSÃO → ADMISSÃO → MISSÕES/INVASÕES → SQUAD → BOSSES → AURA → ÁUDIO → POLIMENTO`

Exemplos de dependência:

`Treinamento → atributos → limite corporal → Release → aura → stamina/sobrecarga → combate → VFX/áudio`

`Arma → empunhadura → animação → hitbox → dano → VFX → áudio → câmera`

`Invasão → cidade/base → setores → Squad → IA → destruição/parkour → objetivos → Boss → aliado → recompensa`

---

# REGRA DE EXECUÇÃO DO CLAUDE

Para cada etapa:
1. auditar o que já existe;
2. identificar arquivos/classes afetados;
3. explicar mentalmente dependências;
4. reutilizar código/assets existentes;
5. implementar somente a etapa atual;
6. compilar;
7. testar no jogo;
8. corrigir regressões;
9. só então avançar.

**Não implementar todas as prioridades simultaneamente.**
# 1. REGRAS GERAIS DO SISTEMA

## 1.1 Fortitude

Usar Fortitude como parâmetro de ameaça, mas não converter diretamente:

`1 Fortitude = X HP`

A Fortitude deve influenciar:
- HP;
- resistência;
- dano;
- força;
- escala;
- prioridade de alvo;
- destruição;
- tamanho;
- resistência a knockback;
- regeneração;
- nível de boss.

## 1.2 Core

Todo Kaiju deve possuir um sistema de core.

O core pode:
- ficar exposto;
- mudar de posição;
- possuir proteção;
- possuir fases;
- receber dano crítico;
- ficar vulnerável após certos ataques.

Criar:

```text
KaijuCore
CoreHealth
CorePosition
CoreProtection
CoreMultiplier
CoreExposeState
```

## 1.3 Uni-organ

Quando aplicável, implementar órgãos especiais.

Exemplos:
- órgão de energia;
- órgão de água;
- órgão de gelo;
- órgão de voo;
- órgão de disparo.

Um órgão destruído pode desativar uma habilidade específica.

---

# 2. KAFKA HIBINO / KAIJU NO. 8

## Identidade

Função: protagonista / Daikaiju aliado.

Perfil de gameplay:
- combate corpo a corpo;
- enorme força;
- velocidade;
- regeneração;
- resistência;
- detecção de Kaijus;
- golpes de impacto;
- destruição ambiental;
- evolução de estado.

## Forma humana

Armas:
- rifle;
- equipamento padrão;
- combate físico básico.

Gameplay:
- baixa capacidade comparada à forma Kaiju;
- foco em sobrevivência e suporte.

## Forma Kaiju No. 8

### Habilidades

**Super Strength**
- golpes extremamente fortes;
- quebra estruturas;
- knockback alto.

**Super Speed**
- corrida rápida;
- dash;
- perseguição.

**Regeneration**
- recuperação de HP;
- regeneração mais rápida fora do combate ou mediante cooldown.

**Kaiju Detection**
- detecta Kaijus próximos;
- mostra direção/distância em HUD quando configurado.

**Roar**
- dano/knockback em área.

**Ground Smash**
- ataque de impacto;
- crateras;
- partículas;
- knockback.

**Heavy Punch**
- hitbox de braço;
- destruição;
- dano elevado.

**Grab/Throw**
- agarrar inimigo compatível;
- arremessar;
- dano de impacto.

### Adaptação

Criar estados:

```text
IDLE
COMBAT
CHASE
HEAVY_ATTACK
RAGE
REGENERATE
ROAR
LOW_HEALTH
```

### Animação

O soco deve envolver:
quadril → tronco → ombro → braço → cotovelo → punho.

Nunca animar somente o braço.

---

# 3. MINA ASHIRO

## Função

Atiradora de longa distância / anti-Daikaiju / comandante.

Mina é especializada em destruir Kaijus gigantes com armamento pesado e possui altíssima capacidade de combate liberado.

## Equipamentos

- traje da Força de Defesa;
- rifle;
- pistola;
- visor;
- canhão pessoal;
- Anti-Giant-Class Kaiju Rail Gun.

## Habilidades

### Precision Shot
Tiro altamente preciso.

### Charged Cannon
Carregamento antes de disparar.

### Core Shot
Prioriza o core.

### Weak Point Shot
Caso um weak point seja conhecido, recebe multiplicador.

### Piercing Shot
Perfura múltiplos alvos.

### Anti-Giant Shot
Ataque de altíssimo dano contra Daikaiju.

### Commander Mark
Marca um Kaiju para o esquadrão.

### Bakko Assist
Bakko ajuda a estabilizar o tiro.

## IA

Mina deve evitar combate corpo a corpo.

Prioridade:
1. Daikaiju;
2. Kaiju Numerado;
3. alvo marcado;
4. ameaça ao esquadrão.

Deve procurar:
- posição elevada;
- linha de visão;
- distância segura.

## Animações

- levantar canhão;
- estabilizar;
- mirar;
- carregar;
- disparar;
- recuo;
- recuperação.

O disparo precisa deslocar o corpo.

---

# 4. SOSHIRO HOSHINA — TRAJE NORMAL

## Função

Especialista de curta distância.

## Armas

- duas espadas;
- katana em configurações específicas.

## Técnicas para implementação

### Kūuchi
Corte rápido com alcance estendido.

### Kōsa-uchi
Corte cruzado.

### Kaeshi-uchi
Contra-ataque.

### Ran-uchi
Barrage de cortes.

### Kasumi-uchi
Feinte + mudança de direção.

### Yae-uchi
Sequência de múltiplos golpes.

### Single Blade Style
Modo de uma espada.

## IA

Hoshina deve:
- entrar rapidamente no alcance;
- circular o inimigo;
- atacar pontos vulneráveis;
- esquivar;
- contra-atacar;
- evitar ficar parado na frente do inimigo.

---

# 5. HOSHINA + NUMBERS WEAPON 10

## Identidade

Não tratar como simples upgrade de dano.

É:

**Hoshina + uma segunda inteligência de combate.**

## Recursos

- 100% de combate liberado;
- cauda;
- consciência do No. 10;
- terceiro ponto de ataque;
- maior força;
- maior resistência;
- maior velocidade;
- ataques sincronizados.

## Cauda

A cauda deve possuir IA própria.

Estados:

```text
TAIL_IDLE
TAIL_GUARD
TAIL_ATTACK
TAIL_COUNTER
TAIL_HOLD_WEAPON
TAIL_PROTECT
TAIL_SYNC
```

A cauda pode:
- atacar;
- bloquear;
- proteger Hoshina;
- segurar arma;
- criar ataques combinados.

## Sincronização

```text
0-30% = controle básico
30-60% = ataques auxiliares
60-80% = combos
80-99% = Full Release
100% = sincronização máxima
```

## Jūni-hitoe

Ultimate.

Representar:
- múltiplos golpes;
- espadas + cauda;
- grande mobilidade;
- abertura do core;
- impacto final.

Não deve ser um one-shot automático.

---

# 6. RENO ICHIKAWA — TRAJE NORMAL

Função:
- rifle;
- suporte;
- combate intermediário;
- munição congelante.

Habilidades:
- Burst Fire;
- Freeze Round;
- Suppression;
- Precision Shot;
- Tactical Retreat.

---

# 7. RENO + NUMBERS WEAPON 6

## Identidade

Especialista em criocinese.

## Habilidades

### Ice Generation
Criar gelo.

### Ice Wall
Parede defensiva.

### Ice Spear
Projétil perfurante.

### Ice Field
Área congelante.

### Freeze Burst
Explosão de gelo.

### Multi-Target Freeze
Congela vários alvos.

### Ice Armor
Aumenta defesa.

### Auxiliary Cannons
Disparos de suporte.

## Risco

Sincronização excessiva pode causar:
- sobrecarga;
- perda de controle;
- congelamento;
- exaustão.

---

# 8. KIKORU SHINOMIYA — TRAJE NORMAL

Especialista:
- força;
- mobilidade;
- machado;
- combate agressivo.

## Habilidades

- Axe Slash;
- Heavy Swing;
- Shockwave;
- Dash Strike;
- Ground Smash;
- Guard Break.

---

# 9. KIKORU + NUMBERS WEAPON 4

## Identidade

Mobilidade aérea + enorme velocidade.

## Habilidades

### Flight
Voo.

### Aerial Dash
Dash aéreo.

### Dive Strike
Ataque em mergulho.

### High-Speed Axe
Golpes muito rápidos.

### Air Combo
Combos no ar.

### Aerial Evasion
Esquiva aérea.

### Vertical Assault
Ataque de cima para baixo.

O modelo precisa de bones para:
- asas/estruturas de voo;
- quadril;
- tronco;
- pernas;
- braços;
- machado.

---

# 10. GEN NARUMI — TRAJE NORMAL

Função:
- comandante;
- atirador;
- combate híbrido.

---

# 11. GEN + NUMBERS WEAPON 1

## Identidade

Predição de movimentos / percepção.

## Habilidades

### Pseudo-Foresight
Ler sinais do corpo.

### Weak Point Vision
Detectar weak points.

### Movement Prediction
Prever trajetória.

### Multi-Target Prediction
Analisar vários inimigos.

### Enhanced Vision
Percepção de:
- sinais;
- temperatura;
- movimento;
- terreno.

### Bayonet Combat
Combate próximo.

### Rifle Fire
Longo alcance.

## Adaptação

O sistema não deve literalmente prever o futuro.

Deve calcular:
- direção;
- velocidade;
- intenção de ataque;
- trajetória provável.

E permitir esquiva/contra-ataque.

---

# 12. ISAO SHINOMIYA

## Identidade

Veterano lendário / mestre de combate.

## Numbers Weapon 2

Recursos:
- força enorme;
- energia;
- ondas de choque;
- defesa;
- ataques de grande área.

## Habilidades

### Sonic Burst
Onda de energia.

### Main Burst
Ataque máximo.

### Energy Gauntlet
Golpe energético.

### Defensive Shield
Barreira.

### Heavy Strike
Golpe de altíssimo impacto.

### Combat Technique
Combate corpo a corpo avançado.

---

# 13. HIKARI SHINOMIYA

Função:
- antiga usuária da Numbers Weapon 4;
- capitã;
- especialista de alta mobilidade.

Implementar como:
- versão histórica;
- versão boss/ally;
- IA aérea;
- machado/arma pesada;
- alta velocidade.

---

# 14. IHARU FURUHASHI

## Identidade

Combatente explosivo / Flash Adapter.

A habilidade de Iharu deve ser implementada como picos de potência.

```text
NORMAL
↓
SURGE
↓
BURST
↓
RECOVERY
```

## Habilidades

### Flash Burst
Aumento instantâneo de potência.

### Burst Dash
Avanço explosivo.

### Heavy Rifle
Tiro poderoso.

### Emergency Surge
Pico de força quando aliado está em perigo.

## IA

Iharu deve ser agressivo e competitivo, mas ainda cooperativo.

---

# 15. HARUICHI IZUMO

Função:
- suporte ofensivo;
- armas tecnológicas;
- combate à distância.

Implementar:
- rifle;
- bow quando aplicável;
- tiro carregado;
- tiro múltiplo;
- marcação de alvo;
- suporte de esquadrão.

---

# 16. AOI KAGURAGI

Função:
- soldado pesado;
- rifle;
- combate de linha.

Equipamentos:
- rifle;
- pile driver.

Habilidades:
- Rifle Burst;
- Heavy Strike;
- Pile Driver;
- Guard Break;
- Charge.

Aoi deve ter mais força física que um soldado comum.

---

# 17. EIJI HASEGAWA

Função:
- vice-capitão;
- tanque;
- líder de campo.

Criar:
- Heavy Guard;
- Shield/Armor behavior;
- Counter;
- Command Ally;
- Heavy Strike.

Prioridade:
proteger aliados e manter formação.

---

# 18. RIN SHINONOME

Função:
- artilharia automática.

Arma:
- machine gun/minigun.

Habilidades:
- Suppressive Fire;
- Burst;
- Cover Fire;
- Target Mark;
- Emergency Retreat.

---

# 19. KOTA TACHIBANA

Função:
- líder de pelotão;
- shotgun;
- combate próximo/intermediário.

Habilidades:
- Shotgun Burst;
- Close Shot;
- Stagger Shot;
- Cover Command.

---

# 20. JUGO OGATA

Função:
- capitão;
- mentor;
- combatente experiente.

Criar:
- rifle;
- melee;
- command buff;
- squad coordination;
- tactical retreat;
- counter.

---

# 21. TOKO KIRIMORI

Função:
- vice-capitã;
- combate e liderança.

Criar:
- rifle;
- command;
- defensive support;
- squad buff.

---

# 22. SOICHIRO HOSHINA

Especialista:
- espada única;
- estilo Hoshina;
- combate técnico;
- rival/mentor de Soshiro.

Criar:
- Single Blade Style;
- counter;
- precision slash;
- iai-style attack;
- dash slash.

Se a versão com arma numerada baseada no No. 12 for implementada, tratá-la como variante separada e opcional.

---

# 23. JURA IGARASHI

Função:
- capitã da Segunda Divisão.

Implementar como:
- capitã de alto nível;
- rifle;
- liderança;
- combate padrão avançado.

---

# 24. AKARI MINASE / HAKUA IGARASHI / RYO IKARUGA / TAE NAKANOSHIMA

Esses personagens devem receber:
- modelos próprios;
- trajes;
- armas;
- animações;
- IA de soldado de elite;
- funções de pelotão.

Quando não houver repertório de técnicas individuais detalhado no canon, não inventar habilidades apresentadas como canônicas. Criar somente adaptações de gameplay identificadas como MOD.

---

# 25. KONOMI OKONOGI

Não é combatente principal.

Função:
- operadora;
- suporte tático.

Gameplay:
- detectar Kaijus;
- monitorar energia;
- revelar targets;
- fornecer informações;
- remover limitadores;
- buffs temporários.

Criar NPC de comando.

---

# 26. KEIJI ITAMI

Função:
- diretor;
- comando estratégico.

Gameplay:
- NPC;
- missões;
- ordens;
- buffs;
- decisões de emergência.

---

# 27. JUZO NOGIZAKA

Função:
- alto comando;
- NPC militar;
- estratégia.

Não criar habilidades de combate exageradas sem base canônica.

---

# 28. MONSTER SWEEPER INC.

## Mizoguchi
NPC supervisor.

## Masahide Tokuda
NPC trabalhador/aliado.

## Ichitaka Mitsuike
NPC trabalhador.

## Hiroto Mori
NPC trabalhador.

Implementar:
- limpeza de carcaças;
- coleta;
- transporte;
- diálogos;
- missões.

Não precisam ser combatentes de alto nível.

---

# 29. SAGAN SHINOMIYA / SEBASU

NPCs da família Shinomiya.

Implementar:
- residência;
- diálogos;
- quests;
- proteção;
- cenas.

---

# 30. BAKKO

Animal/companheiro de Mina.

Função:
- estabilização;
- assistência;
- companhia;
- reconhecimento.

Não transformar em Kaiju combatente de alto nível.

---

# 31. KAIJU NO. 1

## Identidade

Kaiju associado à percepção e previsão.

## Habilidades

- pseudo-foresight;
- leitura de sinais;
- weak-point detection;
- movement prediction;
- percepção extrema.

## Numbers Weapon 1

Essas propriedades devem ser transferidas para Gen.

---

# 33. KAIJU NO. 9 — FORMAS E VARIAÇÕES

## Regra de implementação
Cada forma do No. 9 deve ser uma identidade de combate separada. Não tratar formas como simples troca de textura ou multiplicador de HP/dano. Cada forma deve possuir modelo/anatomia, habilidades, ataques, IA, movimentação, regeneração, resistência, VFX, áudio e animações próprios.

**Separar claramente:** CANON, ADAPTAÇÃO DO MOD e FORMA ORIGINAL DO MOD.

### 33.1 No. 9 — Forma Preta Original
Esta é a **primeira versão preta do No. 9**, anterior à forma relacionada à absorção do Isao Shinomiya/Numbers Weapon 2. Não misturar as duas formas.

Implementar como fase própria com:
- anatomia e silhueta específicas da primeira forma preta;
- regeneração;
- mutação corporal;
- criação/manipulação de membros;
- ataques à distância;
- Finger Gun e variações quando aplicável;
- defesa/endurecimento;
- evasão e movimentação adaptativa;
- inteligência elevada e análise do adversário;
- criação/comando de Kaijus quando aplicável;
- estados de IA próprios;
- animações e VFX próprios.

### 33.2 No. 9 — Forma Fundida ao No. 10 — ORIGINAL DO MOD
**Esta fusão é uma criação do mod e não deve ser apresentada como acontecimento canônico.**

O No. 9 absorve/funde-se ao No. 10 e incorpora características de combate dele. A transformação deve produzir mudança real de anatomia e gameplay.

Características a implementar:
- integração da anatomia/características do No. 10 ao corpo do No. 9;
- força e resistência superiores;
- cauda/appendages derivados do No. 10;
- ataques de cauda;
- projéteis e ataques de membros;
- combate corpo a corpo agressivo;
- capacidade de comando de Kaijus;
- regeneração aprimorada;
- combinação entre inteligência/adaptação do No. 9 e agressividade do No. 10;
- novos combos e ataques híbridos;
- IA específica de fusão;
- novos padrões de alvo e fases de boss;
- VFX, aura, sons e animações exclusivos.

A fusão não deve ser apenas `No9 + dano do No10`. Ela deve alterar a forma como o boss pensa, se movimenta e luta.

### 33.3 No. 9 — Forma Fundida à Formiga — ORIGINAL DO MOD
**Forma original criada para o mod.**

O No. 9 funde-se a uma criatura do tipo formiga, incorporando características biológicas e locomotoras do hospedeiro.

Implementar:
- anatomia híbrida No. 9 + formiga;
- múltiplos membros/apoios conforme o design final;
- maior aderência e mobilidade em superfícies;
- escalada;
- deslocamento rápido;
- ataques com membros;
- mordida/mandíbula quando compatível com o modelo;
- capacidade de atravessar/usar terrenos de forma diferente;
- regeneração e mutação do No. 9;
- adaptação da IA à locomoção de formiga;
- comportamento de perseguição e cerco;
- ataques híbridos exclusivos;
- VFX, áudio, aura e animações próprios.

A forma deve parecer uma fusão biológica coerente, não uma formiga com a textura do No. 9.

### 33.4 No. 9 — Forma Vermelha/Evoluída após absorção do No. 10
Esta forma permanece separada da forma preta original e da forma de fusão direta usada como variante do mod.

Quando usada no evento especial No.10 → No.9:
- alterar IA;
- alterar velocidade;
- alterar ataques;
- alterar regeneração;
- alterar defesa;
- alterar destruição;
- alterar animações;
- alterar VFX/aura;
- alterar áudio;
- criar novos padrões de boss.

### 33.5 No. 9 — Forma após absorção do Isao Shinomiya / Numbers Weapon 2
Esta é uma forma distinta e posterior. **Não confundir com a primeira forma preta.**

Deve incorporar características associadas ao No. 2/Isao, incluindo:
- enorme força;
- energia/ondas de choque;
- ataques de grande área;
- defesa reforçada;
- técnicas híbridas No. 9 + No. 2;
- maior Fortitude;
- adaptação avançada;
- regeneração;
- novas fases de boss.

### 33.6 Forma Final/Adaptativa do No. 9
Reservada para a evolução máxima definida pelo mod. Deve reunir somente habilidades que façam sentido para a progressão estabelecida, evitando simplesmente acumular todas as habilidades de todas as formas.

## Máquina de estados do No. 9
Cada forma deve possuir estados compatíveis, podendo incluir:
`IDLE → OBSERVE → COMBAT → ADAPT → MUTATE → SUMMON → CLONE → DEFEND_CORE → REGENERATE → RETREAT → TRANSFORM → FINAL_FORM`

A máquina de estados deve ser modificada por forma. A fusão com outra criatura deve mudar decisões de combate e não apenas atributos numéricos.

## Transições
As transformações devem possuir:
- animação de transição;
- alteração progressiva do modelo quando aplicável;
- VFX;
- áudio;
- alteração de aura;
- mudança de comportamento;
- janela de vulnerabilidade quando fizer sentido;
- sincronização servidor/cliente;
- atualização de hitboxes;
- atualização de ataques e cooldowns.

# 32. KAIJU NO. 2

## Identidade

Daikaiju extremamente destrutivo.

## Habilidades

- força;
- energia;
- ondas sônicas;
# SISTEMA DE ARMADURAS DO JOGADOR — MODELO DE CORPO INTEIRO

## Regra obrigatória
As armaduras utilizáveis pelo jogador devem aparecer como **armadura de corpo inteiro**, cobrindo visualmente todas as partes correspondentes do personagem. O modelo da armadura **já existente no mod deve ser preservado e reutilizado**; não criar um novo modelo nem substituir o asset existente sem necessidade.

## Implementação
- localizar o modelo/asset de armadura já existente no projeto;
- corrigir o renderer/equipamento para que o modelo seja aplicado ao corpo inteiro do jogador;
- garantir cobertura correta de cabeça, tronco, braços, mãos quando o modelo possuir essa parte, cintura, pernas e pés;
- respeitar a escala, pivôs e proporções do modelo existente;
- mapear corretamente os bones da armadura aos bones correspondentes do jogador/GeckoLib;
- acompanhar animações de caminhada, corrida, salto, queda, agachamento, combate e outras animações de corpo inteiro;
- impedir que partes do corpo atravessem visualmente a armadura quando deveriam estar ocultas;
- evitar armadura flutuando, desalinhada, enterrada no corpo ou presa apenas ao torso;
- manter a armadura sincronizada com primeira, segunda e terceira pessoa quando aplicável;
- verificar mãos/braços durante empunhadura de armas para evitar clipping;
- manter variantes de textura e equipamentos compatíveis;
- não duplicar o sistema de armadura caso já exista um renderer funcional;
- primeiro auditar o código atual e corrigir o sistema existente.

## Regra visual
A armadura deve parecer realmente **vestida pelo personagem**, e não uma peça flutuante sobre o torso. Todas as partes devem acompanhar os movimentos do corpo de forma natural.

## GeckoLib
Quando a armadura usar GeckoLib, criar/montar uma hierarquia de bones compatível com o esqueleto do personagem e garantir que as animações movimentem a armadura junto com o corpo. Se o asset atual já possuir bones, reutilizá-los em vez de recriar o modelo.

## Importante
**Não pedir ao usuário para modelar outra armadura. O modelo já existe no mod. A tarefa é corrigir a integração/renderização para que a armadura existente cubra o corpo inteiro do jogador.**

---


# REGRAS FINAIS DE COERÊNCIA

- Não criar habilidades apresentadas como canônicas quando não houver base suficiente.
- Distinguir sempre CANON, ADAPTAÇÃO DO MOD e IMPLEMENTAÇÃO.
- Não substituir assets já existentes sem necessidade.
- Nenhum sistema deve apagar funcionalidades válidas já presentes.
- Toda habilidade deve sincronizar animação, hitbox, dano, VFX e áudio quando aplicável.
- O Minecraft deve continuar reconhecível como Minecraft; adaptar a experiência sem perder a identidade do universo.
- Performance e estabilidade são requisitos obrigatórios, não etapas opcionais.

# INSTRUÇÃO FINAL

Este documento é a especificação consolidada para implementação no Claude Code. A ordem de execução no início do documento deve ser seguida antes de usar a biblioteca detalhada de personagens, Kaijus, armas e sistemas.
