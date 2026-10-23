# Kaiju No. 8 — Checklist de produção de áudio

Etapa **AUDIO-0** (2026-10-23). Lista de trabalho do áudio novo, do prompt até o arquivo aprovado. Os prompts estão
em `ELEVENLABS_PROMPTS.md`; a ordem em `AUDIO_PRIORITY_PLAN.md`. **Nenhum arquivo do jogo muda até o Miguel aprovar
os áudios gerados.**

## 0. Antes de gerar

- [ ] Miguel ouve os sons atuais que serão substituídos (`assets/kn8/sounds/`, lista da auditoria no catálogo,
  seção 3). Esta auditoria **não pôde ouvir os arquivos**: julgou pela receita de síntese do `tools/audio/gen_sounds.py`
  e pelas medições. Marcar aqui qualquer som atual que ele queira manter.
- [ ] Confirmar na interface do ElevenLabs: duração máxima por efeito, opção de laço e licença de uso comercial/gratuito
  do plano da conta (o mod é gratuito, mas a licença precisa permitir distribuição).

## 1. Gerar (por efeito)

- [ ] Prompt copiado sem mudar o texto (se mudar, anotar a versão usada no fim deste arquivo).
- [ ] Duração e "prompt influence" da entrada (0,6; 0,8 nos alarmes).
- [ ] 4 candidatos; guardar os 2 melhores quando a entrada pedir variações.
- [ ] Ouvir com fone **e** em caixa pequena: alarme não pode doer no ouvido; golpe grave tem que aparecer em caixa
  pequena (sub-grave não basta).

## 2. Tratar o arquivo

- [ ] Cortar silêncio do começo (o som tem que começar no primeiro quadro: o jogo toca no tick do evento).
- [ ] Fim com fade curto (10–50 ms) para não estalar; laços com o corte em cruzamento por zero e testados em repetição.
- [ ] **Mono** para tudo que toca no mundo (posicional); estéreo só nos de interface (alarmes no ouvido, B12, A16).
- [ ] 44,1 kHz, OGG Vorbis qualidade 5 (o Minecraft lê só Vorbis).
- [ ] Pico ≤ −1 dBFS (hoje 6 arquivos passam de 0 dB e saturam). Volume alvo:

| Categoria | Alvo |
|---|---|
| Alarmes de interface (A01, A03–A08, A10, A12–A15) | −14 a −16 LUFS |
| Sirene e cama ambiente (A02, A09) | −20 e −28 LUFS (fundo) |
| Traje (A16) e esquiva perfeita (B12) | −16 a −20 LUFS |
| Golpes corpo a corpo | −18 LUFS |
| Tiros comuns | −14 LUFS |
| Tiros especiais, explosões, canhão | −12 LUFS (a diferença tem que vir do timbre, não só do volume) |
| Habilidades de kaiju | −14 a −16 LUFS |

- [ ] Nome do arquivo: `<grupo>/<nome>_<n>.ogg` igual ao da entrada (minúsculas, sem acento, `_` entre palavras).

## 3. Revisão (Miguel)

- [ ] Cada alarme é reconhecível de olhos fechados e diferente dos outros (tabela de diferenciação na Parte A).
- [ ] Nenhum alarme tem voz ou música.
- [ ] Tiro especial claramente diferente do tiro comum da mesma arma (B07 × B05, B08 × B06, C-MIN-03b × C-MIN-01).
- [ ] Thunder Emperor sem som de eletricidade (decisão do Miguel).
- [ ] Aprovado → marcar a linha da tabela da seção 5.

## 4. Implementação (etapa seguinte, depois da aprovação)

- [ ] Arquivo em `src/main/resources/assets/kn8/sounds/<grupo>/`.
- [ ] Entrada em `assets/kn8/sounds.json` com `subtitle` e, se preciso, `attenuation_distance`; `stream: true` nos laços
  longos (A02, A09).
- [ ] Legenda `subtitles.kn8.<grupo>.<nome>` em `en_us.json` e `pt_br.json`.
- [ ] Registro do evento (`KN8Sounds` para os do código; os de dados podem ser só pelo id no JSON).
- [ ] Ligado no evento certo (seção 3 do catálogo, coluna "Local") e **não** na animação, salvo os cosméticos.
- [ ] Som antigo removido do código/JSON; o `.ogg` antigo apagado só se nada mais o usar
  (`grep -r "<id>" src/`).
- [ ] Teste: servidor dedicado + 2 clientes; os dois ouvem o som no mesmo momento do efeito visual; alarme começa e
  para com a fase; nada de cópias sobrepostas.

