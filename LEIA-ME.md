# kn8 1.0.5 — download

1. Baixe o branch como .zip (botão verde "Code" > "Download ZIP") ou cada parte pelo botão de download.
2. Extraia o zip ("Extrair tudo") e, dentro da pasta extraída, abra o Prompt de Comando (digite `cmd` na barra de
   endereço) e junte:
   - Windows: `copy /b kn8-1.0.5.jar.part0 + kn8-1.0.5.jar.part1 kn8-1.0.5.jar`
   - Linux/macOS: `cat kn8-1.0.5.jar.part0 kn8-1.0.5.jar.part1 > kn8-1.0.5.jar`
3. Confira (opcional): 170887641 bytes, SHA-256 `0aa9e94caf81e285f9091d4f54582b69bf7d388d56c50969268fae9fbd496263`
   (Windows: `certutil -hashfile kn8-1.0.5.jar SHA256`).
4. Ponha o `kn8-1.0.5.jar` na pasta `mods` (NeoForge 21.1.252 para o Minecraft 1.21.1), no lugar do 1.0.4, junto com
   a GeckoLib 4.7.5+ e a Player Animation Library 1.1.5+.

Novo na 1.0.5: os quatro tentáculos do No. 9 Neural têm ossos próprios (balançam parados e andando, chicoteiam no
golpe de tentáculos, alternam um lado por golpe no Domínio; roteiro §154) e o aviso de troca de forma (No. 10
gigante, No. 15, formas do No. 9) agora aparece também para quem está em espectador. Inclui tudo da 1.0.4.
