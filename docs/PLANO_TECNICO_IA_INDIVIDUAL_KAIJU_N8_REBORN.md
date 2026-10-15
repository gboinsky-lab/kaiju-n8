# KAIJU N8 REBORN — PLANO TÉCNICO DE IA INDIVIDUAL
## Combate, movimentação, personagens, kaijus, soldados e esquadrões

**Objetivo:** orientar o Claude Code na auditoria, implementação e validação da IA do mod.  
**Princípio central:** cada personagem e cada kaiju deve agir de maneira individual, reconhecível e fundamentada em pesquisa da obra original, com adaptações de gameplay claramente identificadas.

## SUMÁRIO
1. Regras invioláveis
2. Pesquisa da obra e fidelidade
3. Auditoria do projeto
4. Arquitetura geral da IA
5. Percepção, alvos e memória
6. Movimentação e navegação
7. Combate
8. GeckoLib, animações e impactos
9. Perfis individuais dos personagens
10. Perfis individuais dos kaijus
11. Soldados e esquadrões
12. Dificuldade, adaptação e chefes
13. Desempenho e multiplayer
14. Fases de implementação
15. Testes e critérios de aceitação
16. Instruções para Claude Code
17. Fontes iniciais

---

# PARTE 1 — REGRAS INVIOLÁVEIS

1. Auditar o código antes de editar. Não reescrever a IA inteira sem mapear classes, atributos, goals, registries, animações e eventos atuais.
2. Preservar sistemas funcionais, modelos, texturas, animações, IDs e arquivos de registro.
3. Não mudar HP, dano, armadura, velocidade, alcance, cooldown, regeneração ou tamanho apenas para mascarar um problema de IA.
4. Não presumir que uma animação funciona apenas porque existe no JSON.
5. Não aplicar dano a cada tick de uma animação. Cada impacto deve ser um evento controlado.
6. Compartilhar infraestrutura é bom; usar a mesma lógica tática para todos os personagens não é.
7. Distinguir sempre **CANON**, **INFERÊNCIA DE GAMEPLAY** e **MOD ORIGINAL**.
8. Manter compatibilidade com as versões existentes de Minecraft, loader, Java, GeckoLib e dependências.
9. Implementar mudanças pequenas, reversíveis e testáveis.
10. Não declarar que algo funciona no jogo sem teste real. Compilar não prova que movimento, combate, animação ou multiplayer estão corretos.

# PARTE 2 — PESQUISA DA OBRA E FIDELIDADE INDIVIDUAL

## 2.1 Pesquisar antes de programar
Para cada personagem, forma ou espécie, consultar primeiro:
- site oficial do anime e perfis oficiais;
- episódios, trailers e cenas oficiais disponíveis legalmente;
- materiais oficiais de produção/enciclopédias;
- mangá, quando necessário para distinguir material ainda não adaptado;
- fontes secundárias confiáveis apenas como complemento.

Não basta pesquisar o nome. Observar como luta, se movimenta, a distância preferida, preparação dos ataques, respostas ao perigo, limitações, uso de habilidades e cooperação.

## 2.2 Ficha obrigatória por entidade
Criar uma ficha contendo:
- nome e formas incluídas;
- links oficiais consultados;
- episódio/capítulo/cena, se identificável;
- armas, habilidades e limitações confirmadas;
- estilo de luta e distância preferida;
- mobilidade, esquivas, saltos, voo ou investidas;
- padrões de ataque e tempo de preparação;
- respostas defensivas;
- prioridades táticas e cooperação;
- fatos **CANON**;
- **INFERÊNCIAS** plausíveis para gameplay;
- **MOD ORIGINAL**;
- animações e diferenças entre formas.

Não apresentar inferências como fatos oficiais. A identidade deve ser fiel, mas cooldowns, duração e força podem ser adaptados para o jogo.

# PARTE 3 — AUDITORIA DO PROJETO

