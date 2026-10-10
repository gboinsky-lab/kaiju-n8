# KAIJU NO. 8 --- BIBLIOTECA DE HABILIDADES, PODERES, ATAQUES E COMPORTAMENTOS DOS KAIJUS

**Versão:** 1.0\
**Data:** 9 de outubro de 2026\
**Projeto:** Kaiju N8 Reborn --- Minecraft / GeckoLib / Claude Code

## Objetivo

Este documento reúne exclusivamente a especificação relacionada aos
Kaijus: habilidades, poderes, ataques, sistemas, IA, comportamentos,
fraquezas, órgãos especiais, chefes, fases e destruição ambiental. Não
inclui fichas de habilidades de personagens humanos, soldados ou
animações individuais.

É uma extração organizada da
`Kaiju_N8_Biblioteca_Completa_Final_v20.md`, com instruções técnicas de
integração para o mod. Onde a biblioteca de origem não detalha uma
habilidade de uma espécie, isso é indicado como lacuna a pesquisar, em
vez de inventar uma habilidade e apresentá-la como canônica.

## 1. Regras para Claude Code

1.  Antes de implementar qualquer coisa, auditar as entidades Kaiju,
    poderes, ataques, controladores, IA, hitboxes, VFX, áudio e sistemas
    já existentes.
2.  Reutilizar e corrigir sistemas funcionais; não criar sistemas
    concorrentes nem apagar funcionalidades válidas.
3.  Para cada poder, separar:
    -   **CANON:** habilidade sustentada por referência da obra.
    -   **ADAPTAÇÃO DO MOD:** tradução de uma habilidade para uma
        mecânica de Minecraft.
    -   **MOD ORIGINAL:** habilidade inventada para o mod.
    -   **PENDENTE DE REFERÊNCIA:** falta confirmação suficiente.
4.  Não presumir que todos os Kaijus têm o mesmo repertório, anatomia,
    inteligência ou padrão de ataque.
5.  Não converter Fortitude diretamente em HP por uma fórmula fixa.
    Fortitude é um indicador de ameaça que influencia diversos
    atributos.
6.  Cada habilidade deve conectar, quando aplicável, decisão de IA,
    animação, hitbox, dano, VFX, áudio, cooldown, recuperação e efeitos
    no cenário.
7.  Não modificar arquivos, balanceamento ou código sem a autorização da
    etapa de trabalho atual. Este documento é uma especificação, não
    autorização para implementar tudo imediatamente.
8.  Não criar poderes canônicos para Kaijus cuja forma ou repertório não
    esteja suficientemente documentado. Marcar lacunas e pedir
    referências.
9.  Manter o registro do que já existe, do que foi corrigido e do que
    ainda falta.
10. Implementar por etapas e testar cada entidade no Minecraft.

------------------------------------------------------------------------

## 2. Sistema global de ameaça: Fortitude

Fortitude é um parâmetro de ameaça. **Não usar uma conversão direta como
`1 Fortitude = X HP`.**

A Fortitude deve influenciar de maneira coerente: - HP e resistência; -
dano e força; - escala e tamanho; - prioridade de alvo; - capacidade de
destruição; - resistência a knockback; - regeneração; - nível/fase de
boss; - comportamento da IA e urgência das respostas.

A fórmula exata de cada atributo deve respeitar o sistema já existente
no projeto. Não substituir balanceamento atual sem auditoria e
autorização.

## 3. Sistema de Core

Todo Kaiju deve possuir um sistema de core, com diferenças específicas
conforme a espécie e o modelo.

O core pode: - ficar exposto ou oculto; - mudar de posição; - ter
camadas de proteção; - possuir fases de vulnerabilidade; - receber dano
crítico; - ficar exposto após certos ataques ou estados; - ter efeitos
diferentes quando danificado.

Estrutura conceitual sugerida:

``` text
KaijuCore
CoreHealth
CorePosition
CoreProtection
CoreMultiplier
CoreExposeState
```

### Comportamentos do core

-   A IA pode proteger a região do core quando estiver sob ameaça.
-   Alguns ataques ou estados podem abrir uma janela de vulnerabilidade.
-   O dano ao core deve ser distinto do dano ao corpo quando a mecânica
    estiver implementada.
