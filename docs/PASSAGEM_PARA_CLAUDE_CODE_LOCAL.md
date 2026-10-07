# Passagem para o Claude Code no PC (2026-10-06)

Resumo do estado do projeto para continuar no Claude Code rodando no PC do Miguel (terminal, app desktop ou
IntelliJ). A memória completa do projeto continua no `CLAUDE.md`, que o Claude Code lê sozinho. Este arquivo
cobre só o que muda na troca de ambiente e o que vem a seguir.

## 1. Onde está o código

| Item | Valor |
|---|---|
| Repositório | `gboinsky-lab/kaiju-n8` (GitHub) |
| Branch com todo o trabalho | `claude/laughing-maxwell-tbl62q` (último commit: armas maiores na mão) |
| Pacote zip | `kn8-atualizacao-0.2.zip`: o mesmo conteúdo da branch, **sem** `build.gradle`/`gradle.properties`/`settings.gradle` |

Para trazer para o PC, use um dos dois caminhos:

- **Git:** `git fetch origin` e depois `git checkout claude/laughing-maxwell-tbl62q`. Depois, se quiser, junte na
  sua branch principal.
- **Zip:** extraia por cima da pasta `kn8-main`, **mantendo os seus** `build.gradle` e `gradle.properties`. Os
  seus é que estão certos: PAL `transitive=false`, run `clientJoin`, repo Modrinth e bloco do Better Combat.

## 2. Preparar o PC (uma vez)

| O que | Para quê | Como |
|---|---|---|
| JDK 21 | Compilar e rodar | Já existe em `~/.gradle/jdks/eclipse_adoptium-21-amd64-windows.2`. O `java` do PATH é o 8, então use `JAVA_HOME` apontando para o 21 (ou o JDK 21 no IntelliJ) |
| Python 3.12+ | Só os scripts de `tools/` (arte e som) | python.org (marque "Add to PATH") |
| Pacotes Python | Idem | `pip install numpy pillow trimesh fast-simplification scipy nbtlib` |
| ffmpeg | Gerar os sons (`.ogg`) | `winget install ffmpeg` |
| GLBs do Meshy | Converter modelos | Pasta fora do Git, com os nomes de `tools/art/meshy_assets.json` |

Não é preciso Xvfb, RCON nem o `build.gradle` provisório: eram truques da nuvem para jogar sem tela.

## 3. Comandos

```
gradlew build                    # compila + JUnit (70 testes)
gradlew runGameTestServer        # GameTests (31)
gradlew runServer                # servidor dedicado
gradlew runClient / runClient2   # Dev1 e Dev2 (todo teste manual é com os 2)
python tools/audio/gen_sounds.py [nome ...]          # sons (sem nome = todos)
python tools/art/meshy_convert.py --src <pasta GLB>  # converte modelos do Meshy
python tools/art/meshy_convert.py --display-only     # só refaz o tamanho/posição das armas na mão
python tools/art/rig_primigenius_mesh.py <espécie>   # rig dos Primigenius (tabela SPECIES)
```

## 4. Estado (2026-10-07: tudo compila; JUnit e GameTests 41/41)

A super atualização 0.2 está na branch: Etapas 2 (carreira, Release por treino, boneco), 3 (bancada, trajes,
suprimentos), 5 (missões), 6 (chefes), 7 (invasões; kaiju não nascem mais sozinhos), 8 (Kaiju No. 9) e as
construções (posto avançado, prédio destruído, restos de kaiju, torre de vigia). Cada uma foi vista em jogo na
nuvem; os roteiros §15–§19 de `docs/ROTEIRO_TESTE_0_1_B.md` ficam para o Miguel.

Protocolo de rede `"9"`: servidor e clientes precisam estar na mesma versão.
Construções: `python tools/world/gen_structures.py` (sem dependências) regera os `.nbt` e o worldgen.

## 5. O que vem a seguir

1. Miguel testa os roteiros §13–§19 com 2 clientes e aprova (ou pede ajustes).
2. Depois da 0.2: M12 transformação, NPCs e base (M13/M17), Trichonephila Honju (modelo a pedir no Meshy), mais
   kaiju numerados e Daikaiju, soldados especiais.

## 6. Primeira mensagem sugerida no Claude Code do PC

> Leia o CLAUDE.md e o docs/PASSAGEM_PARA_CLAUDE_CODE_LOCAL.md. Estou continuando no PC o trabalho da branch
> claude/laughing-maxwell-tbl62q. Rode o build e os GameTests para confirmar que está tudo certo aqui e me diga o
> estado.

## 7. Dicas que valem no PC também

- **Finais de linha:** vários arquivos `.java` e `.json` estão em CRLF. Edite mantendo o CRLF, senão o diff fica
  com o arquivo inteiro.
- **Vorbis:** regerar um `.ogg` muda os bytes mesmo sem mudar o som. Regere só os sons alterados
  (`gen_sounds.py nome1 nome2`).
- **Testar som sem ouvir:** ligue as legendas (Opções → Acessibilidade); cada som do kn8 tem legenda própria.