Antes de editar:
1. identificar versões de Minecraft, loader, Java, GeckoLib e dependências;
2. localizar classes de entidades, IA, navegação, combate, armas, habilidades e registries;
3. localizar controllers e arquivos de animação;
4. mapear eventos de dano, hitboxes, cooldowns, transformações e invocações;
5. identificar IA de soldados, esquadrões e aliados;
6. identificar sistemas existentes de memória, fases de chefe, destruição e desempenho.

Claude Code deve entregar um relatório com arquivos relevantes, fluxo atual de percepção → decisão → movimento → ataque → dano → recuperação, bugs reproduzíveis, animações acionadas, riscos e plano de alterações. **Não iniciar uma reescrita geral antes desse relatório.**

Criar checkpoint/commit antes de cada fase. Separar mudanças de IA das mudanças de balanceamento. Não mexer em modelos 3D quando a tarefa for somente lógica.

# PARTE 4 — ARQUITETURA GERAL

## 4.1 Pipeline
1. Percepção.
2. Avaliação tática.
3. Seleção de alvo/intenção.
4. Planejamento de movimento.
5. Execução de ação.
6. Validação de ataque/habilidade.
7. Avaliação do resultado.
8. Atualização da decisão.

## 4.2 Estados de execução
Usar estados equivalentes aos necessários na arquitetura atual:
- IDLE, INVESTIGATE, SEARCH_TARGET;
- APPROACH, CHASE, KEEP_DISTANCE, STRAFE, FLANK;
- ATTACK_PREPARATION, ATTACK_ACTIVE, ATTACK_RECOVERY;
- DODGE, BLOCK/GUARD, STAGGER;
- RETREAT, REPOSITION, USE_ABILITY, TRANSFORM;
- RETURN_TO_POST, DEAD.

Uma camada tática decide a intenção; a máquina de estados executa a ação. Cada estado define entrada, saída, timeout, animação, permissão de dano, movimento e interrupções válidas. Não criar estados redundantes se o projeto já tiver equivalentes.

## 4.3 Perfis individuais
Cada perfil deve controlar distância ideal, distância de segurança, agressividade, tolerância a dano, preferência por golpes rápidos/pesados, esquiva, ataque à distância, proteção de aliados, perseguição/recuo, adaptação e pesos de habilidades. Esses parâmetros táticos não substituem atributos de combate.

# PARTE 5 — PERCEPÇÃO, ALVOS E MEMÓRIA

A seleção de alvo deve considerar ameaça, distância, possibilidade de alcançar, linha de visão, missão, ordens, proteção de aliados e tipo de inimigo. Não escolher sempre o mais próximo.

Evitar troca constante de alvo: manter o alvo atual até morrer, ficar inacessível ou surgir uma ameaça claramente mais importante. Usar margem de prioridade para evitar oscilação.

Distinguir alvo visível, alvo detectado e última posição conhecida. Soldados não devem atirar através de paredes sem habilidade explícita. Ao perder visão, procurar rota ou investigar a última posição.

Manter memória curta e limitada: último alvo, posição conhecida, ataque recente, ameaça detectada, aliados/ordens e padrões recentes quando a entidade puder adaptar-se. Expirar dados antigos e não manter referências inválidas.

# PARTE 6 — MOVIMENTAÇÃO E NAVEGAÇÃO

## 6.1 Intenções
- **APPROACH:** aproximar até a distância ideal.
- **CHASE:** perseguir alvo em fuga.
- **KEEP_DISTANCE:** manter distância de tiro.
- **STRAFE:** mover lateralmente mantendo o alvo em vista.
- **FLANK:** buscar lateral/traseira por rota viável.
- **RETREAT:** recuar por perigo ou necessidade tática.
- **REPOSITION:** sair de posição ruim ou obstáculo.
- **DODGE:** esquiva reativa.
- **HOLD_POSITION:** defender posição.
- **AIR_APPROACH/AIR_DIVE:** aproximação e mergulho aéreo.
- **RETURN:** voltar ao grupo, patrulha ou posto.

