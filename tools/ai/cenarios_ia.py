"""Cenarios de IA (plano de IA §15.1) no servidor dedicado: regressao das fases de IA (Fase 0 em diante).
Precisa do servidor rodando com RCON (porta 25575, senha kn8test). Usa a area x/z 1960-2041 (chunks forcados);
limpa a area em 4 partes (o /fill tem limite de 32.768 blocos) e mata carcacas entre cenarios.
Mede por entidade: distancia andada, janelas paradas (3 s andando < 0,5 bloco com inimigo vivo a mais de
5 blocos), vida perdida, tempo ate o primeiro dano e (cenario parede) dano recebido sem linha de visao.
Uso: python3 tools/ai/cenarios_ia.py <cenario> [segundos]"""
import socket, struct, sys, time, re, json, math
def pkt(i,t,b): d=struct.pack('<ii',i,t)+b.encode()+b'\0\0'; return struct.pack('<i',len(d))+d
def recv(s):
    n=struct.unpack('<i',s.recv(4))[0]; d=b''
    while len(d)<n: d+=s.recv(n-len(d))
    return d[8:-2].decode(errors='replace')
s=socket.create_connection(('127.0.0.1',25575)); s.settimeout(60); s.sendall(pkt(1,3,'kn8test')); recv(s)
def rcon(c): s.sendall(pkt(2,2,c)); return recv(s)
def gt(): return int(re.search(r'(\d+)', rcon('time query gametime')).group(1))
X0, Z0, Y = 2000, 2000, -60
def P(x, z, y=0): return f'{X0+x} {Y+y} {Z0+z}'
SCEN = {
 # nome: (blocos [(x0,y0,z0,x1,y1,z1,bloco)], entidades [(tag, tipo, x, z, nbt, lado)])
 'solda': ([], [('s','kn8:soldier',0,0,'{kn8_variant:"rifle",kn8_power_level:"normal"}','A'), ('k','kn8:primigenius',0,12,'','B')]),
 'formiga': ([], [('h','kn8:hoshina',0,0,'','A'), ('k','kn8:camponotus',0,12,'','B')]),
 'formiga_s': ([], [('s','kn8:soldier',0,0,'{kn8_variant:"sword",kn8_power_level:"high"}','A'), ('k','kn8:camponotus',0,12,'','B')]),
 'so_formiga': ([(-4,0,-4,-4,4,30,'minecraft:bedrock'),(4,0,-4,4,4,30,'minecraft:bedrock')], [('k','kn8:camponotus',0,16,'','B'), ('x','minecraft:armor_stand',30,30,'','A')]),
 'formiga_aberta': ([], [('k','kn8:camponotus',0,16,'','B'), ('p','kn8:primigenius',20,-20,'','B'), ('x','minecraft:armor_stand',30,30,'','A')]),
 'formiga_larga': ([(-7,0,-4,-7,4,30,'minecraft:bedrock'),(7,0,-4,7,4,30,'minecraft:bedrock')], [('k','kn8:camponotus',0,16,'','B'), ('x','minecraft:armor_stand',30,30,'','A')]),
 'aberto': ([], [('h','kn8:hoshina',0,0,'','A'), ('k','kn8:primigenius',0,18,'','B')]),
 'parede': ([(-8,0,8,8,5,8,'minecraft:bedrock')],
            [('s1','kn8:soldier',-2,0,'{kn8_variant:"rifle",kn8_power_level:"normal"}','A'),
             ('s2','kn8:soldier',2,0,'{kn8_variant:"rifle",kn8_power_level:"normal"}','A'),
             ('k','kn8:primigenius',0,16,'','B')]),
 'corredor': ([(-4,0,-4,-4,4,30,'minecraft:bedrock'),(4,0,-4,4,4,30,'minecraft:bedrock')],
              [('s1','kn8:soldier',-1,0,'{kn8_variant:"sword",kn8_power_level:"high"}','A'),
               ('s2','kn8:soldier',0,1,'{kn8_variant:"sword",kn8_power_level:"high"}','A'),
               ('s3','kn8:soldier',1,0,'{kn8_variant:"knife",kn8_power_level:"high"}','A'),
               ('k','kn8:camponotus',0,16,'','B')]),
 'alto': ([(-3,0,12,3,5,18,'minecraft:bedrock')],
          [('h','kn8:kikoru',0,15,'','A',6), ('k','kn8:primigenius',0,0,'','B')]),
 'grupo': ([], [('s1','kn8:soldier',-6,0,'{kn8_variant:"rifle",kn8_power_level:"normal"}','A'),
                ('s2','kn8:soldier',-2,0,'{kn8_variant:"rifle",kn8_power_level:"normal"}','A'),
                ('s3','kn8:soldier',2,0,'{kn8_variant:"rifle",kn8_power_level:"normal"}','A'),
                ('s4','kn8:soldier',6,0,'{kn8_variant:"sword",kn8_power_level:"normal"}','A'),
                ('h','kn8:hoshina',0,-3,'','A'),
                ('k1','kn8:trichonephila',-10,22,'','B'), ('k2','kn8:primigenius',0,24,'','B'),
                ('k3','kn8:camponotus',10,22,'','B')]),
 'alto_preso': ([(-1,0,4,1,5,6,'minecraft:bedrock')],
                [('a','kn8:soldier',0,5,'{kn8_variant:"rifle",NoAI:1b}','A',6),
                 ('c','kn8:soldier',0,-14,'{kn8_variant:"rifle",NoAI:1b}','A'),
                 ('k','kn8:primigenius',0,0,'','B')]),
 'preondactyl': ([], [('s1','kn8:soldier',-3,0,'{kn8_variant:"rifle",kn8_power_level:"high"}','A'),
                      ('s2','kn8:soldier',3,0,'{kn8_variant:"rifle",kn8_power_level:"high"}','A'),
                      ('k','kn8:preondactyl',0,20,'','B')]),
 'municao': ([], [('s1','kn8:soldier',-3,0,'{kn8_variant:"rifle",kn8_power_level:"elite"}','A'),
                  ('s2','kn8:soldier',3,0,'{kn8_variant:"pistol",kn8_power_level:"elite"}','A'),
                  ('r','kn8:reno',0,-2,'','A'),
                  ('k','kn8:primigenius_honju',0,22,'','B')]),
 'municao_alvo': ([], [('s1','kn8:soldier',-3,0,'{kn8_variant:"rifle"}','A'),
                       ('s2','kn8:soldier',3,0,'{kn8_variant:"pistol"}','A'),
                       ('r','kn8:reno',0,-2,'','A'),
                       ('k','kn8:kaiju_no10_giant',0,30,'{NoAI:1b}','B')]),
 'abrigo': ([(-4,0,-4,4,4,4,'minecraft:stone_bricks'),(-3,0,-3,3,3,3,'minecraft:air')],
            [('s1','kn8:soldier',-1,0,'{kn8_variant:"rifle"}','A'), ('s2','kn8:soldier',1,0,'{kn8_variant:"rifle"}','A'),
             ('k','kn8:primigenius',0,10,'','B')]),
 'narumi': ([], [('n','kn8:narumi',0,0,'','A'), ('k','kn8:primigenius_honju',0,18,'','B')]),
 'no8': ([], [('n','kn8:kaiju_no8',0,0,'','A'), ('k','kn8:primigenius_honju',0,18,'','B')]),
 'reno_cobre': ([], [('s','kn8:soldier',0,4,'{kn8_variant:"rifle"}','A'), ('r','kn8:reno',0,-8,'','A'),
                     ('k1','kn8:primigenius',-14,16,'','B'), ('k2','kn8:primigenius',2,10,'','B')]),
 'cerco': ([], [('s1','kn8:soldier',-3,0,'{kn8_variant:"rifle",kn8_power_level:"elite"}','A'),
                ('s2','kn8:soldier',3,0,'{kn8_variant:"rifle",kn8_power_level:"elite"}','A'),
                ('h','kn8:hoshina',0,-2,'','A'),
                ('k1','kn8:primigenius',-4,18,'','B'), ('k2','kn8:primigenius',0,20,'','B'),
                ('k3','kn8:primigenius',4,18,'','B')]),
 'separacao': ([], [('h','kn8:hoshina',-2,0,'','A'), ('i','kn8:kikoru',0,0,'','A'), ('n','kn8:narumi',2,0,'','A'),
                    ('s','kn8:soldier',0,-2,'{kn8_variant:"rifle"}','A'), ('k','kn8:primigenius_honju',0,18,'','B')]),
 # MOV-1: beco sem saida virado para o kaiju (parado, sem IA): os soldados de perto precisam sair pelos fundos e
 # contornar; degraus de 1 e 2 blocos no caminho.
 'beco': ([(-3,0,3,3,3,3,'minecraft:bedrock'),(-3,0,-6,-3,3,3,'minecraft:bedrock'),(3,0,-6,3,3,3,'minecraft:bedrock')],
          [('s1','kn8:soldier',-1,0,'{kn8_variant:"sword",kn8_power_level:"high"}','A'),
           ('s2','kn8:soldier',1,0,'{kn8_variant:"sword",kn8_power_level:"high"}','A'),
           ('k','kn8:primigenius',0,14,'{NoAI:1b}','B')]),
 'degraus': ([(-6,0,4,6,0,6,'minecraft:stone'),(-6,0,7,6,1,9,'minecraft:stone'),(-6,0,10,6,0,11,'minecraft:stone')],
             [('s1','kn8:soldier',-1,0,'{kn8_variant:"sword",kn8_power_level:"high"}','A'),
              ('h','kn8:hoshina',1,0,'','A'), ('k','kn8:primigenius',0,16,'{NoAI:1b}','B')]),
 'pilares': ([(x,0,z,x+1,6,z+1,'minecraft:bedrock') for x in (-8,-2,4) for z in (8,14,20)],
             [('m','kn8:mina',0,0,'','A'), ('k','kn8:primigenius_honju',0,28,'','B')]),
}
name=sys.argv[1]; secs=float(sys.argv[2]) if len(sys.argv)>2 else 40
blocks, ents = SCEN[name]
rcon('difficulty normal'); rcon('gamerule doMobSpawning false'); rcon('gamerule mobGriefing true')
rcon(f'forceload add {X0-48} {Z0-48} {X0+48} {Z0+48}')
rcon(f'kill @e[type=!minecraft:player,x={X0-60},y=-70,z={Z0-60},dx=120,dy=60,dz=120]'); time.sleep(0.3)
rcon(f'kill @e[type=!minecraft:player,x={X0-60},y=-70,z={Z0-60},dx=120,dy=60,dz=120]')
time.sleep(0.5)
rcon(f'kill @e[type=kn8:carcass,x={X0-60},y=-70,z={Z0-60},dx=120,dy=60,dz=120]')
for qx in (-40, 1):
    for qz in (-40, 1):
        r=rcon(f'fill {X0+qx} {Y} {Z0+qz} {X0+qx+40} {Y+10} {Z0+qz+40} air')
        assert 'Too many' not in r, r
