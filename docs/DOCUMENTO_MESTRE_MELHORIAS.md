# Kaiju No. 8 Reborn — Documento mestre de auditoria, correcoes, melhorias e expansao

Recebido do Miguel em 2026-11-11. Copia fiel do conteudo (secoes e itens), para consulta nas etapas.
A auditoria e o plano que respondem a este documento estao em `docs/AUDITORIA_MESTRE.md`.

Projeto: Kaiju No. 8 Reborn. Minecraft 1.21.1, NeoForge. Modelos e animacoes: Meshy AI, Blockbench e GeckoLib.
Combate: "integracao com Epic Fight" (ver a auditoria: o mod nao usa Epic Fight).

## 1. Objetivo principal
Auditoria completa e, depois, todas as melhorias necessarias. Nao se limitar as tarefas do documento: investigar o
projeto inteiro, achar problemas ainda nao identificados e funcionalidades incompletas ou nao implementadas.
Abrange: personagens, modelos 3D, armas, armaduras, rigs, animacoes, combate, habilidades, efeitos, sons, kaiju, IA,
destruicao, movimentacao, parkour, treinamento, NPCs, interface, progressao, otimizacao, integracoes tecnicas.

## 2. Regra mais importante: preservacao
Nao apagar personagens, armas, habilidades, kaiju, modelos do Meshy, bases, sistemas de gameplay, animacoes sem
necessidade; nao substituir armaduras; nao recriar o que funciona; nao simplificar sistemas existentes; nao trocar
uma implementacao funcional sem vantagem tecnica clara e autorizacao. Melhorar, corrigir, expandir e completar.
Implementacao antiga com problema: tentar corrigir sem substituir; se precisar substituir, conversar antes.

## 3. Auditoria geral
Examinar codigo, pacotes, registros, entidades, modelos, texturas, animacoes, sons, recursos, dependencias,
combate, habilidades, GeckoLib, Epic Fight, configuracoes e documentacao. Inventario com niveis:
1 inexistente, 2 incompleta, 3 funcional mas precisa melhorar, 4 completa e funcional. Nada e "concluido" so por
existir arquivo: conferir se esta ligado ao jogo e funciona.

## 4. Correcoes individuais das armas
1. Miguel: inverter lado/orientacao da foice; a parte maior da lamina para baixo.
2. Kota: tamanho da arma. 3. Iharu: tamanho da arma. 4. Aoi Kaguragi: posicao da arma.
5. Haruichi: arco na mao e encaixe da empunhadura. 6. Rin Shinonome: posicao e tamanho.
7. Jura: posicao. 8. Soichiro: posicao. 9. Gen Narumi: posicao. 10. Kikoru: posicao.
11. Mina Ashiro: posicao, orientacao e maneira como segura o "rifle".
Conferir em cada uma: escala, rotacao, posicao, ancoragem, maos, animacoes, sobreposicao com o corpo, 1a/3a pessoa.
Sem configuracao generica para armas diferentes.

## 5. Modelos do Meshy, rigs e animacoes
5.1 Auditar todos: com/sem rig, rig incompleto, hierarquia errada, ossos desconectados, deformacao, geometria
defeituosa, partes ausentes, texturas incorretas, problemas nas animacoes. 5.2 Rig para quem nao tem e correcao dos
defeituosos, compativel com GeckoLib (e Epic Fight); rigs diferentes por tipo (humano, armadura, arma, kaiju).
5.3 Nao substituir os modelos originais; preservar o arquivo original ao corrigir.

## 6. Animacoes (todas)
Personagens: idle, caminhada, corrida, sprint, pulo, queda, aterrissagem, agachamento, esquiva, parkour, escalada,
defesa, ataques, combos, especiais, dano, morte. Movimentos naturais, transicoes suaves, sincronia com a arma, maos
certas, sem travar nem deformar, identidade propria por personagem. Kaiju: idle, caminhada, corrida, ataques,
habilidades, rugidos, movimentos especiais, reacoes, morte, pela anatomia e tamanho de cada um.

## 7. Experiencia do jogador (prioridade)
7.1 Movimentacao (andar, correr, sprint, lateral, tras, pulo, queda, aterrissagem, esquiva, agachar, parkour,
escalada, movimento em combate). 7.2 Inventario individual de todas as armas (modelo, textura, escala, empunhadura,
posicao, animacoes, ataques, combos, efeitos, sons, hitbox, alcance, dano, velocidade, Epic Fight) e implementar:
leves, pesados, combos, especiais, aereos, em movimento, defesa, contra-ataque, finalizacao, preparo, recolhimento,
troca de arma, impacto, sons individuais; estilo proprio por tipo (arco, espada, foice, arma de fogo, especiais).
7.3 Combate: acertos, hitboxes, combos, dano, alcance, velocidade, recuperacao, cancelamento, reacoes dos inimigos,
retorno visual e sonoro, integracao com habilidades; mais responsivo e com mais impacto.
7.4 Sons do jogador (passos, corrida, pulo, aterrissagem, esquiva; ataques, acerto, erro, bloqueio, contra-ataque,
combos, finalizacao; disparo, recarga, movimento, preparo; armadura: ativacao, liberacao, movimento, energia,
especiais). 7.5 Efeitos (rastros, particulas, impactos, ondas, velocidade, energia, especiais, ativacao da armadura,
liberacao, esquiva, aterrissagem), sincronizados. 7.6 Habilidades: funcionamento, animacao, som, efeito, dano,
alcance, recarga, custo, hitbox, integracao; nao remover nenhuma.