## 6.2 Bugs a eliminar
A IA não deve correr contra paredes, alternar esquerda/direita sem progresso, atacar fora de alcance repetidamente, girar no lugar, atravessar aliados sem necessidade, abandonar combate por pequenas oscilações ou ficar presa em animações.

Detectar falta de progresso; tentar rota alternativa e reposicionamento; cancelar ações impossíveis com segurança. Teleporte não deve ser solução padrão.

## 6.3 Escala e locomoção
A navegação deve considerar a caixa de colisão e a escala real da entidade. Uma rota válida para um humano não é necessariamente válida para um kaiju gigante. Voo deve ter navegação própria, altitude e aterrissagem; não tratar uma criatura aérea como um mob terrestre.

Investidas devem definir preparação, trajetória, colisão, impacto e recuperação. Destruição de blocos, quando existir, deve ser limitada por regras explícitas e orçamento por intervalo.

Sempre que possível, usar aceleração/desaceleração e rotação gradual. A entidade deve virar antes de ataques direcionais; não girar instantaneamente durante todo golpe, salvo exceção intencional.

# PARTE 7 — SISTEMA DE COMBATE

Todo ataque deve ter:
1. seleção;
2. validação de alvo, alcance, ângulo, linha de visão e cooldown;
3. preparação;
4. execução;
5. evento de impacto;
6. recuperação;
7. avaliação de acerto/erro.

Cada golpe define alcance, arco, janela de impacto, preparação, recuperação, custo/cooldown, alvo adequado, interrupções e animação. Escolher o ataque pela situação e identidade, não apenas porque está disponível.

Combos devem ter sequência explícita e impacto individual por golpe. Não repetir infinitamente o mesmo ataque, não continuar combo quando o alvo saiu do alcance e não duplicar dano.

Esquivas dependem de ataque identificado, direção, espaço livre, cooldown e perfil individual; não devem ser aleatórias a cada tick. Se existir invulnerabilidade, limitar a uma janela explícita.

Reações a dano podem incluir continuar, stagger, esquivar, recuar, mudar alvo, proteger zona vulnerável ou iniciar fase. Não aplicar stagger a cada golpe leve.

Contra alvos grandes, usar hitboxes e alcance corretos. Não permitir que uma arma curta acerte a vários blocos só porque a caixa do kaiju é grande; também evitar golpes visualmente conectados que falham por hitbox deslocada.

# PARTE 8 — GECKOLIB, ANIMAÇÕES E IMPACTOS

Para cada animação de ataque, documentar controller, entidade/arma, duração, preparação, tempo de impacto, janela ativa, recuperação, som/VFX, interrupções e número real de impactos.

Um golpe simples aplica dano uma vez no evento configurado. Golpes múltiplos têm eventos separados e estado de “já acertou neste golpe” para evitar dano repetido por vários ticks/colisões. Limpar o estado no próximo golpe. Confirmar que dano e cooldowns são autoritativos no servidor.

Ataques fixos devem restringir movimento somente quando necessário; ataques móveis devem ter trajetória definida. Evitar conflito entre controller de animação e navegação. Toda animação deve ter saída por conclusão, interrupção válida ou timeout seguro.

Testar no jogo: orientação, sincronização, hitbox, dano único, segundo golpe, som/VFX sem repetição, retorno à locomoção e consistência multiplayer.

# PARTE 9 — PERFIS INDIVIDUAIS DOS PERSONAGENS

Os perfis abaixo são intenções de gameplay; habilidades específicas devem ser verificadas em fontes oficiais e no código atual. Não inventar poderes canônicos.

## 9.1 Kafka Hibino — forma humana
Usar conhecimento prático de anatomia e limpeza de kaijus, improvisação e persistência. Procurar oportunidades, cobertura e vulnerabilidades; ajudar aliados e evitar enfrentar sozinho ameaças muito superiores. Não lutar como a forma No. 8 nem usar automaticamente habilidades da transformação.