rcon(f'fill {X0-40} {Y-1} {Z0-40} {X0+40} {Y-1} {Z0+40} stone')
for b in blocks:
    x0,y0,z0,x1,y1,z1,blk=b; rcon(f'fill {P(x0,z0,y0)} {P(x1,z1,y1)} {blk}')
for e in ents:
    tag,typ,x,z,nbt,side=e[:6]; y=e[6] if len(e)>6 else 0
    n=nbt[1:-1] if nbt else ''
    rcon(f'summon {typ} {P(x+0.5,z+0.5,y)} {{Tags:["kn8a_{tag}","kn8a_{side}"]{","+n if n else ""}}}')
time.sleep(0.5)
def sample():
    out={}
    for e in ents:
        tag=e[0]
        r=rcon(f'data get entity @e[tag=kn8a_{tag},limit=1] Pos')
        h=rcon(f'data get entity @e[tag=kn8a_{tag},limit=1] Health')
        p=[float(v) for v in re.findall(r'(-?[\d.]+)d',r)]
        hm=re.search(r'data: ([0-9.]+)f',h)
        out[tag]=(p, float(hm.group(1)) if hm else None)
    return out
t0=gt(); hist=[]
while (gt()-t0)/20 < secs:
    hist.append(((gt()-t0)/20, sample())); time.sleep(0.25)
    alive={side: any(hist[-1][1][e[0]][1] for e in ents if e[5]==side) for side in ('A','B')}
    if not alive['A'] or not alive['B']: break
