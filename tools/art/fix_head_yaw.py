#!/usr/bin/env python3
"""Gira a cabeca de um GLB do Meshy que veio olhando para o lado (0.6, Kaiju No. 10 pequeno).

Reescreve so o POSITION/NORMAL da malha dentro do GLB original (textura e resto intactos, byte a byte). A cabeca e o
pedaco conectado, acima do corte do pescoco, que contem o ponto mais perto de (hx, hz) na altura dos olhos; gira
rigida em volta da base do pescoco (px, pz), o que tambem a traz de volta ao centro do tronco. Coordenadas do GLB
normalizado do Meshy (altura -1..1). Os triangulos que cruzam o corte esticam: no rig a cabeca vira um osso
proprio e o corte e tampado (cap_holes).
Uso (No. 10 pequeno): python3 tools/art/fix_head_yaw.py entrada.glb saida.glb -65 0.65 0.33 0.20 0.21 0.04 0.135
       (giro em graus, corte do pescoco, raio, centro da cabeca x z, base do pescoco x z)
"""
import json, struct, sys
import numpy as np
src, dst = sys.argv[1], sys.argv[2]
yaw, y_cut, radius, hx, hz, px, pz = (float(a) for a in sys.argv[3:10])
data = bytearray(open(src, 'rb').read())
jlen = struct.unpack_from('<I', data, 12)[0]
gltf = json.loads(data[20:20 + jlen]); bin_start = 20 + jlen + 8
prim = gltf['meshes'][0]['primitives'][0]
def view(i):
    acc = gltf['accessors'][i]; bv = gltf['bufferViews'][acc['bufferView']]
    return acc, bin_start + bv.get('byteOffset', 0) + acc.get('byteOffset', 0)
acc, off = view(prim['attributes']['POSITION']); n = acc['count']
V = np.frombuffer(bytes(data[off:off + n * 12]), dtype='<f4').reshape(n, 3).astype(np.float64)
# Cabeca = componente conectado (acima do corte) que contem o ponto mais perto de (hx, hz) na altura dos olhos.
# Vertices soldados pela posicao (o GLB separa costuras de UV).
key = np.round(V / 1e-4).astype(np.int64)
_, weld = np.unique(key, axis=0, return_inverse=True)
weld = weld.ravel()
iacc, ioff = None, None
idx_acc = gltf['accessors'][prim['indices']]; ibv = gltf['bufferViews'][idx_acc['bufferView']]
ioff = bin_start + ibv.get('byteOffset', 0) + idx_acc.get('byteOffset', 0)
dt = {5125: '<u4', 5123: '<u2'}[idx_acc['componentType']]
Fi = np.frombuffer(bytes(data[ioff:ioff + idx_acc['count'] * np.dtype(dt).itemsize]), dtype=dt).reshape(-1, 3)
above = V[:, 1] > y_cut
parent = np.arange(weld.max() + 1)
def find(x):
    while parent[x] != x:
        parent[x] = parent[parent[x]]; x = parent[x]
    return x
for a_, b_, c_ in Fi:
    if above[a_] and above[b_] and above[c_]:
        ra, rb, rc = find(weld[a_]), find(weld[b_]), find(weld[c_])
        parent[rb] = ra; parent[find(rc)] = ra
cand = np.where(above)[0]
seed = cand[np.argmin(np.hypot(V[cand, 0] - hx, V[cand, 2] - hz) + np.abs(V[cand, 1] - 0.78))]
root = find(weld[seed])
sel = above & np.array([find(w) == root for w in weld])
print('extensao da cabeca x', V[sel, 0].min().round(2), V[sel, 0].max().round(2), 'y', V[sel, 1].min().round(2),
      V[sel, 1].max().round(2))
a = np.radians(yaw); c, s = np.cos(a), np.sin(a)
def rot(M, cx, cz):
    x, z = M[:, 0] - cx, M[:, 2] - cz
    M[:, 0], M[:, 2] = cx + x * c + z * s, cz - x * s + z * c
H = V[sel]; rot(H, px, pz); V[sel] = H
data[off:off + n * 12] = V.astype('<f4').tobytes(); acc['min'] = V.min(0).tolist(); acc['max'] = V.max(0).tolist()
if 'NORMAL' in prim['attributes']:
    nacc, noff = view(prim['attributes']['NORMAL'])
    N = np.frombuffer(bytes(data[noff:noff + n * 12]), dtype='<f4').reshape(n, 3).astype(np.float64)
    M = N[sel]; rot(M, 0.0, 0.0); N[sel] = M; data[noff:noff + n * 12] = N.astype('<f4').tobytes()
js = json.dumps(gltf, separators=(',', ':')).encode(); js += b' ' * ((4 - len(js) % 4) % 4)
rest = bytes(data[20 + jlen:])
open(dst, 'wb').write(struct.pack('<4sII', b'glTF', 2, 20 + len(js) + len(rest)) + struct.pack('<I4s', len(js), b'JSON') + js + rest)
print('vertices da cabeca', int(sel.sum()))
