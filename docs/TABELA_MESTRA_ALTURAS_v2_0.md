# KAIJU N8 REBORN — TABELA MESTRA DE ALTURAS
## Personagens, soldados, Kaijus e formas especiais
**Versão:** 2.0  
**Data:** 10 de outubro de 2026  
**Destino:** Claude Code — projeto Minecraft / GeckoLib

---

## 1. Instrução principal para o Claude Code

Este documento reúne as alturas de referência para **todos os personagens e Kaijus identificados na documentação atual do projeto**, incluindo formas alternativas e as duas versões da fusão do Kaiju N.º 9 com o N.º 10.

Antes de implementar:
1. Audite os registros de entidades, modelos `.bbmodel`, arquivos de animação, renderers, configurações de escala e hitboxes.
2. Identifique qual modelo corresponde a cada linha desta tabela. Não associe modelos apenas pelo nome do arquivo.
3. Confira se o valor atual é altura visual, fator de escala, dimensão de hitbox ou unidade interna do modelo.
4. Use **1 bloco do Minecraft ≈ 1 metro** apenas como convenção de altura visual-alvo do projeto. Não presuma que uma unidade interna do modelo equivale a um bloco.
5. Preserve proporções, pivôs, hierarquia de ossos, UVs, texturas, posições de armas e animações.
6. Ajuste e verifique separadamente a altura visual, a hitbox e o alinhamento com o chão.
7. Não altere vida, dano, IA, habilidades, velocidade ou outros atributos de combate só por mudar a escala.
8. Não altere modelos que não possam ser identificados com segurança. Liste-os como pendentes e peça confirmação.
9. Faça primeiro um teste em uma entidade; depois de validar o método, aplique às demais.
10. Ao terminar, entregue relatório com altura anterior, altura-alvo, arquivos alterados e testes executados.

## 2. Como interpretar os valores

- **Referência documentada:** valor que já aparece na documentação do projeto ou em uma referência identificada.
- **Proposta do mod:** altura visual sugerida para manter uma escala coerente no Minecraft; não é altura canônica confirmada.
- **Pendente:** não há altura individual suficientemente definida na documentação atual. Claude Code deve medir o modelo e deixar o valor sem alteração até a aprovação.
- Para humanos, equipamentos, armas, asas, caudas e efeitos que se estendem além do corpo não devem ser confundidos com a altura corporal.
- As faixas de categoria definidas pelo projeto são: **Yoju: 4–8 blocos; Honju: 6–15 blocos; Daikaiju: 20–30 blocos**. A categoria é uma referência de projeto e não substitui a altura específica aprovada para uma variante.

---

## 3. Personagens humanos e integrantes da Força de Defesa

As alturas abaixo são **propostas visuais para o mod**, salvo quando explicitamente identificadas como referência. Não são uma lista de alturas oficiais do anime.

| Personagem / entidade | Altura-alvo | Status / observações |
|---|---:|---|
| Jogador humano | 1,80 bloco | Referência de projeto; não alterar a escala global do jogador sem autorização |
| Kafka Hibino — forma humana | 1,81 bloco | Referência publicada para a altura humana |
| Kafka Hibino — forma Kaiju N.º 8 | 2 blocos | Referência citada anteriormente para a forma normal; confirmar visualmente com o modelo e a fonte antes de fixar como definitiva |
| Kafka / N.º 8 — versão colossal original do mod | Pendente | Não usar 22 blocos como se fosse a forma normal; só criar se houver uma variante colossal separada e aprovada |
| Mina Ashiro | 1,75 bloco | Proposta visual |
| Soshiro Hoshina — normal | 1,72 bloco | Proposta visual; conferir alcance e posições das espadas |
| Soshiro Hoshina — equipado com Numbers Weapon N.º 10 | 1,72 bloco | O equipamento não aumenta automaticamente a altura corporal |
| Reno Ichikawa — normal | 1,75 bloco | Proposta visual |
| Reno Ichikawa — equipado com Numbers Weapon N.º 6 | 1,75 bloco | O equipamento/efeitos de gelo não aumentam a altura corporal |
| Kikoru Shinomiya — normal | 1,60 bloco | Proposta visual |
| Kikoru Shinomiya — equipada com Numbers Weapon N.º 4 | 1,60 bloco | Asas/efeitos podem ampliar a dimensão total, mas não a altura corporal base |
| Gen Narumi — normal | 1,78 bloco | Proposta visual |
| Gen Narumi — equipado com Numbers Weapon N.º 1 | 1,78 bloco | Equipamento não aumenta automaticamente a altura corporal |
| Isao Shinomiya — normal | 1,90 bloco | Proposta visual |
| Isao Shinomiya — equipado com Numbers Weapon N.º 2 | 1,90 bloco | Equipamento não aumenta automaticamente a altura corporal |
| Iharu Furuhashi | 1,72 bloco | Proposta visual |
| Haruichi Izumo | 1,75 bloco | Proposta visual |
| Aoi Kaguragi | 1,85 bloco | Proposta visual |
| Eiji Hasegawa | 1,85 bloco | Proposta visual |
| Rin Shinonome | 1,70 bloco | Proposta visual |
| Kota | 1,72 bloco | Proposta provisória; confirmar identidade/modelo no repositório |
| Jugo Ogata | 1,85 bloco | Proposta visual |
| Toko Kirie | 1,75 bloco | Proposta visual |
| Soichiro Hoshina | 1,78 bloco | Proposta visual |
| Hikari Shinomiya | 1,75 bloco | Proposta visual |
| Jura Igarashi | 1,80 bloco | Proposta visual |
| Akari Minase | 1,65 bloco | Proposta visual |
| Hakua Igarashi | 1,72 bloco | Proposta visual |
| Ryo Ikaruga | 1,75 bloco | Proposta visual |
| Tae Nakanoshima | 1,65 bloco | Proposta visual |
| Konomi Okonogi | 1,60 bloco | Proposta visual |
| Keiji Itami | 1,72 bloco | Proposta visual |
| Juzo Nogizaka | 1,75 bloco | Proposta visual |
| Akira Kurusu | 1,72 bloco | Proposta visual |