def dist(a,b): return math.dist([a[0],a[2]],[b[0],b[2]])
res={}
for e in ents:
    tag,side=e[0],e[5]
    seq=[(t,smp[tag]) for t,smp in hist if smp[tag][1] is not None and len(smp[tag][0])==3]
    if not seq: res[tag]={'morto_cedo':True}; continue
    walked=sum(dist(seq[i][1][0],seq[i+1][1][0]) for i in range(len(seq)-1))
    h0=seq[0][1][1]; hend=seq[-1][1][1]
    first=next((t for t,(p,h) in seq if h<h0-0.01),None)
    # janelas paradas de 3 s com inimigo vivo e longe
    stuck=0.0
    for i in range(len(seq)):
        j=next((k for k in range(i,len(seq)) if seq[k][0]-seq[i][0]>=3.0),None)
        if j is None: break
        if dist(seq[i][1][0],seq[j][1][0])<0.5:
            t=seq[i][0]; smp=dict(hist)[t] if False else None
            foes=[hist_s[1] for hist_s in hist if hist_s[0]==seq[i][0]]
            snap=foes[0] if foes else {}
            near=[dist(seq[i][1][0],snap[f[0]][0]) for f in ents if f[5]!=side and snap.get(f[0]) and snap[f[0]][1] and len(snap[f[0]][0])==3]
            if near and min(near)>5: stuck+=0.25
    res[tag]={'tipo':e[1].split(':')[1],'andou':round(walked,1),'parado_longe_s':round(stuck,1),
              'vida':f'{h0:.0f}->{hend:.0f}','primeiro_dano_s':first,'vivo_no_fim':hist[-1][1][tag][1] is not None}
