# Kaiju No. 8 — Plano de prioridade e implementação do áudio

Etapa **AUDIO-0** (2026-10-23): planejamento. **Nenhum código mudou.** A implementação começa depois que os áudios
gerados no ElevenLabs forem aprovados pelo Miguel (checklist, seção 3).

## 1. Ordem de produção

| Fase | Conteúdo | Códigos | Por quê primeiro |
|---|---|---|---|
| **S1 — Alarmes de invasão** | abertura, sirene em laço, detectado, alertas por classe, ameaça crítica, ameaça subiu, onda eliminada, vitória, derrota, cama da luta | A01–A05, A07–A10, A12–A14 | Pedido do Miguel; hoje é uma sirene sintetizada repetida a cada 5 s, e vitória/derrota/onda não têm som próprio |
| **S2 — Tiros e disparos especiais** | tiro preciso (rifle/pistola), canhão da Mina (todos), tiros e gelo do Reno/No. 6, tiro da baioneta, ondas do Isao, Kūuchi, Finger Gun/Cannon, raios de energia, sopro atômico, golpe no chão do No. 8 | B07, B08, C-MIN-*, C-REN-*, C-NAR-05, C-ISA-05/06, C-HOS-01, C-K8-05, D-N9-01..04/09, D-N10-01, D-PRI-05, D-DIC-*, D-PRE-01 | Hoje os tiros dos especiais soam como espada e os raios de kaiju como rugido, e só no impacto |
| **S3 — Ataques especiais corpo a corpo** | técnicas do Hoshina, Kikoru, Narumi, Isao, No. 8; socos (hoje com som de machado); especiais do jogador (faca, espada, machado); esquiva perfeita; preparo genérico de kaiju; ácido, teia, espinhos, esporos | C-HOS, C-KIK, C-NAR, C-ISA, C-K8, B01–B04, B12, K00, D-FOR, D-ARA-04/05, D-PHI, D-FUN | Identidade de cada personagem |
| **S4 — Resto** | golpes corpo a corpo de kaiju, tiros comuns refeitos, recargas, eventos do No. 9, autodestruição, Full Release, A06/A11/A15–A18 | demais | Já existe som próprio razoável ou a mecânica ainda não existe |

## 2. O que já dá para trocar só com dados (sem código)

A maior parte das substituições de arma e de habilidade **não precisa de Java**: o id do som já vem do JSON.

| Onde | Campo | Exemplo |
|---|---|---|
| `weapon/<arma>.json` | `sounds.swing/heavy/hit/shot` | socos do No. 8/Isao (`axe_*` → `no8/fist_*`, `isao/fist_*`), baioneta, canhão da Mina (`rifle_shot` → `mina/cannon_shot`) |
| `weapon/<arma>.json` | `special.sound` | faca (`knife_flurry`), espada (`sword_dash_cut`), espada do Hoshina (`hoshina/kuuchi_release`), machado |
| `weapon_profile/<perfil>.json` | `draw.sound`, `reload.stages[].sound` | recargas (besta vanilla → `weapon/rifle_reload_*`) |
| `ability/<habilidade>.json` | `sound` (toca no **impacto**) | as 58 habilidades de kaiju (bite/slam/roar/pistol → id próprio) |

Basta o `.ogg` + a entrada no `sounds.json` + a legenda. Mas só o impacto: preparo, saída e voo precisam do código
da seção 4.

## 3. Alarmes contínuos — início e fim sem cópias

Situação atual (código): `InvasionService.siren` toca `kn8:alert.siren` com `playNotifySound` em cada jogador a até
2× o raio, no início e a cada `SIREN_INTERVAL` (100 ticks) nas fases WARNING e BREAK. Não há laço: são cópias de 3 s.

Plano:

1. **Laço no cliente, guiado pelo estado que já é sincronizado.** O `InvasionStateS2C` (público, enviado na mudança e
   no login/respawn/troca de dimensão: regra 8) já leva `phase`, invasão, centro, onda e nível. Um
   `client/audio/InvasionAlarmClient` (novo) recebe cada estado e decide:
   - fase WARNING ou BREAK → garante **uma** instância de `A02 siren_loop`;
   - fase FIGHT → para a sirene (fade 1,5 s) e garante uma instância de `A09 ongoing_bed`;
   - `InvasionStateS2C.NONE` (fim) → para tudo com fade;
   - saída do servidor (`ClientPlayerNetworkEvent.LoggingOut`) → para tudo sem fade.
2. **Instância única:** um campo por laço com a instância atual (`AbstractTickableSoundInstance` própria, posicional
   no centro da invasão, `looping = true`). Antes de criar, se a atual ainda está ativa
   (`SoundManager.isActive`), não cria outra. A instância se encerra sozinha (`stop()`) quando o estado guardado não é
   mais a fase dela: mesmo que um pacote se perca, ela não fica tocando sem invasão (o `tick()` dela confere o estado
   atual a cada tick).