### Soldados genéricos

| Tipo de soldado | Altura-alvo | Observações |
|---|---:|---|
| Recruta | 1,70 bloco | Proposta visual |
| Soldado normal | 1,75 bloco | Proposta visual |
| Soldado experiente | 1,80 bloco | Proposta visual |
| Soldado de elite | 1,85 bloco | Proposta visual |
| Soldado pesado | 1,90 bloco | Proposta visual; armadura pode aumentar a silhueta, não necessariamente a altura corporal |

---

## 4. Kaijus comuns, Yojus, Honjus e variantes ressuscitadas

As alturas individuais marcadas como proposta são alvos iniciais do mod. Validar cada uma com o modelo e a categoria antes de implementar.

| Kaiju / variante | Categoria / tipo | Altura-alvo | Status / observações |
|---|---|---:|---|
| Trichonephila — Yoju | Yoju | 4,5 blocos | Proposta visual; a documentação antiga contém dimensões de hitbox/modelo diferentes, portanto medir antes de converter |
| Trichonephila — Honju | Honju | 9 blocos | Proposta visual |
| Primigenius — Yoju | Yoju | 5,5 blocos | Proposta visual |
| Primigenius — Yoju ressurgido | Yoju ressurgido | 6 blocos | Proposta visual |
| Primigenius — Honju | Honju | 8,5 blocos | Proposta visual |
| Primigenius — Honju revivido | Honju revivido | 9 blocos | Proposta visual |
| Preondactyl | Yoju/Honju conforme a versão e o contexto | 7 blocos de altura corporal | Proposta; asas abertas podem exceder bastante a largura/altura corporal. Não usar envergadura como altura |
| Outros Yojus já presentes no projeto | Yoju | Pendente: 4–8 blocos | Medir modelo individual; nenhum Kaiju deve ficar visualmente menor que um humano |
| Outros Honjus já presentes no projeto | Honju | Pendente: 6–15 blocos | Medir modelo individual |
| Outros Kaijus comuns não listados nominalmente | Conforme registro | Pendente | Não inventar nomes ou valores; identificar nos registros do projeto |

---

## 5. Kaijus numerados e formas especiais

**Atenção:** algumas alturas são propostas anteriores do mod, não medidas canônicas. Os N.º 3–5, N.º 7 e N.º 11–15 permanecem pendentes porque a documentação disponível não fixa alturas individuais confiáveis.