## 5. Acompanhamento

Status: ☐ a gerar · ◐ gerado · ✔ aprovado · ⇄ implementado. Uma linha por código de `ELEVENLABS_PROMPTS.md`.

| Código | Arquivo | P | Gerado | Aprovado | Implementado |
|---|---|---|---|---|---|
| A01 | alarm/invasion_start | P1 | ☐ | ☐ | ☐ |
| A02 | alarm/siren_loop | P1 | ☐ | ☐ | ☐ |
| A03 | alarm/kaiju_detected | P1 | ☐ | ☐ | ☐ |
| A04 | alarm/class_yoju | P1 | ☐ | ☐ | ☐ |
| A05 | alarm/class_honju | P1 | ☐ | ☐ | ☐ |
| A06 | alarm/class_daikaiju (reservado) | P4 | ☐ | ☐ | ☐ |
| A07 | alarm/class_numbered | P1 | ☐ | ☐ | ☐ |
| A08 | alarm/critical_threat | P1 | ☐ | ☐ | ☐ |
| A09 | alarm/ongoing_bed | P1 | ☐ | ☐ | ☐ |
| A10 | alarm/threat_rise | P1 | ☐ | ☐ | ☐ |
| A11 | alarm/proximity (reservado) | — | ☐ | ☐ | ☐ |
| A12 | alarm/wave_cleared | P1 | ☐ | ☐ | ☐ |
| A13 | alarm/all_clear | P1 | ☐ | ☐ | ☐ |
| A14 | alarm/invasion_lost | P1 | ☐ | ☐ | ☐ |
| A15 | alarm/boss_phase | P2 | ☐ | ☐ | ☐ |
| A16 | suit/overheat_warning | P2 | ☐ | ☐ | ☐ |
| A17 | alarm/comm_failure (reservado) | — | ☐ | ☐ | ☐ |
| A18 | alarm/evacuation (reservado, PM-10) | — | ☐ | ☐ | ☐ |
| B01–B02 | weapon/axe_seismic_* | P3 | ☐ | ☐ | ☐ |
| B03 | weapon/sword_dash_cut | P3 | ☐ | ☐ | ☐ |
| B04 | weapon/knife_flurry | P3 | ☐ | ☐ | ☐ |
| B05–B06 | weapon/rifle_shot, pistol_shot (refazer) | P3 | ☐ | ☐ | ☐ |
| B07–B08 | weapon/*_precise_shot | P2 | ☐ | ☐ | ☐ |
| B09–B11 | weapon/*_reload, magazine_load | P4 | ☐ | ☐ | ☐ |
| B12 | combat/perfect_dodge | P3 | ☐ | ☐ | ☐ |
| C-HOS-01..10 | hoshina/* | P2/P3 | ☐ | ☐ | ☐ |
| C-KIK-01..12 | kikoru/*, kikoru_no4/* | P3 | ☐ | ☐ | ☐ |
| C-MIN-01..08 | mina/* | P2 | ☐ | ☐ | ☐ |
| C-REN-01..11 | reno/*, reno_no6/* | P2 | ☐ | ☐ | ☐ |
| C-NAR-01..08 | narumi/*, narumi_no1/* | P2/P3 | ☐ | ☐ | ☐ |
| C-ISA-01..07 | isao/* | P2/P3 | ☐ | ☐ | ☐ |
| C-K8-01..10 | no8/*, kafka/* | P2/P3 | ☐ | ☐ | ☐ |
| K00 | kaiju/heavy_windup | P3 | ☐ | ☐ | ☐ |
| D-PRI-01..10 | kaiju/primigenius/* | P3/P4 | ☐ | ☐ | ☐ |
| D-ARA-01..06, D-FOR-01..02 | kaiju/spider/*, kaiju/ant/* | P3/P4 | ☐ | ☐ | ☐ |
| D-PHI-01, D-DIC-01..03, D-FUN-01..03 | kaiju/philinosoma, diclonius, fungus | P3 | ☐ | ☐ | ☐ |
| D-PRE-01..05 | kaiju/preondactyl/* | P3 | ☐ | ☐ | ☐ |
| D-N9-01..15 | kaiju/no9/* | P2/P3 | ☐ | ☐ | ☐ |
| D-N10-01..07, D-N13-01..05 | kaiju/no10/*, kaiju/no13/* | P3/P4 | ☐ | ☐ | ☐ |

## 6. Versões de prompt alteradas

(anotar aqui código, data e o texto usado quando o prompt original não servir)