3. **Uma invasão por dimensão** (regra atual): trocar de dimensão manda `NONE` e para os laços.
4. **Volume para quem está longe:** a instância segue o centro; quem está dentro da área ouve; quem está a mais de 2×
   o raio não ouve (como hoje). Opcional (A02d): camada distante.
5. **Os toques pontuais ficam no servidor**, como já são (com `playNotifySound` para os jogadores da área), nos
   mesmos lugares do código: A01/A03 no `start`; A04–A07 + A08 + A10 no `startWave` (escolha pela maior classe entre
   os kaiju da onda, chefe e surpresa); A12 no fim de onda; A13/A14 no `finish`. Cada um toca uma vez por evento, então
   não acumula.
6. **Remover** a repetição do `SIREN_INTERVAL` (o laço do cliente a substitui). O som `kn8:alert.siren` deixa de
   ser usado.

Sem pacote novo e sem mudar o protocolo (continua "22"), porque tudo o que o cliente precisa já está no
`InvasionStateS2C`. [VERIFICAR na implementação: o `InvasionStateS2C.NONE` chega em todos os fins (vitória, derrota,
`/kn8 invasion stop`) — o `finish` manda para a dimensão inteira, conferido no código.]

## 4. Ataques e tiros especiais — sincronização

### 4.1 Registro de sons por perfil

O `AbilityDef` e o `Technique` já têm 16 campos (limite do `RecordCodecBuilder`), então os sons por etapa vão num
registro novo, como o `vfx_profile`:

```
data/kn8/kn8/sound_profile/<id>.json      (id = espécie de kaiju ou perfil de soldado especial)
{
  "abilities":  { "<habilidade>": { "windup": "kn8:...", "release": "kn8:...", "travel": "kn8:...",
                                   "impact": "kn8:...", "aftermath": "kn8:...", "hit": "kn8:..." } },
  "techniques": { "<tecnica>":   { mesmos campos } },
  "events":     { "transform": "kn8:...", "regenerate": "kn8:...", "tail_guard": "kn8:...", ... }
}
```

Sem arquivo ou sem a etapa, fica o som de hoje (o `sound` do JSON da habilidade, o `sounds` da arma). Validação: id
de som inexistente gera aviso no log e não derruba o servidor (regra 9).

### 4.2 Onde cada etapa toca (servidor decide)

| Etapa | Kaiju | Soldado especial | Jogador |
|---|---|---|---|
| Preparo/carga | `KaijuTelegraph.start` / `startAbility` (substitui o Warden) | `HoshinaEntity.startTechnique` (no lugar do `sword_swing` fixo; mesmo momento do VFX `_charge`) | `SpecialAttacks` (início do especial) |
| Execução (disparo/golpe) | `KaijuAbilities.fire` (projétil sai) ou tick de impacto da habilidade | `HoshinaEntity.fireHit`/`strike` no tick do golpe do JSON (no lugar do `sword_heavy` fixo) | tick de impacto do especial; tiro preciso no `CombatService` |
| Voo | som preso ao `KaijuProjectile` no cliente | som preso ao `SlashProjectile` no cliente (pelo dado `TRAIL` que já é sincronizado: "perfil\|chave") | — |
| Impacto | `AbilityEffects.play` (já é no impacto) | acerto no `strike`; acerto do `SlashProjectile` | acerto do golpe |
| Consequência | explosão do `KaijuProjectile`/autodestruição | `SlashProjectile` ao explodir (no lugar do `generic.explode`) | — |

Regras:

- **Nada de som de disparo no `triggerAnim`.** A animação começa no preparo; o tiro sai no tick do JSON. O som segue o
  tick.
- **Cancelamento:** se a técnica é cancelada (`cancelTechnique`: alvo morto, tiro inválido, saiu do alcance), a
  carga longa (C-MIN-03a, C-MIN-04a, C-ISA-07a, C-REN-07a, D-DIC-01) é parada para os jogadores por perto
  (`ClientboundStopSoundPacket` pelo id da carga). Como cada carga tem id próprio, só ela para. Cargas curtas (≤ 1 s)
  terminam sozinhas. [VERIFICAR: se dois personagens iguais carregarem ao mesmo tempo, parar um para os dois; aceitável
  pela raridade ou trocar por som preso à entidade no cliente.]