| Kaiju / forma | Altura-alvo | Status / observações |
|---|---:|---|
| Kaiju N.º 1 | 20 blocos | Proposta visual inicial; confirmar com modelo e escala desejada |
| Kaiju N.º 2 | 25 blocos | Proposta visual inicial; confirmar com modelo |
| Kaiju N.º 3 | Pendente | Medir modelo e aprovar |
| Kaiju N.º 4 | Pendente | Medir modelo e aprovar; não confundir a arma/efeitos de Kikoru com uma entidade Kaiju |
| Kaiju N.º 5 | Pendente | Medir modelo e aprovar |
| Kaiju N.º 6 | 22 blocos | Proposta visual inicial; confirmar com modelo |
| Kaiju N.º 7 | Pendente | Medir modelo e aprovar |
| Kaiju N.º 8 — forma normal de Kafka | 2 blocos | Referência citada anteriormente; verificar o modelo e a fonte antes de fixar como valor final |
| Kaiju N.º 8 — forma colossal opcional do mod | 22 blocos | Somente se existir como variante separada e deliberadamente original; não aplicar à forma normal |
| Kaiju N.º 9 — forma base | 8 blocos | Valor provisório listado em documentação anterior; confirmar se era altura visual ou outra dimensão |
| Kaiju N.º 9 — forma preta inicial | 10 blocos | Proposta visual do mod |
| Kaiju N.º 9 — fusão com Formiga | 11 blocos | Proposta original do mod; forma separada |
| Kaiju N.º 9 — fusão com N.º 10, forma pequena | **5 blocos** | **Valor solicitado pelo usuário; substituir qualquer altura anterior da variante pequena por 5 blocos** |
| Kaiju N.º 9 — fusão com N.º 10, forma grande | **20 blocos** | **Valor solicitado pelo usuário; manter a forma grande em 20 blocos** |
| Kaiju N.º 9 — forma vermelha/evoluída | 22 blocos | Proposta visual do mod |
| Kaiju N.º 9 — após absorver Isao / Numbers Weapon N.º 2 | 25 blocos | Proposta visual do mod; manter separada da fusão com N.º 10 |
| Kaiju N.º 9 — forma adaptativa final | 28 blocos | Proposta visual do mod |
| Kaiju N.º 10 — forma pequena | 4 blocos | Valor explicitamente listado na documentação de balanceamento; conferir se corresponde à variante/modelo pretendido |
| Kaiju N.º 10 — forma gigante | 24 blocos | Referência explícita na documentação de balanceamento; manter como referência atual |
| Kaiju N.º 11 | Pendente | Medir modelo e aprovar |
| Kaiju N.º 12 | Pendente | Medir modelo e aprovar |
| Kaiju N.º 13 | Pendente | Medir modelo e aprovar |
| Kaiju N.º 14 | Pendente | Medir modelo e aprovar |
| Kaiju N.º 15 | Pendente | Medir modelo e aprovar |

---

## 6. Regras específicas para a fusão N.º 9 + N.º 10

| Variante | Altura-alvo definitiva para o mod | Instrução |
|---|---:|---|
| Fusão pequena | **5 blocos** | Alterar para 5 blocos; preservar proporções e corrigir hitbox separadamente |
| Fusão grande | **20 blocos** | Manter em 20 blocos; não reduzir nem aumentar nesta tarefa |

As duas formas são **adaptações originais do mod**, não alturas oficiais do anime. Devem ter registros/modelos ou configuração de escala claramente separados. Não permitir que o valor de uma variante sobrescreva o da outra.

---

## 7. Regras globais de escala

1. Nenhum Kaiju comum deve acabar menor que um humano, salvo se for explicitamente um efeito, parte, larva ou entidade especial cuja documentação aprove isso.
2. Yoju: alvo geral de 4–8 blocos; Honju: 6–15 blocos; Daikaiju: 20–30 blocos.
3. Essas faixas são diretrizes gerais do projeto, não um substituto para as alturas específicas acima.
4. Não aplicar um único multiplicador a todos os modelos: cada modelo pode ter origem, escala de exportação e proporções diferentes.
5. Asas, chifres, caudas e armas podem ultrapassar a silhueta do corpo. Registrar separadamente altura corporal, dimensão total em pose neutra e hitbox.
6. Se uma altura proposta parecer incompatível com a aparência, não a corrigir silenciosamente. Reportar o conflito e solicitar aprovação.
7. As medidas de humanos são principalmente alvos de consistência visual; pequenas diferenças de estatura não devem prejudicar animações ou colisões.

---

## 8. Checklist de execução do Claude Code

- [ ] Encontrar todos os modelos e registros de entidades listados.
- [ ] Produzir tabela de auditoria: nome, arquivo/modelo, altura atual, tipo de valor, altura-alvo e arquivos que precisam mudar.
- [ ] Sinalizar nomes que não correspondam a nenhuma entidade/modelo encontrado.
- [ ] Aplicar apenas valores identificados com segurança; não inventar valores para pendências.
- [ ] Alterar a fusão pequena N.º 9 + N.º 10 para 5 blocos.
- [ ] Confirmar que a fusão grande N.º 9 + N.º 10 continua em 20 blocos.
- [ ] Manter o N.º 10 gigante em aproximadamente 24 blocos, salvo se o projeto atual já tiver um valor validado diferente e o relatório explicar a diferença.
- [ ] Verificar renderização, hitbox, alinhamento no chão, armas, asas/caudas e animações.
- [ ] Não alterar combate ou balanceamento fora do escopo.
- [ ] Executar build/testes disponíveis e reportar resultados reais.
- [ ] Entregar relatório final com as alterações feitas e as pendências que precisam de decisão humana.

**Prioridade de confiabilidade:** valores explicitamente escolhidos pelo usuário (como 5 e 20 blocos para as duas fusões) têm prioridade. Valores marcados como proposta continuam provisórios; pendências não devem ser preenchidas por suposição.
