# kn8 1.0.2 — download

1. Baixe o branch como .zip (botão verde "Code" > "Download ZIP") ou cada parte pelo botão de download.
2. Extraia o zip ("Extrair tudo") e, dentro da pasta extraída, abra o Prompt de Comando (digite `cmd` na barra de
   endereço) e junte:
   - Windows: `copy /b kn8-1.0.2.jar.part0 + kn8-1.0.2.jar.part1 kn8-1.0.2.jar`
   - Linux/macOS: `cat kn8-1.0.2.jar.part0 kn8-1.0.2.jar.part1 > kn8-1.0.2.jar`
3. Confira (opcional): 170.872.320 bytes, SHA-256 `5c168df4413131bc928e37a88f051ee4f5fc9211187a06679ff6d59ba7ea4210`
   (Windows: `certutil -hashfile kn8-1.0.2.jar SHA256`).
4. Ponha o `kn8-1.0.2.jar` na pasta `mods` (NeoForge 21.1.252 para o Minecraft 1.21.1), no lugar do 1.0.1, junto com
   a GeckoLib 4.7.5+ e a Player Animation Library 1.1.5+.
