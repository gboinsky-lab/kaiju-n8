# KAIJU N8 REBORN — DOCUMENTO MESTRE DE IDEIAS E IMPLEMENTAÇÃO
## Sistemas selecionados, integração com mecânicas existentes e instruções para Claude Code

**Projeto-alvo:** Minecraft 1.21.1 + NeoForge  
**Objetivo:** reunir em um único documento os sistemas avançados escolhidos para o mod, incluindo combate, IA, armas, progressão, missões, mundo dinâmico e suporte militar.

> **Regra de escopo:** este documento é uma especificação de design e implementação. Ele não comprova que os sistemas já existem nem altera o código por si só. O Claude Code deve auditar o repositório real, integrar o que já está implementado e testar as alterações.

---

# PARTE I — REGRAS GERAIS PARA O CLAUDE CODE

1. Antes de editar arquivos, audite o repositório e identifique entidades, IA, combate, animações, armas, equipamentos, missões, invasões, HUD, NPCs, progressão, persistência, dissecação, materiais, receitas e efeitos visuais já existentes.
2. Classifique cada sistema descrito neste documento como `EXISTENTE`, `PARCIAL`, `AUSENTE` ou `QUEBRADO`, citando os arquivos/classes e evidências.
3. Não crie sistemas paralelos quando já existir um sistema apropriado. Estenda os sistemas atuais sempre que tecnicamente possível.
4. **Dissecação:** o mod já tem um método de dissecar kaijus. Preserve o fluxo e amplie-o; não crie outra mecânica de dissecação ou coleta de restos.
5. Preserve IDs de registros, dados salvos, receitas, itens, compatibilidade, balanceamento e comportamento existente, salvo quando uma alteração for necessária e justificada.
6. Não invente APIs do Minecraft/NeoForge nem presuma que um modelo possui animações funcionais apenas porque elas existem no arquivo de arte. Verifique as versões e padrões do projeto.
7. Separe o que é **fato do cânone do anime**, **adaptação de gameplay** e **conteúdo original do mod**. Não apresente uma mecânica criada para o jogo como habilidade confirmada do anime.
8. Implemente por etapas pequenas, compilando e testando a cada etapa. Não faça uma reescrita massiva nem adicione classes sem necessidade.
9. Combate e progressão precisam ser validados no servidor. Evite dano duplicado entre animação e lógica de combate, exploits de pacotes, recompensas duplicadas e ações sem cooldown.
10. Evite trabalho caro a cada tick: nada de varreduras globais de entidades, criação contínua de NPCs, partículas ou eventos sem limites. Use eventos, estados, intervalos e regiões delimitadas.
11. Se uma dependência não existir, implemente primeiro a extensão mínima necessária ou registre o bloqueio. Não apresente botões sem função como sistemas concluídos.
12. Não declare testes aprovados sem executá-los. No relatório final, diferencie `TESTADO`, `NÃO TESTADO` e `BLOQUEADO`.
13. Antes das mudanças, apresente um plano curto com dependências e riscos. Ao final, liste os arquivos alterados, os testes reais e os passos de validação dentro do jogo.

---

# PARTE II — COMBATE, ARMAS E IA

## 1. Movimentos e funções próprias para cada arma

### Objetivo
Dar identidade mecânica às armas e evitar que sejam apenas modelos com números de dano diferentes.

### Requisitos
- Inventariar as armas existentes e suas propriedades: dano, alcance, velocidade, munição/energia, animações, efeitos e requisitos.
- Utilizar uma definição de comportamento centralizada e compatível com a arquitetura atual, evitando condicionais espalhadas por várias classes.
- Quando aplicável, cada arma poderá definir ataque primário, ataque alternativo, tipo de impacto, preparação, recuperação, custo, cooldown, efeitos visuais/sonoros e vantagens específicas.
- Exemplos de identidade, adaptados ao conteúdo que realmente existe:
  - **Espadas:** ataques próximos, rápidos e encadeáveis.
  - **Machados pesados:** maior impacto e recuperação mais lenta.
  - **Armas de fogo:** alcance, munição, recarga e comportamento de mira.
  - **Armas especializadas:** funções próprias apenas se sustentadas pelo design e pelos sistemas disponíveis.
- Não conceder automaticamente a todos os personagens as técnicas exclusivas de outro personagem.
- Validar arma equipada, estado do jogador, cooldown, munição e permissões no servidor.
- Se não houver animação exclusiva, oferecer uma alternativa funcional sem bloquear o combate.