## 9.2 Kafka / Kaiju No. 8
Combate próximo, força explosiva e golpes de impacto. Fechar distância, escolher ataques pesados contra alvos grandes e golpes mais rápidos quando úteis, aproveitar aberturas e reagir a ameaças próximas. A forma precisa de perfil, riscos e recuperação próprios; não ser apenas troca de textura ou multiplicador de atributos.

## 9.3 Mina Ashiro
Especialista de longo alcance e grandes alvos. Buscar linha de tiro e distância segura, priorizar ameaças grandes ou objetivos designados, preparar disparos fortes e reposicionar-se quando o inimigo se aproxima. Não perseguir inimigos para combate corpo a corpo nem atirar através de paredes.

## 9.4 Soshiro Hoshina
Especialista em lâminas, precisão, velocidade e combate contra kaijus pequenos/médios. Manter distância curta, usar sequências rápidas, explorar aberturas, aproximar-se em passos curtos e reposicionar-se lateralmente. Cada golpe deve ter impacto separado. Não agir como atirador nem ser automaticamente o melhor contra qualquer escala de inimigo.

## 9.5 Kikoru Shinomiya
Combatente agressiva, talentosa e móvel, com machado. Avançar com decisão, alternar golpes rápidos e comprometidos, explorar flancos e esquivar de ataques pesados. Ser agressiva sem ser invencível. Forma/equipamento especial deve ter perfil próprio, não apenas mais vida e dano.

## 9.6 Reno Ichikawa
Disciplina, aprendizagem e apoio à equipe. Manter posição de tiro/suporte quando adequado, priorizar ameaças aos aliados e reposicionar-se se a linha de tiro for bloqueada. Se houver forma/equipamento No. 6, tratá-lo como perfil próprio com habilidades confirmadas, não como Reno com atributos multiplicados.

## 9.7 Gen Narumi
Combatente de elite com decisões precisas e leitura de padrões. Priorizar ameaças de alto risco, adaptar a estratégia quando o inimigo revela padrões, alternar pressão e reposicionamento. Não prever tudo perfeitamente: reações respeitam tempo e possibilidade real de esquiva.

## 9.8 Isao Shinomiya
Comandante e força pesada. Postura estável, ataques comprometidos contra ameaças importantes, resposta disciplinada e coordenação de aliados se o sistema permitir. Não perseguir alvos pequenos sem motivo. Forma/equipamento associado ao No. 2 deve ser perfil distinto.

## 9.9 Iharu Furuhashi
Iniciativa e competitividade, mas ainda útil ao grupo. Avançar ao ver oportunidade, pressionar alvos compatíveis e reposicionar-se. A impulsividade pode mudar pesos de decisão, nunca criar loops nem ignorar objetivos essenciais.

## 9.10 Haruichi Izumo
Combatente de equipe: manter formação, cobrir linhas de tiro, apoiar alvo designado e evitar disputar o espaço de aliados quando há outra ameaça.

## 9.11 Aoi Kaguragi
Disciplina e coordenação. Manter ordens, avançar com o grupo, priorizar ameaças que rompem formação e recuar de forma organizada.

## 9.12 Konomi Okonogi
Se representada como operadora/suporte, usar IA de suporte, não de combatente corpo a corpo por padrão. Permanecer em posição segura, comunicar ameaças e apoiar objetivos apenas se essas mecânicas existirem.

## 9.13 Rin Shinonome, Hasegawa e outros
Pesquisar individualmente. Não copiar Mina ou um soldado genérico só porque usam armas à distância. Documentar arma, papel, disciplina, agressividade, posicionamento e habilidades confirmadas antes de programar.

# PARTE 10 — PERFIS INDIVIDUAIS DOS KAIJUS

## 10.1 Princípio
Kaijus devem diferir por padrões, mobilidade, alcance, ataques, resistência, regeneração, inteligência e fases — não apenas HP e dano. Valores atuais devem ser auditados e testados; este documento não autoriza mudanças automáticas.

## 10.2 Yojus
Ameaças menores, rápidas ou numerosas. Podem atacar em aproximações curtas, circular, recuar diante de alvos muito fortes e cercar em grupo sem ocupar o mesmo ponto. Não tornar todos suicidas; variar agressividade, velocidade e defesa por espécie.

