# Roteiro de teste geral — versão 1.0.0

Um passeio pelo mod inteiro, para conferir a versão antes de distribuir. Não substitui os roteiros de cada etapa
(`docs/ROTEIRO_TESTE_0_1_B.md`, §1 a §148): quando um passo falhar, o § indicado tem o detalhe.

**Preparação:** servidor dedicado (`./gradlew runServer` ou o jar em um servidor NeoForge 21.1.252 com GeckoLib e a
Player Animation Library) e **2 clientes** (Dev1 em pt_br, Dev2 em en_us). Mundo **normal novo** (não plano: as
cidades e Ariake não cabem no plano). Dev1 operador. Marque cada linha com OK / falhou (e o que viu).

## 1. Início e dados

| # | Passo | Esperado | § |
|---|---|---|---|
| 1.1 | Subir o servidor e entrar com os 2 clientes | Sem erro do `kn8` no log; linha `[kn8] ... carregado` sem arquivos com erro | 148 |
| 1.2 | `/reload` | Mesma contagem, sem erro | 148 |
| 1.3 | Abrir um save de uma versão anterior | Abre; carreira, Release e esquadrão continuam | 148 |
| 1.4 | Os dois clientes de pé por 5 min | Nenhum cai (`data_sync` grande) | 143 |

## 2. Jogador

| # | Passo | Esperado | § |
|---|---|---|---|
| 2.1 | **M** (menu): todas as abas, Q/E troca | Perfil, Missões, Alertas, Esquadrão, Bestiário, Arsenal, Base sem texto cru (`kn8.`) nos dois idiomas | 12 |
| 2.2 | Sem traje, segurar **G** | Aviso de que precisa de traje; Release 0 | 25 |
| 2.3 | Vestir o Mk1, **G** até o limite e acima, **Shift + G** | Aura sobe; acima do limite aquece; ao voltar, fadiga | 25, 108 |
| 2.4 | Perfil: botão da aura (esquerdo/direito) | Cor troca e o Dev2 vê a cor nova | 130 |
| 2.5 | Faca, espada, machado, rifle, pistola: combo, pesado, carregado, **V**, **Z**, **R** | Animações, sons, pente e recarga; esquiva perfeita no tempo certo | 29, 86 |
| 2.6 | Espada do Hoshina com as duas: **X** e **R** nas 7 formas | HUD "n/7", recarga por técnica; Jūni-hitoe recusado sem o Numbers 10 | 110, 114 |
| 2.7 | Machado no alto de um prédio, pular sobre um kaiju e **R** no Falling Thunder | Mergulha e esmaga com o raio; no chão recusado | 112 |
| 2.8 | Minigun da Rin segurando o botão | Atira, superaquece, esfria | 145 |
| 2.9 | **C**: vault, escalar parede alta, borda automática, salto longo entre telhados | Sobe e passa por cima; pendurado anda com A/D | 41, 118 |
| 2.10 | Cair 25 blocos de traje e sem traje | Sem dano com traje; dano sem traje | 58 |
| 2.11 | Suporte de pesos, escalada, treino com soldado (Shift + clique de mão vazia) | XP do corpo; treino sem morte, vencedor ganha | 119 |

## 3. Carreira e base

| # | Passo | Esperado | § |
|---|---|---|---|
| 3.1 | `/locate structure kn8:exam_ground`, ir até lá, aceitar o exame com o Oficial de Provas | Kit entregue, 4 candidatos, prova concluída, nota e promoção; o aprovado fica com o kit | 120, 130 |
| 3.2 | Missão de campo numa cidade | Alvos dentro da cidade, rastreador, avaliação S a D | 92, 121 |
| 3.3 | Evacuação | Civis seguem e fogem do kaiju; extração conta; perdas demais fracassam | 98 |
| 3.4 | Rádio de Campo em missão: reconhecimento, suprimentos, bombardeio | Cada um uma vez; fora de operação recusa | 99 |
| 3.5 | Esquadrão: recrutar, Seguir, Ficar, Focar, Guarda, Recuar | Ordens obedecidas; especial só de Vice-Capitão para cima | 49, 91 |
| 3.6 | Bancada, pesquisa no terminal, estação de preparação | Fabricação pela patente/pesquisa; pesquisa termina sozinha; estação cura e reabastece | 16, 93, 95 |
| 3.7 | Núcleo da Base: fundar, construir os 5 módulos, demolir | Módulos no lugar; Comando com os oficiais | 96 |
| 3.8 | Arma numerada sem os requisitos e depois com eles | Recusa com a mensagem; domínio sobe | 100, 101 |