### Aceitação
- As armas relevantes têm diferenças reais e testáveis.
- Um ataque não causa dano duplicado por evento de animação e código de combate.
- Cooldowns e munição não podem ser burlados por spam de pacotes.
- Armas antigas continuam funcionais.

## 2. Contra-ataques e esquivas perfeitas

### Objetivo
Recompensar leitura de movimento e precisão de tempo, sem transformar o combate em invulnerabilidade constante.

### Requisitos
- Verificar primeiro se esquiva, bloqueio, parry ou janela de invulnerabilidade já existem.
- Se houver sistema de estados de combate, integrar a janela de esquiva perfeita nele.
- Uma esquiva executada no instante correto pode conceder uma pequena janela de contra-ataque, interromper um golpe elegível ou permitir reposicionamento.
- A janela deve variar por ataque e ser configurável. Ataques de área ou ataques explicitamente não bloqueáveis não precisam permitir parry.
- Evitar invulnerabilidade longa ou empilhamento de esquivas sem custo.
- Dar feedback visual/sonoro breve, respeitando as opções de efeitos do projeto.
- Sincronizar a decisão de sucesso no servidor para evitar discrepância em multiplayer.

### Aceitação
- Esquiva normal e esquiva perfeita são distinguíveis.
- O jogador não fica invulnerável indefinidamente.
- O contra-ataque não pode ativar várias vezes no mesmo golpe recebido.
- Testar timing correto, incorreto, latência e ataques que não aceitam parry.

## 3. IA de kaijus que aprende padrões do jogador

### Objetivo
Fazer inimigos inteligentes adaptarem a resposta a hábitos repetidos do jogador, inspirados na capacidade estratégica de certos kaijus, sem atribuir essa inteligência a toda espécie.

### Requisitos
- Criar perfis de comportamento por espécie ou entidade. Só kaijus classificados como táticos devem usar adaptação avançada.
- Registrar sinais simples e limitados, como repetição de ataques, frequência de combate à distância, esquivas repetidas ou permanência em uma faixa de alcance.
- Responder com contramedidas disponíveis ao kaiju: aproximar-se, recuar, interromper uma sequência, mudar a distância ou usar um ataque alternativo existente.
- Não permitir que a IA conheça ações futuras, atravesse paredes ou acerte ataques impossíveis.
- Usar memória curta e limitada, com decaimento/reset fora do combate, para evitar custo e comportamento permanentemente punitivo.
- A adaptação deve produzir variedade e leitura tática, não imunidade automática ao estilo do jogador.
- Reutilizar a máquina de estados/IA existente; não criar um segundo cérebro que entre em conflito com pathfinding e ataques atuais.

### Aceitação
- A adaptação acontece apenas em perfis elegíveis.
- Repetir uma tática pode provocar uma resposta observável, mas o inimigo ainda pode ser derrotado.
- A IA não fica oscilando entre estados nem executa ações inválidas.
- Desempenho é medido em encontros com múltiplas entidades.

## 4. Ferimentos persistentes nos kaijus

### Objetivo
Fazer o dano recebido alterar o comportamento do kaiju, e não apenas sua barra de vida.

### Requisitos
- Auditar se já há membros segmentados, pontos fracos, estados de ferimento ou animações de dano.
- Implementar apenas efeitos coerentes com a estrutura da entidade: ferimento em perna pode reduzir velocidade; braço afetado pode limitar um ataque; ferimento grave pode alterar postura ou agressividade.
- Não presumir que todos os kaijus têm anatomia humanoide ou partes equivalentes.
- Separar estado mecânico de efeito visual. Se o modelo não suporta uma deformação ou animação específica, manter o efeito mecânico sem prometer uma mudança visual inexistente.
- Não reduzir atributos a cada golpe sem limite. Usar limiares, duração ou estados bem definidos.
- Persistir o estado somente quando necessário para a entidade e para o ciclo atual do encontro; evitar salvar dados transitórios sem motivo.
- Integrar a avaliação de ameaça e os pontos fracos, se esses sistemas estiverem presentes.

### Aceitação
- Um ferimento elegível altera uma ação ou atributo definido.
- O efeito não acumula indefinidamente.
- O estado visual, quando disponível, corresponde ao estado mecânico.
- A entidade continua funcional depois de salvar e recarregar.

## 5. Adaptação dos kaijus ao terreno

### Objetivo
Fazer espécies diferentes aproveitarem o ambiente de maneira coerente.

