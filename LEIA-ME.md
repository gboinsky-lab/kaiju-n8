# kn8 1.0.6 — download

1. Baixe o branch como .zip (botão verde "Code" > "Download ZIP") ou cada parte pelo botão de download.
2. Extraia o zip ("Extrair tudo") e, dentro da pasta extraída, abra o Prompt de Comando (digite `cmd` na barra de
   endereço) e junte:
   - Windows: `copy /b kn8-1.0.6.jar.part0 + kn8-1.0.6.jar.part1 kn8-1.0.6.jar`
   - Linux/macOS: `cat kn8-1.0.6.jar.part0 kn8-1.0.6.jar.part1 > kn8-1.0.6.jar`
3. Confira (opcional): 170889394 bytes, SHA-256 `82bd2cb91ff727458cbb4acb89a7f15076045083ed339129ce0694c54b47009c`
   (Windows: `certutil -hashfile kn8-1.0.6.jar SHA256`).
4. Ponha o `kn8-1.0.6.jar` na pasta `mods` (NeoForge 21.1.252 para o Minecraft 1.21.1), no lugar do 1.0.5, junto com
   a GeckoLib 4.7.5+ e a Player Animation Library 1.1.5+.

Novo na 1.0.6: o No. 2 e o No. 6 passam pela guarda contra golpe pesado e pela guarda da cauda, e batem mais forte em
soldados especiais (x2,5 e x3,0; jogador e soldado comum iguais). O Hoshina No. 10 agora perde para os dois e continua
vencendo o No. 10 gigante (roteiros §155 e §156). Inclui tudo da 1.0.5.