print(json.dumps({'cenario':name,'duracao_s':round(hist[-1][0],1),'resultado':res},ensure_ascii=False))
if name=='parede':
    lost=0.0
    for i in range(len(hist)-1):
        a,b=hist[i][1],hist[i+1][1]
        k0,k1=a['k'],b['k']
        if not (k0[1] and k1[1] and len(k0[0])==3): continue
        hidden=k0[0][2]>Z0+8.6 and abs(k0[0][0]-X0)<8 and all(
            a[t][1] is None or (len(a[t][0])==3 and a[t][0][2]<Z0+7.4) for t in ('s1','s2'))
        if hidden and k1[1]<k0[1]: lost+=k0[1]-k1[1]
    print(json.dumps({'parede_dano_no_kaiju_sem_visao':round(lost,1)}))

# IA-4: empilhamento do lado B (menor distancia media entre kaiju vivos do mesmo lado, em blocos).
gaps=[]
for t,smp in hist:
    pos=[smp[e[0]][0] for e in ents if e[5]=='B' and smp[e[0]][1] and len(smp[e[0]][0])==3]
    if len(pos)>1:
        gaps.append(min(dist(pos[i],pos[j]) for i in range(len(pos)) for j in range(i+1,len(pos))))
if gaps:
    print(json.dumps({'kaiju_menor_distancia_media':round(sum(gaps)/len(gaps),2)}))

# IA-5: separacao do lado A (menor distancia media entre aliados vivos, so com o lado B perto: na luta).
gaps=[]
for t,smp in hist:
    pos=[smp[e[0]][0] for e in ents if e[5]=='A' and smp[e[0]][1] and len(smp[e[0]][0])==3]
    foes=[smp[e[0]][0] for e in ents if e[5]=='B' and smp[e[0]][1] and len(smp[e[0]][0])==3]
    if len(pos)>1 and foes and min(dist(p,f) for p in pos for f in foes)<10:
        gaps.append(min(dist(pos[i],pos[j]) for i in range(len(pos)) for j in range(i+1,len(pos))))
if gaps:
    print(json.dumps({'aliados_menor_distancia_media':round(sum(gaps)/len(gaps),2),
                      'aliados_colados_pct':round(100*sum(g<1.0 for g in gaps)/len(gaps))}))