### Requisitos
- Auditar pathfinding, quebra de blocos, voo, navegação aquática e interações ambientais atuais.
- Criar capacidades por perfil de espécie: contornar obstáculos, quebrar obstáculos autorizados, usar rotas abertas, voar ou aproveitar espaços específicos quando a entidade realmente suporta isso.
- Não conceder voo, escavação ou destruição universal a todas as espécies.
- Limitar destruição por configuração e regras do mundo; respeitar proteção de áreas e opções de servidor quando existentes.
- Evitar que a IA tente repetidamente atravessar blocos impossíveis ou destrua estruturas sem fim.
- Fornecer fallback de navegação se o comportamento especializado falhar.

### Aceitação
- Cada perfil usa apenas capacidades suportadas pela entidade.
- A IA consegue desistir de uma rota impossível e tentar outra.
- Destruição ambiental é limitada e configurável.
- Testar terrenos abertos, espaços estreitos, obstáculos e diferenças de altura.

## 6. Hierarquia e funções entre kaijus

### Objetivo
Permitir encontros em que kaijus coordenem papéis diferentes, em vez de atacarem todos da mesma maneira.

### Requisitos
- Definir papéis opcionais por espécie ou encontro: líder tático, agressor, perseguidor, suporte à distância ou distração.
- Não afirmar que todas as espécies canônicas possuem hierarquia social; a coordenação é uma mecânica de gameplay quando não estiver explicitamente sustentada pela obra.
- A coordenação deve depender de proximidade, estado de combate e capacidades disponíveis.
- Um líder não deve comandar entidades fora da região ou de encontros não relacionados.
- Se o líder for derrotado, os demais podem mudar de comportamento de acordo com regras configuráveis, não necessariamente fugir ou morrer.
- Evitar sincronizar todos os ataques no mesmo instante. Usar intervalos, prioridades e limites de atacantes ativos.

### Aceitação
- Em um grupo, pelo menos dois papéis comportam-se de forma diferente.
- Os inimigos não bloqueiam uns aos outros de forma permanente nem atacam todos simultaneamente sem controle.
- A derrota do líder não quebra a IA restante.

## 7. Chefes com fases realmente distintas

### Objetivo
Criar batalhas em que cada fase exige uma estratégia diferente, em vez de apenas aumentar vida e dano.

### Requisitos
- Usar fases explícitas e configuráveis, baseadas em vida, eventos de combate ou condições próprias do chefe.
- Cada fase deve alterar algo concreto: conjunto de ataques, ritmo, distância preferida, vulnerabilidade, mobilidade ou comportamento.
- Manter as transições sincronizadas e processadas uma única vez.
- Evitar que cura, dano simultâneo ou recarga do mundo faça o chefe alternar repetidamente entre fases.
- Criar telegráficos claros para ataques perigosos e janelas de reação justas.
- Não criar uma fase nova para todo kaiju comum; reservar o sistema completo para chefes e entidades designadas.

### Aceitação
- Cada fase possui ao menos uma mudança mecânica observável.
- A transição não duplica ataques, recompensas ou animações.
- O jogador tem uma oportunidade razoável de compreender o novo padrão.
- O chefe continua correto depois de recarregar o mundo, quando o encontro for persistente.

---

# PARTE III — ESQUADRÕES E PROGRESSÃO

## 8. Companheiros com funções táticas

### Objetivo
Montar esquadrões com soldados que tenham papéis e ordens distintos.

### Requisitos
- Reutilizar NPCs, seguidores, sistema de esquadrão e armas existentes.
- Funções possíveis: atacante, suporte, atirador, defesa ou reconhecimento, desde que os equipamentos e a IA suportem a função.
- Ordens possíveis: seguir, manter posição, atacar alvo designado, proteger uma área ou recuar.
- Cada soldado deve respeitar alcance, linha de visão, munição e capacidade de navegação.
- Impedir que ordens repetidas gerem entidades duplicadas ou estados conflitantes.
- Limitar tamanho do esquadrão por configuração e progressão.
- Se a IA de escolta ainda for frágil, começar com ordens simples e testáveis em vez de prometer uma formação avançada completa.

### Aceitação
- Pelo menos duas funções/ordens têm comportamento realmente diferente.
- Soldados não disparam sem munição, atacam alvos inválidos ou ficam presos em loops de comando.
- O estado do esquadrão é consistente após reload.

## 9. Provas de promoção