-   VFX e áudio devem comunicar exposição, dano crítico e destruição.
-   O sistema precisa respeitar a anatomia e o design da entidade; não
    posicionar o core num ponto genérico para todos.

## 4. Sistema de Uni-organ / órgãos especiais

Quando aplicável, um Kaiju pode ter órgãos especiais responsáveis por
uma capacidade.

Exemplos previstos na biblioteca: - órgão de energia; - órgão de água; -
órgão de gelo; - órgão de voo; - órgão de disparo.

Se um órgão for destruído, pode desativar ou limitar a habilidade
correspondente. Isso precisa ser específico da espécie e não aplicado
indiscriminadamente a todos os Kaijus.

Requisitos: - associar cada órgão à habilidade que ele sustenta; -
determinar como ele pode ser danificado; - comunicar o dano
visualmente; - atualizar a IA após a perda da função; - não remover
habilidades sem que exista uma relação definida entre órgão e
habilidade.

------------------------------------------------------------------------

## 5. Kaiju No. 8 --- poderes e comportamento

### Identidade de gameplay

Protagonista e Daikaiju aliado. Perfil centrado em combate corpo a
corpo, força enorme, velocidade, regeneração, resistência, detecção de
Kaijus, golpes de impacto e destruição ambiental.

### Habilidades listadas na biblioteca

**Super Strength** - Golpes extremamente fortes. - Quebra de
estruturas. - Knockback elevado.

**Super Speed** - Corrida rápida. - Dash. - Perseguição de alvos.

**Regeneration** - Recuperação de HP. - A biblioteca propõe regeneração
mais rápida fora do combate ou por cooldown; tratar o detalhe como
adaptação de gameplay.

**Kaiju Detection** - Detecta Kaijus próximos. - Pode fornecer direção e
distância no HUD quando configurado.

**Roar** - Dano e/ou knockback em área, conforme implementação.

**Ground Smash** - Ataque de impacto. - Crateras, partículas e
knockback.

**Heavy Punch** - Hitbox associada ao braço. - Destruição ambiental. -
Dano elevado.

**Grab / Throw** - Agarrar inimigo compatível. - Arremessar. - Causar
dano de impacto.

### Estados conceituais de IA

``` text
IDLE
COMBAT
CHASE
HEAVY_ATTACK
RAGE
REGENERATE
ROAR
LOW_HEALTH
```

Esses estados são um esquema proposto na biblioteca de origem. Antes de
adicioná-los, verificar se o projeto já possui estados equivalentes.

### Regras de combate

-   Ataques de impacto precisam ter hitbox coerente com o membro que
    ataca.
-   O soco deve usar cadeia corporal: quadril → tronco → ombro → braço →
    cotovelo → punho. Não animar apenas o braço.
-   A escala de destruição deve corresponder ao tamanho e à força da
    entidade.
-   A detecção de Kaijus deve respeitar alcance, direção e configuração
    do HUD.
-   A regeneração não deve tornar o combate interminável; cooldowns e
    condições devem respeitar o balanceamento existente.

------------------------------------------------------------------------

## 6. Kaiju No. 1 --- percepção e previsão

### Identidade

Kaiju associado à percepção e previsão.

### Habilidades listadas

-   pseudo-foresight;
-   leitura de sinais;
-   detecção de pontos fracos;
-   previsão de movimento;
-   percepção extrema.

### Relação com Numbers Weapon No. 1

A biblioteca de origem indica que essas propriedades devem ser
transferidas para Gen Narumi/Numbers Weapon No. 1. Este documento mantém
o registro apenas do lado relacionado ao Kaiju e à origem do poder.

### Adaptação técnica

-   Não implementar previsão literal do futuro.
-   Interpretar sinais do combate para estimar intenção de ataque,
    trajetória ou vulnerabilidade, se o sistema correspondente existir.
-   Diferenciar capacidade original do Kaiju da adaptação da arma e do
    usuário.
-   A biblioteca de origem não detalha aqui uma lista completa de
    ataques físicos próprios do corpo do No. 1; não inventá-la como
    canônica.

------------------------------------------------------------------------

## 7. Kaiju No. 2 --- destruição e energia

### Identidade

Daikaiju extremamente destrutivo.

### Habilidades listadas

-   força;
-   energia;
-   ondas sônicas;
-   interação ambiental;
-   destruição.

### Requisitos de implementação