## 8. Impacto fisico e reacoes
Empurrao proporcional, cambaleio, atordoamento, quedas, reacao a golpe pelas costas e na cabeca, por categoria,
tremor de camera, ondas de choque, impacto no chao, reacao corporal. Integrar ao combate atual, sem substituir.

## 9. Ferimentos visuais dos kaiju
Marcas de impacto, carapaca danificada, nucleo exposto, aparencia pela vida, efeitos de ferimento, reacoes, perda
de partes quando viavel. Solucao por anatomia, nao generica.

## 10. IA dos kaiju e NPCs
Nao recriar; implementar so o que falta: perseguicao, distancia, ataque pela situacao, destruir obstaculos e
construcoes, defensivo/ofensivo, uso inteligente de habilidades, reacoes, por especie, coordenacao. Soldados:
equipe, protecao, esquadroes, ataques coordenados, reacao aos kaiju, combate, ambiente.

## 11. Liberacao de poder das armaduras
Indicadores, auras, velocidade, forca, habilidades desbloqueadas, efeitos de ativacao, efeitos individuais,
equilibrio. Preservar armaduras, habilidades e personagens.

## 12. Destruicao ambiental
Expandir sem substituir: destrocos, crateras, ondas, marcas, estruturas desabando, destruicao pela forca, interacao
com construcoes, efeitos. Referencia: soldados localizado, Yoju menor, Honju estruturas maiores, Daikaiju grandes
areas, numerados especial pelas habilidades (referencia de comportamento, nao autorizacao para mexer em configs).

## 13. Kaiju numerados e nao numerados
Nao numerados: animacoes, ataques, sons, rugidos, efeitos, IA, movimento, reacoes, morte. Numerados: identidade
propria (animacoes, ataques, habilidades, efeitos, sons, vocalizacoes, combate, IA, reacoes, destruicao), nunca
genericos com outra cor. Regras: Yoju parecido com o Honju dele; Honju maior e distinto; Daikaiju muito maior;
numerados humanoides e bipedes em geral; nenhum kaiju menor que um humano; preservar a identidade dos modelos.

## 14. Animacoes cinematograficas
Finalizacoes, entradas de chefes, animacoes especiais, transformacoes, combos, cameras especiais, ataques fortes,
sincronia com efeitos; sem prejudicar o jogo nem exagerar na duracao.

## 15. Armaduras no jogador — PRIORIDADE MAXIMA
Investigar por que todas as armaduras apresentam problemas equipadas: renderizacao, posicao, escala, ossos,
compatibilidade, texturas, animacoes, 1a e 3a pessoa, conflito com Epic Fight. Achar a causa real e corrigir sem
substituir as armaduras.

## 16. Revisao de todos os modelos
Partes ausentes ou quebradas, faces invertidas, geometria defeituosa, texturas ausentes, sobreposicao, escala, rig,
deformacao; corrigir preservando a identidade.

## 17. Sons do projeto
Personagens, soldados, armas, armaduras, habilidades, kaiju, ambiente, destruicao, interface: qualidade, volume,
distancia, variacao, sincronia; sem repeticao excessiva nem som fora de hora.

## 18. Otimizacao
LOD, particulas por distancia, animacoes fora da tela, IA, entidades, destruicao, calculos, qualidade, modelos e
texturas; sem baixar a qualidade sem necessidade.

## 19. Outras funcionalidades
Parkour, treinamento, progressao, missoes, interface, inventario, equipamentos, interacoes, NPCs, combate,
habilidades e o resto; documentar oportunidades.

## 20. Autonomia e participacao do Miguel
Autonomia para investigar, corrigir bugs, ajustar armas e escalas, melhorar codigo, otimizar, corrigir rigs sem
mudar a identidade, melhorar animacoes preservando o carater, sons, efeitos, completar o que ja esta especificado.
Perguntar quando: varias formas de implementar com resultado diferente, escolhas de design, duvida sobre como um
personagem/kaiju/arma/habilidade funciona, anime x adaptacao, mudanca grande de jogo, mudanca estrutural de modelo
ou animacao, problema sem solucao segura, conflito entre sistemas, ideias novas grandes, precisar de referencias,
limites do Meshy/GeckoLib/Epic Fight, equilibrio. Pedir autorizacao para: excluir, substituir modelos ou bases,
remover habilidades, mudar a identidade de personagens, recriar sistemas de forma incompativel, mudancas
irreversiveis, decisoes de design nao definidas. Melhor perguntar do que decidir errado.

## 21. Metodologia
Fase 1 auditoria; Fase 2 correcoes visuais e tecnicas (armas, armaduras, modelos, rigs, renderizacao); Fase 3
jogador e combate; Fase 4 kaiju e NPCs; Fase 5 destruicao e efeitos; Fase 6 gameplay e otimizacao; Fase 7 testes e
revisao final. Pode dividir em subtarefas, documentando o progresso.

## 22. Testes obrigatorios
Inicializacao, entidades, modelos, animacoes, sons, armas, armaduras, ataques, habilidades, combate, destruicao, IA,
desempenho; testar em jogo quando possivel; dizer claramente o que nao foi testado; nunca dizer que testou sem testar.

## 23. Relatorio por fase
Encontrado; ja existia; incompleto; nao existia; implementado; corrigido; arquivos; sistemas preservados; testes;
problemas restantes; decisoes do Miguel; proximas tarefas. Lista de pendencias atualizada.

## 24-25. Resultado esperado e instrucao final
Experiencia completa e consistente, sistemas integrados e testados, nada que exista so no visual. Investigar,
identificar, planejar, perguntar quando necessario, implementar, testar e aprimorar, preservando o trabalho feito.