### Objetivo
Fazer promoções dependerem de desempenho demonstrado, além de XP acumulada.

### Requisitos
- Auditar patentes, XP, méritos, missões e requisitos existentes.
- Integrar avaliações como precisão, combate, sobrevivência, resgate e trabalho em equipe, selecionando somente métricas que o projeto consiga medir de verdade.
- Criar testes práticos curtos e reproduzíveis. Não exigir uma métrica que não seja registrada pelo jogo.
- Apresentar requisitos antes de iniciar a prova e explicar o resultado ao final.
- Evitar perder progresso já obtido por jogadores após atualização.
- Não permitir farm de promoção repetindo uma prova de forma explorável.

### Aceitação
- O jogador consegue ver os requisitos e o resultado.
- A prova usa estatísticas reais e a recompensa é concedida uma única vez.
- Patentes antigas e saves existentes continuam válidos.

## 10. Base com instalações funcionais

### Objetivo
Transformar a base da Força de Defesa em um centro operacional com serviços úteis.

### Requisitos
- Auditar estruturas, blocos funcionais, menus, laboratórios, bancadas e sistemas de missão atuais.
- Instalações possíveis: treinamento, pesquisa, fabricação, preparação de esquadrão e seleção de missões.
- Cada instalação deve ter ao menos uma função real, desbloqueio ou interação funcional.
- Reutilizar menus e interfaces existentes antes de criar novos.
- Não exigir uma base totalmente construída para usar sistemas básicos se isso não fizer parte da progressão atual.
- Se a construção modular ainda não existir, começar com interações funcionais em instalações fixas.

### Aceitação
- As instalações não são apenas decorativas.
- Os serviços respeitam requisitos e custos.
- A base não duplica menus, receitas ou funcionalidades já disponíveis em outro lugar.

## 11. Compatibilidade e domínio de equipamentos numerados

### Objetivo
Dar aos equipamentos avançados requisitos e estilos próprios, sem permitir que todos os personagens os usem com eficiência idêntica por padrão.

### Requisitos
- Auditar equipamentos numerados, compatibilidade de personagem, traje, liberação de poder e domínio já existentes.
- Se os dados canônicos sustentarem uma compatibilidade específica, registrá-la como dado de design; separar de qualquer regra original de balanceamento.
- Quando apropriado, definir requisitos, domínio, afinidade, estabilidade e curva de aprendizagem.
- Não bloquear o conteúdo atual de forma inesperada nem invalidar equipamento já adquirido.
- Não inventar um risco narrativo como fato canônico. Riscos mecânicos adicionais são adaptação de gameplay.
- Exibir requisitos e consequências antes de equipar.
- Evitar multiplicadores de poder excessivos que tornem equipamentos anteriores inúteis.

### Aceitação
- Requisitos são claros e validados no servidor.
- Equipamentos incompatíveis não podem ser ativados por exploração.
- Equipamentos existentes continuam funcionando e o balanceamento foi comparado.

## 12. Avaliação pós-missão

### Objetivo
Dar feedback claro sobre o desempenho e conectar as missões à progressão.

### Requisitos
- Reutilizar o sistema de missões e as estatísticas já existentes.
- Avaliar apenas métricas mensuráveis, como objetivos concluídos, precisão se rastreada, dano recebido, aliados/civis salvos e recursos gastos.
- Separar nota, recompensa e requisitos de promoção para que uma nota baixa não apague progresso não relacionado.
- Evitar contabilizar o mesmo objetivo várias vezes em multiplayer.
- Exibir resumo conciso, recompensas, objetivos incompletos e sugestões úteis.
- Não conceder recompensas em duplicidade ao reabrir a tela ou recarregar.

### Aceitação
- Cada missão concluída produz no máximo uma avaliação e uma concessão de recompensa.
- O resultado corresponde aos dados registrados durante a missão.
- Missões antigas sem algumas métricas continuam funcionando.

---

# PARTE IV — MUNDO, MISSÕES E APOIO MILITAR

## 13. Cidades com níveis de alerta

### Objetivo
Fazer regiões reagirem às ameaças de forma persistente e legível.

