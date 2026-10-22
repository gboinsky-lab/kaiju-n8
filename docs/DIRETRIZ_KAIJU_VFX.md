<!-- Transcricao de Diretriz_KaijuVFX_Efeitos_por_Personagem.docx (Miguel, 2026-10-22). O .docx na raiz do repositorio e o original. -->

# KaijuVFX — Diretriz de Efeitos Visuais por Personagem

Projeto: Kaiju No. 8 | Minecraft 1.21.1 | NeoForge

Documento de referência para o Claude Code. Esta diretriz deve ser consultada antes de criar, alterar ou integrar partículas, efeitos especiais, habilidades, animações visuais ou efeitos de traje.

## 1. Regra principal: identidade visual individual

Cada personagem deve possuir efeitos visuais próprios, coerentes com seu design. Não aplique o mesmo efeito genérico a todos os personagens apenas trocando a cor. Use como referência o personagem real presente no projeto, suas texturas, cores, traje, armas, núcleo, anatomia, tema de combate e habilidades estabelecidas.

- Corpo e aparência: use as cores predominantes e secundárias do modelo e das texturas do personagem.
- Traje/armadura: respeite a paleta, os materiais e os detalhes visuais do traje. Efeitos de soldados devem combinar com o uniforme, equipamento e arma utilizados.
- Habilidades: escolha cores, formas, movimento, densidade e intensidade coerentes com a habilidade e com a identidade visual do personagem.
- Pontos de energia: se o personagem tiver núcleo, órgãos luminosos, fissuras ou pontos de carga, use-os como origem visual apenas quando isso fizer sentido para o design.
- Efeitos de transformação/regeneração: mantenha a identidade do personagem e diferencie esses efeitos dos ataques comuns.
- Não invente cores ou poderes como se fossem canônicos. Se a referência não for clara, inspecione os assets do projeto e pesquise fontes confiáveis; registre as incertezas antes de decidir.

## 2. Ficha obrigatória de identidade visual

Antes de implementar efeitos para cada personagem, preencher ou atualizar uma ficha com:

- Nome/identificador do personagem e versão/forma (incluindo transformações ou formas numeradas).
- Paleta primária, secundária e de destaque, baseada nas texturas/modelos existentes.
- Cores do traje, armadura, armas e acessórios, quando aplicável.
- Cores e aparência de cada habilidade conhecida.
- Formato visual: partículas, arcos, feixes, aura, fumaça, eletricidade, detritos, distorção ou outros.
- Origem e direção do efeito: mão, boca, núcleo, arma, membro, ponto de impacto ou posição definida pela animação.
- Referência usada: arquivo de textura/modelo, documentação ou fonte consultada.
- Alternativas para qualidade baixa e limites de desempenho.

## 3. Diferenciar os efeitos além da cor

A cor é obrigatória, mas não é suficiente. Diferencie personagens também pela silhueta do efeito, ritmo, velocidade, direção, tamanho, densidade, textura, som e sequência de execução. Dois personagens podem compartilhar uma cor, mas não devem parecer usar a mesma habilidade com uma simples troca de tonalidade.

- Ataques físicos: rastros curtos, arcos de corte, poeira e impacto conforme a força e o movimento.
- Projéteis/feixes: núcleo, camada externa, rastro, animação e impacto com parâmetros configuráveis.
- Carregamento: intensificação progressiva, partículas atraídas ou energia acumulada quando apropriado.
- Impacto: onda de choque, detritos e fumaça; a destruição de blocos deve ser controlada separadamente.
- Aura/passivos: efeito contínuo econômico, com limites de partículas e atualização eficiente.
- Regeneração/mutação: efeito próprio, visualmente distinguível de ataques e de dano recebido.

## 4. Implementação técnica e arquitetura

- Centralize recursos reutilizáveis em KaijuVFX, mas mantenha configurações específicas por personagem e habilidade.
- Prefira dados/configurações por habilidade para cores, texturas, escala, duração, velocidade, opacidade e qualidade, em vez de duplicar código.
- Use partículas com texturas próprias e renderizadores personalizados quando partículas vanilla não forem suficientes.
- Use GeckoLib para sincronizar os momentos visuais com as animações e os estados do personagem; evite temporizadores arbitrários quando houver eventos de animação adequados.
- Separe renderização de lógica de combate: o servidor continua sendo a autoridade para dano, colisão e execução da habilidade.
- Em multiplayer, envie eventos compactos de efeito aos clientes; não envie um pacote por partícula.
- Shaders e pós-processamento são opcionais. Efeitos essenciais devem continuar legíveis sem shaderpack externo.
- Mantenha compatibilidade com Minecraft 1.21.1, NeoForge e as versões reais das dependências do repositório. Verifique APIs antes de usá-las.

## 5. Processo obrigatório antes de criar cada efeito

1. Inspecionar: examinar modelo, texturas, traje, arma, animações e código atual do personagem.
2. Pesquisar: consultar documentação compatível com as versões instaladas e referências confiáveis para a aparência da habilidade.
3. Definir: registrar paleta, forma, movimento, origem, duração e comportamento do efeito.
4. Implementar: reutilizar o KaijuVFX, mantendo parâmetros e recursos específicos do personagem.
5. Testar: verificar aparência em jogo, sincronização com animação, multiplayer, encerramento e níveis gráficos.
6. Documentar: anotar arquivos alterados, recursos criados, testes executados e limitações restantes.

## 6. Desempenho, acessibilidade e robustez

- Adicionar níveis de qualidade (baixo, médio e alto), limites de partículas e distância de renderização.
- Evitar criar entidades temporárias para cada fragmento; reutilizar renderização de partículas quando apropriado.
- Garantir que efeitos tenham duração e limpeza definidas e não permaneçam após a habilidade terminar.
- Evitar ofuscar a tela, esconder perigos importantes ou produzir flashes excessivos. Oferecer opções para reduzir tremor de câmera e efeitos intensos.
- Testar vários personagens e habilidades simultaneamente e observar FPS, uso de memória e logs.

## 7. Critérios de aceitação por personagem

- O efeito combina com as cores reais do corpo, traje, arma e habilidade do personagem.
- A identidade visual não depende apenas de trocar a cor de um efeito genérico.
- Origem, direção, escala e timing combinam com o modelo e a animação.
- O efeito funciona sem shaderpack externo e tem uma alternativa de qualidade reduzida.
- Dano e colisão continuam corretos com efeitos visuais reduzidos ou desativados.
- A implementação compila e foi testada; qualquer teste não realizado é declarado claramente.
- Texturas, arquivos e parâmetros estão nomeados e organizados de forma consistente.

## 8. Instrução fixa para o Claude Code

LEMBRE-SE EM TODA IMPLEMENTAÇÃO: crie efeitos especiais individualizados para cada personagem. Respeite a cor do corpo, a cor e os detalhes do traje/armadura, as armas, a paleta e a aparência de cada habilidade, além da forma e do movimento característicos. Não use efeitos genéricos sem adaptação. Antes de codificar, inspecione os assets e o código existentes e pesquise referências compatíveis. Registre a paleta e as decisões visuais por personagem, reutilize a infraestrutura comum do KaijuVFX, teste no jogo e informe o que foi efetivamente validado.

Aplicação recomendada: salvar uma cópia deste documento na raiz do repositório e acrescentar em CLAUDE.md uma instrução curta apontando para ele, para que o Claude Code consulte esta diretriz em futuras sessões.