## 10.3 Honjus
Ameaças mais robustas, com golpes comprometidos, controle de área e janelas de recuperação reconhecíveis. A diferença para Yojus deve ser tática, não só mais vida.

## 10.4 Daikaijus
Considerar escala e colisão, usar ataques de área com preparação visual, pressionar grupos/zonas quando houver mecânica, reagir a mudanças de fase e evitar girar instantaneamente para cada soldado. Limitar destruição de ambiente.

## 10.5 Kaiju No. 9
Ameaça inteligente, estratégica e adaptativa. Avaliar ameaças, alternar combate próximo/distância quando a forma tiver esses ataques, usar Finger Gun ou invocações apenas quando confirmados para aquela forma, proteger-se em desvantagem e recuar para regenerar apenas com condição plausível. Evitar regeneração infinita.

**Adaptação limitada:** registrar tipos de ataque recentes, aumentar prioridade de defesa/reposicionamento contra padrões repetidos, buscar ângulos/distâncias melhores, expirar memória e nunca criar imunidade permanente ou aumento ilimitado de atributos. Não conhecer ataques que não poderia perceber.

Para cada forma original do mod — base, escura, fusão com No. 10, fusão com Formiga/Ant, evoluída/vermelha, após absorção de Isao/No. 2 e forma adaptativa final — criar perfil próprio. Marcar cada forma como CANON, INFERÊNCIA ou MOD ORIGINAL. Fusões alternativas não devem ser apresentadas como canônicas sem fonte.

## 10.6 Kaiju No. 10
Pressão agressiva, força bruta e ataques de impacto. Perseguir de forma direta, punir quem fica perto, usar ataques pesados com preparação e recuperar-se após golpes comprometidos. Não copiar a adaptação estratégica do No. 9. Fases avançadas podem aumentar agressividade, sem remover todas as janelas de reação.

Se houver versões pequena/grande da fusão No. 9 + No. 10, cada uma precisa de navegação, hitbox e alcance próprios. Preservar a decisão do projeto: forma pequena de fusão = **5 blocos**; forma grande = **20 blocos**, até nova instrução explícita.

## 10.7 Trichonephila
Se teias forem confirmadas no conteúdo, controlar área, limitar rotas, criar zonas de risco e conduzir alvos para posições desfavoráveis. Não lançar teia sem alcance/linha de visão, não cobrir o campo inteiro sem limites e limitar partículas/entidades afetadas.

## 10.8 Preondactyl
Se a forma voadora estiver incluída, escolher altitude e distância, aproximar-se por rota aérea, executar mergulhos com trajetória e impacto definidos e recuperar altitude. Evitar alternar voo/chão a cada tick ou bater repetidamente em estruturas.

## 10.9 Ressurgidos/revividos
Definir o que muda em relação à forma original. Não somar todas as melhorias ao mesmo tempo sem testes. Distinguir visual e taticamente a variante; limitar regeneração e revives para evitar lutas intermináveis. Marcar como conteúdo do mod se não for canon.

## 10.10 Outros numerados e espécies futuras
Pesquisar No. 1, 2, 3, 4, 5, 6, 7, 11–15 e outras entidades individualmente. Não inferir habilidades pelo número. Distinguir kaiju, usuário de equipamento e transformação. Documentar fonte e pendências de confirmação.

# PARTE 11 — SOLDADOS E ESQUADRÕES

## 11.1 Papéis
- **Atirador:** manter distância/linha de visão, buscar cobertura, reposicionar-se e não avançar para corpo a corpo sem motivo.
- **Corpo a corpo:** aproximar-se por rota viável, buscar flanco e evitar ocupar o mesmo espaço de aliados.
- **Suporte/defesa:** ficar perto de aliados/objetivos e usar apenas mecânicas de suporte existentes.
- **Líder:** distribuir alvos, manter coesão e emitir ordens de avanço, defesa, foco, retirada e reagrupar.