### Requisitos
- Auditar invasões, spawn, regiões, alertas, NPCs, missões e persistência do mundo.
- Criar estados configuráveis, por exemplo: normal, alerta, evacuação e combate ativo.
- Cada estado deve alterar apenas sistemas que existam ou que sejam implementados de forma funcional: patrulhas, alertas, disponibilidade de missões, spawn e serviços.
- Manter um estado autoritativo por região, com transições e condições claras.
- Não recalcular todas as regiões a cada tick. Atualizar por eventos, intervalos ou quando ocorrer uma mudança relevante.
- Definir recuperação/retorno ao normal e evitar que a cidade permaneça em alerta para sempre por causa de um evento órfão.
- Se o mod ainda não possui sistema de cidades, começar com regiões/eventos delimitados em vez de fingir que existe uma simulação urbana completa.

### Aceitação
- Uma região muda de estado por uma causa identificável.
- HUD e missões mostram o mesmo estado.
- O estado persiste conforme necessário e se recupera após o encerramento da ameaça.
- O sistema não exige varreduras globais constantes.

## 14. Evacuação e zonas de perigo

### Objetivo
Criar objetivos de evacuação e zonas que exigem preparação antes ou durante o combate.

### Requisitos
- Integrar às missões, alertas regionais e NPCs existentes.
- Definir limites da zona, objetivo de evacuação, prazo quando necessário e condição de encerramento.
- A zona pode sinalizar risco, limitar serviços ou ativar objetivos, mas não deve aplicar dano arbitrário sem aviso.
- Se existirem civis, usar entidades e regras atuais. Se não existirem, implementar primeiro um objetivo simples de extração compatível com o sistema de missão.
- Não criar NPCs continuamente nem duplicar sobreviventes após reload.
- Falha de evacuação deve ter consequência explícita e balanceada; não precisa significar morte automática de todos os NPCs.

### Aceitação
- O jogador consegue identificar a zona e o que precisa fazer.
- Sucesso, falha e expiração funcionam.
- A evacuação conecta-se ao estado de alerta da região sem criar dois estados contraditórios.

## 15. Apoio militar solicitado pelo jogador

### Objetivo
Permitir solicitar recursos operacionais em momentos adequados da missão.

### Requisitos
- Auditar suporte aéreo, rádio, suprimentos, extração, soldados aliados e cooldowns existentes.
- Tipos possíveis, conforme os sistemas disponíveis: reconhecimento, fogo de cobertura, suprimentos ou extração.
- Cada solicitação exige condições válidas, como rádio funcional, área acessível, recurso disponível, cooldown ou progresso de missão.
- O suporte não pode resolver qualquer batalha automaticamente. Limitar dano, alcance, frequência e situações em que pode ser solicitado.
- Se o projeto ainda não possui helicópteros ou veículos funcionais, não criar dependência deles para todas as funções: usar uma representação simples e consistente ou marcar o recurso visual como pendente.
- Autoridade do servidor para evitar spam, duplicação e uso fora da missão.
- Exibir confirmação, indisponibilidade e resultado real da solicitação.

### Aceitação
- Cada solicitação tem custo/requisito e efeito funcional.
- Cooldown e limites são aplicados no servidor.
- Chamadas inválidas não consomem recursos indevidamente.
- Falha de navegação ou ausência de alvo não trava a missão.

## 16. Rastro dinâmico de destruição

### Objetivo
Fazer a passagem de um kaiju deixar evidências temporárias que possam ajudar a compreender seu trajeto e o estado do local.

### Requisitos
- Reutilizar o sistema atual de destruição ambiental, pistas e investigação, caso exista.
- Registrar apenas alterações ou pontos de interesse relevantes; não guardar uma cópia completa de cada bloco destruído.
- Pistas possíveis: marcas, blocos danificados, resíduos ou sinais de combate, desde que existam formas eficientes de representá-los.
- Vincular pistas ao evento/kaiju quando essa informação for conhecida; caso contrário, identificá-las como inconclusivas.
- Definir duração, limite por região e limpeza segura das pistas.
- Não modificar/restaurar blocos do jogador de forma destrutiva nem sobrescrever construções para criar rastros.
- Evitar atualizações pesadas por tick e salvar apenas o necessário.

### Aceitação
- O rastro corresponde a um evento real e não aponta para entidades inexistentes.
- Pistas expiram ou são limpas sem acumular indefinidamente.
- Não há restauração indevida de construções dos jogadores.
- A investigação consegue consumir as pistas sem duplicar dados.

---

# PARTE V — INTEGRAÇÃO E ORDEM DE IMPLEMENTAÇÃO

## Dependências recomendadas

