"""Carga de IA (IA-7, plano de IA Parte 13) no servidor dedicado: muitos kaiju e soldados lutando sem morrer
(Resistencia V em todos) e o tempo medio de tick do mundo pelo /neoforge tps. Precisa do servidor com RCON
(porta 25575, senha kn8test). Usa a area x/z 1960-2041 (ou a mais longe dos jogadores com 'longe').
Uso: python3 tools/ai/carga_ia.py <kaiju> <soldados> [segundos] [longe]"""
import socket, struct, sys, time, re, random
def pkt(i,t,b): d=struct.pack('<ii',i,t)+b.encode()+b'\0\0'; return struct.pack('<i',len(d))+d
def recv(s):
    n=struct.unpack('<i',s.recv(4))[0]; d=b''
    while len(d)<n: d+=s.recv(n-len(d))
    return d[8:-2].decode(errors='replace')
s=socket.create_connection(('127.0.0.1',25575)); s.settimeout(60); s.sendall(pkt(1,3,'kn8test')); recv(s)
def rcon(c): s.sendall(pkt(2,2,c)); return recv(s)
nk=int(sys.argv[1]); ns=int(sys.argv[2]); secs=float(sys.argv[3]) if len(sys.argv)>3 else 30
far='longe' in sys.argv
X0, Z0, Y = (2400, 2400, -60) if far else (2000, 2000, -60)
KAIJU=['kn8:primigenius','kn8:primigenius','kn8:trichonephila','kn8:camponotus','kn8:primigenius_honju']
SPECIAL=['kn8:hoshina','kn8:kikoru','kn8:narumi','kn8:mina']
rcon('difficulty normal'); rcon('gamerule doMobSpawning false'); rcon('gamerule mobGriefing false')
rcon(f'forceload add {X0-48} {Z0-48} {X0+48} {Z0+48}')
box=f'x={X0-60},y=-70,z={Z0-60},dx=120,dy=60,dz=120'
rcon(f'kill @e[type=!minecraft:player,{box}]'); time.sleep(0.5); rcon(f'kill @e[type=!minecraft:player,{box}]')
for qx in (-40, 1):
    for qz in (-40, 1):
        rcon(f'fill {X0+qx} {Y} {Z0+qz} {X0+qx+40} {Y+10} {Z0+qz+40} air')
rcon(f'fill {X0-40} {Y-1} {Z0-40} {X0+40} {Y-1} {Z0+40} stone')
rnd=random.Random(7)
for i in range(nk):
    rcon(f'summon {KAIJU[i%len(KAIJU)]} {X0+rnd.uniform(-30,30):.1f} {Y} {Z0+rnd.uniform(10,30):.1f} {{Tags:["kn8c"]}}')
for i in range(ns):
    typ=SPECIAL[i//5 % len(SPECIAL)] if i%5==4 else 'kn8:soldier'
    nbt='kn8_variant:"rifle",' if typ=='kn8:soldier' else ''
    rcon(f'summon {typ} {X0+rnd.uniform(-30,30):.1f} {Y} {Z0+rnd.uniform(-30,-10):.1f} {{{nbt}Tags:["kn8c"]}}')
rcon(f'effect give @e[tag=kn8c] resistance infinite 4 true')
time.sleep(5)
def mspt():
    r=rcon('neoforge tps minecraft:overworld')
    m=re.search(r'([\d.]+) ms',r) or re.search(r'Mean tick time: ([\d.]+)',r)
    return float(m.group(1)) if m else None
vals=[]
t0=time.time()
while time.time()-t0<secs:
    v=mspt()
    if v is not None: vals.append(v)
    time.sleep(2)
alive=rcon('execute if entity @e[tag=kn8c]')
vals.sort()
print(f'kaiju={nk} soldados={ns} longe={far} amostras={len(vals)} media={sum(vals)/len(vals):.2f} ms '
      f'mediana={vals[len(vals)//2]:.2f} max={vals[-1]:.2f} vivos: {alive}')
rcon(f'kill @e[tag=kn8c]')