- **Voo:** só para os marcados (raios contínuos, bomba de esporos, Anti-Giant, espiral do Isao, lança de gelo). Som
  preso à entidade (`EntityBoundSoundInstance` em laço), que para sozinho quando a entidade some. O
  `KaijuProjectile` já sincroniza o id da habilidade (`ABILITY`, conferido no código) e o `SlashProjectile` o dado
  `TRAIL`: o cliente sabe qual som de voo tocar sem pacote novo (o `sound_profile` precisa ir ao cliente, como o
  `vfx_profile`, ou só a tabela de voo).
- **Combos e rajadas:** um som por golpe, variação sorteada pelo `sounds.json` (várias entradas) e tom variando pelo
  código (como hoje).
- **Efeito visual e áudio no mesmo tick:** o som toca na mesma linha do `VfxService.play` correspondente (`_charge`
  com a carga, `<tecnica>/n` com o golpe, `_impact`/`_explosion` com o impacto). Teste: gravar os dois clientes e
  conferir que o som e o desenho chegam juntos.

## 5. Ligação de cada alarme a eventos que existem

| Alarme | Evento existente | Estado |
|---|---|---|
| A01 Invasão iniciada | `InvasionService.start` | existe |
| A02 Sirene | fases WARNING/BREAK | existe (hoje por cópias) |
| A03 Kaiju detectado | mensagem `kn8.invasion.alert` no `start` | existe |
| A04 Yoju / A05 Honju / A07 Numerado | `startWave`: maior `KaijuClass` entre os kaiju da onda | existe (a classe está no `kaiju/*.json`) |
| A06 Daikaiju | `startWave` com Daikaiju | **nenhuma espécie é Daikaiju hoje**: fica pronto, sem uso |
| A08 Ameaça crítica | onda com `boss`, ou invasão de nível 5–6 | existe |
| A09 Em andamento | fase FIGHT | existe |
| A10 Ameaça subiu | `startWave` com `wave > 0`; surpresa (`chance` < 1) que entrou | existe |
| A11 Aproximação | — | **não existe**: precisaria de verificação de distância kaiju × área protegida (`ProtectedAreas`, locais C3) ou jogador; [DECIDIR] se vale criar |
| A12 Onda eliminada | `wave_cleared` | existe |
| A13 Vitória / A14 Derrota | `finish` (VICTORY / DEFEAT por tempo) | existe |
| A15 Fase do chefe | `BossService.tick` (PM-5) | existe |
| A16 Traje superaquecendo | `PowerService` | existe |
| A17 Falha de comunicação | — | **não existe**; sem uso |
| A18 Evacuação | — | **não existe** (PM-10: civis e evacuação) |

## 6. Volume e distância

- Alarmes de interface: no ouvido (`playNotifySound`), sem posição; mínimo de efeitos simultâneos (um toque por
  evento).
- Sirene e cama: posicionais no centro da invasão, alcance de até 96 blocos, sempre abaixo dos sons de combate.
- Tiros especiais e explosões: alcance pelo volume da chamada (16 blocos × volume) ou `attenuation_distance` no
  `sounds.json`: tiro preciso 48–64, canhão 80, Anti-Giant 128.
- Kaiju: já escala pelo tamanho do corpo; manter.
- Config de cliente opcional `[audio] alarmVolume` (0–1) para quem achar os alarmes altos [SUPOSIÇÃO].

## 7. Riscos

- **Licença do ElevenLabs** para distribuir no mod (confirmar no plano da conta antes de colocar no repositório).
- **Tamanho do pacote:** ~200 arquivos novos; alvo ≤ 15 MB de áudio (mono, Vorbis q5, curtos). Laços longos com
  `stream: true`.
- **Fadiga auditiva:** a sirene não pode tocar a luta inteira em volume alto: por isso a sirene só no aviso e entre
  ondas, e a cama baixa na luta.
- **Sons de kaiju gigante:** o tom mais grave pelo código pode embolar arquivos que já são graves; conferir em jogo
  com o No. 10 gigante e a fusão gigante.

## 8. [DECIDIR] (Miguel)

1. A11 alarme de aproximação: criar a mecânica (kaiju a menos de N blocos de uma área protegida/local C3 ou do
   jogador)?
2. O rugido do kaiju no início da onda fica por baixo do alerta de classe ou sai?
3. A sirene: posicional no centro (realista, some longe) ou no ouvido de quem está na área (como hoje)?
4. Ordem S1 → S4 está boa, ou os tiros especiais (S2) vêm antes dos alarmes?

## 9. Próximas etapas

1. **AUDIO-1:** Miguel gera os áudios S1 (alarmes) no ElevenLabs com os prompts, aprova; implementação dos alarmes
   (seção 3) e testes com servidor dedicado + 2 clientes.
2. **AUDIO-2:** S2 (tiros especiais) — dados + registro `sound_profile` + ganchos de preparo/saída/voo.
3. **AUDIO-3:** S3; **AUDIO-4:** S4.
