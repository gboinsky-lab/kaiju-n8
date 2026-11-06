# kn8 1.0.3 — download

1. Baixe o branch como .zip (botão verde "Code" > "Download ZIP") ou cada parte pelo botão de download.
2. Extraia o zip ("Extrair tudo") e, dentro da pasta extraída, abra o Prompt de Comando (digite `cmd` na barra de
   endereço) e junte:
   - Windows: `copy /b kn8-1.0.3.jar.part0 + kn8-1.0.3.jar.part1 kn8-1.0.3.jar`
   - Linux/macOS: `cat kn8-1.0.3.jar.part0 kn8-1.0.3.jar.part1 > kn8-1.0.3.jar`
3. Confira (opcional): 170.878.825 bytes, SHA-256 `6aefc60065531e73ddc63a07982766c0ad657eb84893d1da042384d756b7bdc4`
   (Windows: `certutil -hashfile kn8-1.0.3.jar SHA256`).
4. Ponha o `kn8-1.0.3.jar` na pasta `mods` (NeoForge 21.1.252 para o Minecraft 1.21.1), no lugar do 1.0.2, junto com
   a GeckoLib 4.7.5+ e a Player Animation Library 1.1.5+.

Novo na 1.0.3: passivas das fusões do No. 9 (Pressão Abissal, Consciência Expandida), couraça do Couraçado cedendo
abaixo de 65%, tentáculos do Neural que puxam e controle mental (roteiro §152).