## 4. Kaiju e soldados

| # | Passo | Esperado | § |
|---|---|---|---|
| 4.1 | `/kn8 kaiju spawn` de um Yoju, um Honju e um numerado perto de soldados | Lutam; aviso no chão do golpe pesado; carcaça e desmonte | 50, 59 |
| 4.2 | Golpear a perna de um Primigenius até ferir | "FERIDO: Pernas" na barra; mais lento | 87 |
| 4.3 | No. 9 contra Hoshina e Kikoru até pouca vida | Tenta se retirar; pego, morre | 71 |
| 4.4 | No. 10 pequeno até 50% | Vira o gigante no lugar | 23, 52 |
| 4.5 | No. 14 contra especiais | Escudo alterna, teleporta | 142 |
| 4.6 | No. 15 e No. 1 contra o jogador e especiais | Ilusão, ataque mental; o No. 1 esquiva o golpe repetido e erra na finta | 146 |
| 4.7 | Myxogasterocarp com pouca vida | Para, divide em 2 Phaneroplasmodium; golpe no núcleo interrompe | 146 |
| 4.8 | Kaiju sobre um prédio de uma base | Desce quebrando | 52 |
| 4.9 | Alguns especiais novos (Rin, Tae, Ryo, Narumi, Haruichi) contra um Honju | Marcas, veneno, escudo e anel amarelo | 145 |

## 5. Hospedeiro

| # | Passo | Esperado | § |
|---|---|---|---|
| 5.1 | `/kn8 host spawn Dev1` | Larva circula e infecta; título e náusea | 103 |
| 5.2 | **B** perto de soldados (Candidato) | Forma de 3 m; soldados atiram; mérito cai | 104, 128 |
| 5.3 | Lutar até os 2 min (`/kn8 host rampage Dev1`) | Sem controle, vermelho, ataca em volta; volta exausto | 105 |
| 5.4 | **Shift + B** de traje | Braço do No. 8 por 5 s; Release travado em 0,1%–1% | 129 |

## 6. Mundo e invasões

| # | Passo | Esperado | § |
|---|---|---|---|
| 6.1 | `/locate structure #kn8:defense_sites` e visitar Ariake e Tachikawa | Base grande com a cidade ao norte e bairros; nome na tela; guarnição | 123, 125 |
| 6.2 | Base atingida (pode exigir mais de um mundo) e missão pós-ataque | Posto de campanha; alvos nas ruínas | 124, 126 |
| 6.3 | `/kn8 invasion start kn8:tachikawa_raid ~ ~ ~` com os 2 jogadores | Sirene em laço, ondas, ameaça grande, vitória com recompensa; sem travar o servidor | 132, 148 |
| 6.4 | Durante a invasão: aba Alertas e faixa da região | COMBATE, depois RECUPERAÇÃO | 97 |
| 6.5 | Rastreador nas pistas de destruição | Pistas e direção; mérito uma vez | 97 |

## 7. Visual, som e desempenho

| # | Passo | Esperado | § |
|---|---|---|---|
| 7.1 | `particleLevel` LOW/MEDIUM/HIGH numa luta grande | Menos efeitos no LOW; FPS sobe | 85 |
| 7.2 | `[fx] modSoundVolume` 0,35 e 1,0 | Volume dos sons do mod muda | 108 |
| 7.3 | `/tick query` durante 6.3 | Abaixo de 50 ms por tick | 148 |

## Critério de pronto da 1.0.0

Nenhum erro do `kn8` no log, nenhum cliente caindo, todas as linhas OK ou com o problema anotado e decidido.