-   A onda sônica deve ter alcance, forma de área, impacto e resposta
    visual/sonora definidos pelo sistema de combate existente.
-   A destruição ambiental deve considerar os tipos de bloco e a escala
    do ataque.
-   Não criar uma IA genérica para o No. 2; os seus padrões devem
    refletir sua função destrutiva.
-   A biblioteca de origem não especifica nesta seção o catálogo
    completo de golpes individuais. Marcar técnicas adicionais como
    pendentes de pesquisa antes de declará-las canônicas.

------------------------------------------------------------------------

## 8. Kaiju No. 9 --- repertório e evolução

A biblioteca mestre lista o No. 9 como Kaiju numerado e inclui o seu
papel na sequência de invasão especial com o No. 10: observar o corpo,
analisar e absorver o No. 10, transformando-se numa forma
vermelha/evoluída. Também orienta que a transformação altere IA,
ataques, velocidade, regeneração, VFX, áudio, animações, destruição e
padrões de alvo.

A versão fonte v20 consultada não apresenta, na seção de habilidades
específica do No. 9, uma ficha completa e individual de todos os seus
ataques. Por isso, não tratar uma lista ampliada de poderes como se
estivesse integralmente confirmada por esta fonte.

### Comportamentos de fase confirmados no documento mestre

1.  Entrada do No. 9 após o estado crítico/derrota do No. 10.
2.  Análise do corpo do No. 10.
3.  Absorção.
4.  Transformação para a forma vermelha/evoluída.
5.  Início de uma nova fase de combate.
6.  Alteração de IA, ataques, velocidade, regeneração, VFX, áudio,
    animações, destruição e seleção de alvos.

### Requisitos técnicos

-   Implementar como sequência de fases, não apenas aumento de HP ou
    dano.
-   A transição deve ser legível e coordenar o modelo/estado com a IA.
-   Verificar se já existe sistema de fases antes de criar outro.
-   Registrar separadamente as habilidades do No. 9 em cada forma, após
    verificar referências e os dados já presentes no projeto.

------------------------------------------------------------------------

## 9. Kaiju No. 10 --- invasão e relação com o No. 9

O documento mestre descreve a seguinte estrutura de invasão especial:

1.  O No. 10 inicia a invasão.
2.  Comanda Kaijus.
3.  Combate a Força de Defesa.
4.  Entra em estado crítico ou é derrotado.
5.  O corpo permanece no campo.
6.  O No. 9 aparece.
7.  O No. 9 analisa o corpo.
8.  O No. 9 absorve o No. 10.
9.  O No. 9 transforma-se numa forma vermelha/evoluída.
10. Inicia-se uma nova fase.

### Requisitos de sistema

-   O No. 10 deve participar da sequência de invasão e comando de Kaijus
    se essa mecânica estiver implementada.
-   A transição após derrota deve manter o corpo no campo quando
    necessário para o evento.
-   A lógica de evento precisa impedir que o corpo seja removido antes
    da absorção.
-   A nova fase deve ativar seus próprios estados, ataques, padrões de
    alvo, VFX, áudio e parâmetros.
-   A lista de ataques individuais do No. 10 deve ser confirmada nas
    referências da obra e nos dados do projeto; não inventar golpes
    canônicos a partir do evento narrativo.

------------------------------------------------------------------------

## 10. Fusão conceitual No. 9 + No. 10

A fusão No. 9 + No. 10 é um **conceito original do mod**, não uma forma
canônica confirmada da obra.

### Princípios

-   Separar visualmente o que é herdado de cada Kaiju e o que é criação
    original.
-   Não copiar um ataque existente e apenas renomeá-lo para fingir que é
    uma habilidade nova.
-   Definir forma, anatomia, core, órgãos especiais, poderes, ataques,
    IA e fases antes de implementar.
-   Reutilizar sistemas existentes de ataques, área, VFX, áudio,
    destruição e IA quando apropriado.
-   Marcar todo poder inventado como `MOD ORIGINAL`.

### Pacote de sistemas que precisa ser especificado antes da implementação

-   estado de repouso e alerta;
-   locomoção e perseguição;
-   ataque corpo a corpo;
-   ataque pesado;
-   ataque de área;
-   defesa e reação a dano;
-   regeneração, se prevista no conceito;
-   transição de fase;
-   vulnerabilidade do core;
-   interação com o cenário;
-   comportamento de boss e seleção de alvo.