## 11.2 Coordenação
Distribuir alvos por função, distância, ameaça e ordens. Evitar que todos disputem o mesmo espaço. Permitir foco coordenado contra chefe quando ordenado. Recalcular quando o campo mudar de forma relevante.

## 11.3 Formação
Formação flexível, espaçamento compatível com arma e escala do inimigo, sem empilhamento persistente. Abrir corredores, reorganizar após obstáculos e não teletransportar soldados para manter formação.

## 11.4 Ordens
Se o mod tiver ou receber ordens, padronizar: Seguir, Manter posição, Defender área, Atacar alvo, Focar chefe, Recuar, Reagrupar, Patrulhar e Interagir com objetivo. A ordem do jogador supera preferências secundárias, mas não exige executar ações impossíveis.

## 11.5 Retirada
Recuar por rotas viáveis, usar cobertura da linha de frente quando implementada, manter atiradores em distância segura e reagrupar no destino. Não correr em direções aleatórias.

# PARTE 12 — DIFICULDADE, ADAPTAÇÃO E CHEFES

Sempre que possível, a dificuldade deve afetar qualidade de decisão, reação, uso de habilidades, coordenação e frequência de erros — não apenas multiplicadores de HP/dano. Manter multiplicadores numéricos documentados à parte.

Cada fase de chefe deve especificar condição de entrada, ações desbloqueadas, mudança de prioridades, efeitos, vulnerabilidades, saída e proteção contra transições repetidas em loop.

Ataques perigosos precisam de preparação visual/sonora, direção ou área ameaçada e tempo aproximado de impacto. Dificuldades maiores podem reduzir a janela de reação, mas não devem remover toda legibilidade sem escolha explícita.

A adaptação só se aplica a entidades apropriadas, com memória limitada, expiração e limites claros. Nunca criar imunidade permanente ou atributos ilimitados.

# PARTE 13 — DESEMPENHO E MULTIPLAYER

- Não executar pathfinding caro a cada tick.
- Controlar intervalos de percepção e decisão.
- Reutilizar informação de esquadrão quando seguro.
- Limitar invocações, clones, partículas, destruição e efeitos.
- Reduzir complexidade para entidades distantes sem quebrar suas funções.
- Restaurar frequência normal em combate próximo.
- Em multiplayer, o servidor deve ser autoridade para dano, cooldowns e decisões; clientes recebem estados/animações sincronizados.
- Adicionar logs de debug opcionais para estado, alvo/motivo, movimento, ataque, falha de rota, fase, adaptação e tempo de decisão. Não imprimir por tick em produção.

# PARTE 14 — FASES DE IMPLEMENTAÇÃO

## Fase 0 — Auditoria
Inventário, fluxo atual, bugs, animações, métricas básicas e plano. Não editar sistemas importantes antes do relatório.

## Fase 1 — Movimento e estados
Corrigir travamentos, recuperação de rota, estabilidade de alvo, saídas de estado, aproximação/distância e orientação antes do ataque.

## Fase 2 — Combate
Validar alcance/ângulo, preparação/impacto/recuperação, dano único, interrupções e sincronização com animações.

## Fase 3 — Personagens
Piloto: Hoshina, Kikoru, Mina, Kafka humano/No. 8, Reno, Narumi e Isao; depois os demais. Implementar e testar cada perfil separadamente.

## Fase 4 — Kaijus
Piloto: Yoju terrestre, Honju terrestre, No. 9, No. 10, Trichonephila, Preondactyl e depois variantes/numerados.

## Fase 5 — Esquadrões
Papéis, distribuição de alvos, formação, foco, ordens, retirada e reagrupar.

## Fase 6 — Chefes/adaptação
Fases, sinais legíveis, adaptação limitada do No. 9 e pressão do No. 10.

## Fase 7 — Otimização
Carga com várias entidades, multiplayer, regressões, telemetria e documentação.

Ao concluir cada fase, listar arquivos alterados, testes, resultados e problemas pendentes.