1. **Auditoria completa:** criar mapa dos sistemas existentes, parciais, ausentes e quebrados.
2. **Base de combate:** centralizar estados de ataque, esquiva, contra-ataque e regras de arma, sem substituir sistemas funcionais.
3. **IA de kaijus:** perfis táticos, adaptação de padrões, ferimentos persistentes, adaptação ao terreno e papéis de grupo.
4. **Chefes:** implementar fases usando os estados e ataques já disponíveis.
5. **Esquadrões:** funções e ordens, depois conectar a missões e avaliação.
6. **Progressão:** provas de promoção, compatibilidade/domínio de equipamento e avaliação pós-missão.
7. **Base:** conectar instalações a treinamento, pesquisa, fabricação e missões existentes.
8. **Mundo dinâmico:** estados de alerta regionais, evacuação e rastros de destruição.
9. **Apoio militar:** integrar rádio/solicitações com as regras de missão e alerta.
10. **Integração final:** validar que missões, HUD, IA e persistência compartilham a mesma fonte de verdade.

A auditoria pode alterar essa ordem. Se uma dependência já estiver funcional, valide-a em vez de reimplementá-la.

## Regras de desempenho e compatibilidade
- Atualizar IA adaptativa e estados regionais em intervalos adequados, não necessariamente a cada tick.
- Limitar memória de padrões, quantidade de entidades coordenadas, frequência de apoio e número de rastros.
- Usar dados configuráveis para cooldowns, limiares, duração de alerta e limites ambientais quando fizer sentido.
- Preservar saves antigos; usar valores padrão seguros para dados novos ausentes.
- Garantir que lógica de combate, recompensas, promoção e chamadas de apoio seja validada no servidor.
- Não exigir assets que ainda não existem para que a lógica principal funcione.

---

# PARTE VI — PLANO DE TESTES

Executar e registrar resultados reais para:

1. Build/compilação e inicialização no Minecraft 1.21.1 + NeoForge da versão do projeto.
2. Armas: ataque básico, habilidades, cooldowns, munição, troca de arma e dano sem duplicação.
3. Esquiva: timing correto/incorreto, contra-ataque, ataques não bloqueáveis e multiplayer se suportado.
4. IA adaptativa: repetição de ataques, reset de combate, múltiplos inimigos e ausência de loops.
5. Ferimentos: limiares, limites de penalidade, efeitos visuais disponíveis e save/reload.
6. Terreno: obstáculos, diferenças de altura, rotas impossíveis e limites de destruição.
7. Hierarquia: papéis diferentes, derrota do líder, distâncias e número de ataques simultâneos.
8. Chefes: transição de fase, morte, reload, dano simultâneo e concessão única de recompensa.
9. Esquadrões: ordens, munição, alvos inválidos, navegação, reload e limite de soldados.
10. Promoções: requisitos, métricas, falha, repetição da prova e saves anteriores.
11. Base: interações funcionais, requisitos, custos e integração com sistemas existentes.
12. Equipamentos numerados: compatibilidade, domínio, desbloqueio, equipamento antigo e balanceamento.
13. Avaliação de missão: métricas, objetivos opcionais, multiplayer e recompensa única.
14. Alertas regionais: transições, recuperação, persistência e múltiplas ameaças.
15. Evacuação: sucesso, falha, expiração, desconexão e reload.
16. Apoio militar: chamada válida/inválida, cooldown, custo, ausência de alvo e spam.
17. Rastros: criação, limite, expiração, reload e proteção de construções dos jogadores.
18. Desempenho: sem varreduras globais ou criação contínua de entidades sem limite.

Se algum teste não puder ser executado, marque-o como `NÃO TESTADO` ou `BLOQUEADO` e explique o motivo.

---

# PARTE VII — RELATÓRIO OBRIGATÓRIO DO CLAUDE CODE

Ao terminar cada etapa, informe:

1. Estado inicial de cada sistema e arquivos relacionados.
2. O que foi implementado, reaproveitado, parcialmente concluído ou bloqueado.
3. Arquivos alterados e motivo de cada alteração.
4. Como as integrações evitam duplicar sistemas existentes.
5. Comandos de build/teste executados e resultados reais.
6. Problemas restantes, riscos de desempenho e compatibilidade.
7. Passos manuais para testar dentro do Minecraft.
8. Próxima etapa recomendada.

**Instrução final:** comece auditando o projeto. Não implemente todos os sistemas de uma vez. Priorize integração real, preservação das mecânicas existentes, testes e relatório honesto acima da quantidade de arquivos ou classes novas.