Este documento não declara um repertório oficial de ataques para a
fusão. O catálogo final deve ser definido como design original do mod e
aprovado antes de codificar.

------------------------------------------------------------------------

## 11. Kaijus não numerados: famílias de comportamento

A biblioteca mestre lista Kaijus e espécies não numerados, incluindo
Primigenius Honju, Primigenius Yoju e variantes ressuscitadas. O
comportamento deve ser específico por espécie; não se deve atribuir a
todas o mesmo conjunto de poderes.

### Yoju

O documento mestre de habilidades deve registrar para cada Yoju a
anatomia, mobilidade, ataques observados, ameaças e fraquezas a partir
do modelo e das referências existentes. Não presumir que todo Yoju
possui mordida, cauda, asas, veneno ou projéteis.

### Honju

Tratar como ameaça de maior porte conforme a classificação já definida
no projeto. Definir ataques e comportamento conforme a espécie, modelo e
evidências disponíveis; não apenas multiplicar HP e dano de um Yoju.

### Daikaiju

Tratar como entidade de grande ameaça, com escala, destruição,
resistência e combate adequados. Definir os ataques com base na
identidade da espécie e nos sistemas de boss existentes, sem usar uma IA
genérica.

### Primigenius Yoju / Primigenius Honju

Estão listados na biblioteca como espécies específicas. Antes de lhes
atribuir repertório detalhado, verificar os modelos, registros e
referências existentes no projeto.

### Kaijus ressuscitados

Estão listados como variantes. Especificar o que muda em relação à forma
original --- por exemplo, aparência, estado, resistência ou
comportamento --- apenas se o conceito do projeto ou as referências
justificarem. Não presumir que "ressuscitado" automaticamente significa
novos poderes.

------------------------------------------------------------------------

## 12. IA de combate dos Kaijus

A biblioteca mestre determina que a IA não deve ser genérica para todos
os seres.

Fluxo de decisão de referência:

``` text
alvo
→ ameaça
→ distância
→ terreno
→ ação apropriada
→ animação
→ hitbox
→ dano
→ VFX
→ áudio
→ recuperação
```

### Requisitos

-   Considerar alvo, ameaça, distância e terreno antes de selecionar o
    ataque.
-   Escolher a ação compatível com a anatomia e o repertório daquela
    entidade.
-   Coordenar ataque, animação, hitbox, dano, VFX, áudio e recuperação.
-   Evitar ficar preso em obstáculos comuns: atravessar, destruir ou
    contornar conforme tamanho, força e regras ambientais do projeto.
-   Preservar estados de ataque e recuperação para impedir spam
    incoerente.
-   Não reutilizar uma mesma IA sem diferenças para espécies com
    capacidades distintas.
-   Reutilizar os controladores existentes e evitar duplicação.

### Bosses

Bosses devem ser fases completas de combate, não apenas inimigos com
muito HP. As fases podem alterar repertório, IA, velocidade,
regeneração, efeitos visuais, áudio, destruição e prioridades de alvo
quando isso fizer parte do design.

------------------------------------------------------------------------

## 13. Destruição ambiental e ataques de área

A biblioteca mestre exige sistemas para destruição e ataques de grande
área e destaca a interação ambiental como parte da ameaça dos Kaijus.

### Requisitos gerais

-   Escalar o impacto conforme o ataque e a entidade.
-   Usar hitboxes/áreas compatíveis com o movimento.
-   Distinguir dano a entidades de destruição de blocos.
-   Respeitar regras e proteções do mundo já existentes.
-   Evitar destruir blocos fora da área real do ataque.
-   Sincronizar impacto, partículas, áudio, dano e destruição.
-   Considerar obstáculos, terreno e navegação da IA.
-   Não deixar um Kaiju gigante preso em portas, cercas ou paredes
    comuns: destruir, atravessar ou contornar segundo as regras já
    implementadas.

Não criar um sistema concorrente de destruição se o projeto já tiver um.
Auditar primeiro e integrar.

------------------------------------------------------------------------

## 14. Sistema de chefes e eventos especiais

### Sequência No. 10 → No. 9

O evento precisa preservar a ordem narrativa definida na biblioteca: -
invasão do No. 10; - comando de Kaijus e combate; - estado
crítico/derrota; - permanência do corpo no campo; - aparição do No. 9; -
análise e absorção; - transformação; - nova fase.