# PARTE 15 — TESTES E ACEITAÇÃO

## 15.1 Cenários mínimos
Testar alvo parado/em movimento, fora do alcance, atrás de obstáculo, morto durante preparação, dano recebido durante ação, vários inimigos, corredor estreito, terreno irregular, perda/recuperação de visão, mudança de alvo, alvo descarregado, aliados próximos e conclusão de animação.

## 15.2 Métricas
Registrar quando possível:
- duração e resultado de combate;
- taxa de acerto por ataque;
- ataques inválidos;
- travamentos e tempo de recuperação;
- dano duplicado (meta: zero);
- transições de estado inválidas;
- custo/tempo de decisão;
- chamadas de pathfinding;
- entidades/efeitos ativos;
- frequência de ataque por personagem;
- participação dos aliados.

## 15.3 Balanceamento
Não tirar conclusões de uma luta isolada. Repetir confrontos em condições comparáveis. Separar problemas de IA, hitbox, animação e atributos. Antes de mudar HP/dano, verificar posicionamento, frequência, precisão, cooldown, regeneração e eventos de impacto. Metas de vitória são objetivos de design, não fatos comprovados.

## 15.4 Matriz de regressão
- Hoshina contra Yoju/Honju, No. 9 e No. 10;
- Kikoru contra Honju, No. 9 e No. 10;
- Mina contra kaiju terrestre grande;
- Kafka/No. 8 contra alvo menor e alvo semelhante;
- soldado de rifle com aliado corpo a corpo;
- esquadrão contra múltiplas ameaças;
- duas ou mais entidades grandes em terreno com obstáculos.

# PARTE 16 — INSTRUÇÕES PARA CLAUDE CODE

Ordem obrigatória:
1. Ler este documento e as instruções do repositório.
2. Pesquisar fontes oficiais para a entidade que será trabalhada.
3. Criar ficha e separar CANON, INFERÊNCIA e MOD ORIGINAL.
4. Auditar classes e recursos atuais.
5. Apresentar plano específico antes de editar.
6. Alterar uma fase/entidade por vez.
7. Compilar e executar testes disponíveis.
8. Comparar comportamento anterior e novo.
9. Informar arquivos alterados, testes e limitações.

Não alterar dezenas de entidades simultaneamente; não inventar poderes canônicos; não usar HP/dano para substituir IA; não criar dano duplicado; não alterar balanceamento para mascarar navegação ruim; não declarar sucesso sem teste real.

Relatório final por etapa:
- objetivo;
- fontes consultadas;
- canon/inferência/mod original;
- arquivos alterados;
- comportamento anterior e novo;
- testes e resultados;
- problemas conhecidos;
- riscos/regressões;
- próximo passo.

**Sucesso significa:** personagens com decisões próprias; kaijus com padrões táticos reconhecíveis; movimento e combate confiáveis; animações sincronizadas; soldados cooperativos; No. 9 adaptativo com limites; No. 10 agressivo; fontes e inferências claramente separadas; melhoria comprovada sem regressões.

# PARTE 17 — FONTES INICIAIS DA OBRA

Use estas fontes como ponto de partida e abra as páginas antes de implementar. Para decisões específicas, localizar uma cena oficial que demonstre o comportamento.

1. Site oficial do anime (inglês): https://kaiju-no8.net/en/
2. Site oficial japonês: https://kaiju-no8.net/
3. Production I.G — equipe e elenco: https://www.production-ig.co.jp/works/kaiju-no8/staf_cast.html
4. Shonen Jump+ — informações oficiais de mídia: https://www.shonenjump.com/p/mediainfo/kaiju8/
5. Kaiju No. 8 THE GAME — site oficial complementar: https://www.kj8-thegame.com/en/

Os perfis oficiais podem resumir apenas parte das habilidades. Se não houver evidência suficiente para uma decisão, marcar como inferência ou conteúdo original do mod. Mecânicas de outro jogo não são automaticamente cânone do anime/mangá.

---
**Fim do documento.**