A transformação precisa alterar mais do que atributos numéricos. Deve
conectar fase, IA, ataques, velocidade, regeneração, VFX, áudio,
animações, destruição e alvos.

### Regras para outros bosses

-   Criar fases apenas quando previstas no design do boss.
-   Manter transições claras entre estados.
-   Evitar duplicar o sistema de fases.
-   Fazer registro das condições de início e término de cada fase.
-   Testar morte, interrupção, carregamento de mundo e remoção de
    entidades para evitar eventos quebrados.

------------------------------------------------------------------------

## 15. Integração de cada habilidade

Para cada habilidade, manter uma ficha técnica com:

  -----------------------------------------------------------------------
  Campo                               Conteúdo esperado
  ----------------------------------- -----------------------------------
  Nome                                Nome estável e único da habilidade.

  Entidade/formas                     Kaiju e fase aos quais pertence.

  Origem                              CANON, ADAPTAÇÃO DO MOD, MOD
                                      ORIGINAL ou PENDENTE DE REFERÊNCIA.

  Tipo                                Corpo a corpo, área, projétil,
                                      defesa, mobilidade, percepção,
                                      regeneração etc.

  Condição                            Quando a IA pode usar a habilidade.

  Alcance                             Distância/área válida.

  Preparação                          Estado ou antecipação antes do
                                      efeito.

  Efeito                              Dano, knockback, destruição, estado
                                      ou outra consequência.

  Hitbox                              Forma, tamanho e momento de
                                      ativação.

  Cooldown                            Regras atuais do projeto, sem
                                      alterar sem autorização.

  Custo/limite                        Energia, órgão, fase ou condição,
                                      quando aplicável.

  Fraqueza/contramedida               Core, órgão, janela de
                                      vulnerabilidade ou outra regra
                                      confirmada.

  Animação                            ID da animação correspondente.

  VFX/áudio                           Efeitos necessários.

  Recuperação                         Estado após executar o ataque.

  Status                              Existe, incompleta, ausente ou
                                      pendente de referência.
  -----------------------------------------------------------------------

------------------------------------------------------------------------

## 16. Auditoria obrigatória antes de implementar

Para cada Kaiju presente no projeto:

1.  Localizar a entidade, o modelo, os controladores, os atributos e o
    código de IA.
2.  Inventariar poderes e ataques que já existem.
3.  Identificar quais são funcionais, incompletos, duplicados ou
    ausentes.
4.  Comparar com este documento e com referências confiáveis.
5.  Preservar as habilidades funcionais existentes.
6.  Corrigir bugs antes de adicionar sistemas concorrentes.
7.  Separar habilidades canônicas de adaptações.
8.  Registrar lacunas de informação.
9.  Propor um plano pequeno e ordenado de implementação.
10. Aguardar autorização antes de modificar arquivos.

## 17. Lacunas a pesquisar

A fonte v20 não apresenta, nas seções específicas consultadas, um
catálogo completo de cada ataque para todos os Kaijus numerados e não
numerados. Portanto, esta versão não afirma que todos os repertórios
canônicos já estejam preenchidos.

O próximo passo de pesquisa deve completar, com fontes oficiais e dados
já existentes no projeto: - ataques específicos de cada Kaiju
numerado; - habilidades e fases completas do No. 9 por forma; -
repertório específico do No. 10; - poderes e comportamento dos Kaijus
não numerados; - diferenças entre formas originais, variantes e
ressuscitados; - fraquezas, órgãos e condições de vulnerabilidade por
espécie.

Não preencher essas lacunas com invenções silenciosas. Toda proposta
nova deve ser marcada como adaptação ou conteúdo original do mod.

------------------------------------------------------------------------

## 18. Instrução final para Claude Code

Use este documento junto com `Kaiju_N8_Biblioteca_Completa_Final_v20.md`
e com os arquivos reais do projeto.

**Não implemente tudo de uma vez.** Primeiro audite o que existe, depois
informe o que está completo, o que falta e quais referências são
necessárias. Preserve os sistemas existentes e aguarde autorização antes
de editar arquivos.

Este documento é focado exclusivamente em Kaijus: seus poderes,
habilidades, ataques, sistemas, IA, comportamentos, fraquezas, fases e
interação ambiental.
