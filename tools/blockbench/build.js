// Constroi um projeto do Blockbench a partir de args (gerado por tools/art/blockbench_templates.py) e devolve o
// .bbmodel. args: {format, name, textures:[{name,png}], groups:[{name,origin,parent,rotation}],
// cubes:[{name,parent,from,to,uv_offset,texture,inflate}], meshes:[{name,parent,texture,vertices:[[x,y,z]],
// faces:[[i,j,k,u0,v0,u1,v1,u2,v2]]}], animations:[{name,length,loop,bones:{bone:{rotation:[[t,x,y,z]],
// position:[[t,x,y,z]]}}}], shots:[{file,anim,time,camera:[x,y,z],target:[x,y,z]}]}
newProject(Formats[args.format || 'free']);
Project.name = args.name || 'model';
Project.texture_width = args.texture_width || 16; Project.texture_height = args.texture_height || 16;
const tex = {};
for (const t of args.textures || []) tex[t.name] = new Texture({name: t.name}).fromDataURL(t.png).add(false);
const groups = {};
for (const g of args.groups) {
  const grp = new Group({name: g.name, origin: g.origin, rotation: g.rotation || [0, 0, 0]});
  if (g.parent) grp.addTo(groups[g.parent]);
  grp.init();
  groups[g.name] = grp;
}
for (const c of args.cubes || []) {
  const opts = {name: c.name, from: c.from, to: c.to, inflate: c.inflate || 0};
  if (c.faces) {
    opts.box_uv = false;
    opts.faces = {};
    for (const [face, uv] of Object.entries(c.faces)) opts.faces[face] = {uv, texture: tex[c.texture].uuid};
  } else {
    Object.assign(opts, {box_uv: true, uv_offset: c.uv_offset || [0, 0]});
  }
  const cube = new Cube(opts);
  cube.addTo(groups[c.parent]).init();
  if (c.texture && !c.faces) cube.applyTexture(tex[c.texture], true);
}
for (const m of args.meshes || []) {
  const mesh = new Mesh({name: m.name, vertices: {}});
  mesh.addTo(groups[m.parent]);
  const keys = mesh.addVertices(...m.vertices);
  const t = tex[m.texture];
  const faces = m.faces.map(f => new MeshFace(mesh, {vertices: [keys[f[0]], keys[f[1]], keys[f[2]]],
      uv: {[keys[f[0]]]: [f[3], f[4]], [keys[f[1]]]: [f[5], f[6]], [keys[f[2]]]: [f[7], f[8]]},
      texture: t ? t.uuid : false}));
  mesh.addFaces(...faces);
  mesh.init();
}
Canvas.updateAll();
const anims = {};
for (const a of args.animations || []) {
  const anim = new Animation({name: a.name, length: a.length, loop: a.loop || 'once', snapping: 20}).add(false);
  // Marcadores = instante em que o servidor aplica o dano (a animacao tem que acertar ali).
  for (const t of a.markers || []) anim.markers.push(new TimelineMarker({time: t, color: 1}));
  for (const [bone, ch] of Object.entries(a.bones)) {
    const animator = anim.getBoneAnimator(groups[bone]);
    for (const [channel, frames] of Object.entries(ch)) {
      for (const f of frames) {
        animator.addKeyframe({channel, time: f[0], data_points: [{x: f[1], y: f[2], z: f[3]}],
                              interpolation: 'linear'});
      }
    }
  }
  anims[a.name] = anim;
}
const P = Preview.selected;
for (const s of args.shots || []) {
  if (s.anim) { anims[s.anim].select(); Timeline.setTime(s.time || 0); Animator.preview(); }
  if (s.preset) {
    P.loadAnglePreset(DefaultCameraPresets.find(p => p.id === s.preset));
    if (s.zoom) { P.camera.zoom = s.zoom; P.camera.updateProjectionMatrix(); }
  } else {
    P.camera.position.set(...s.camera); P.controls.target.set(...s.target); P.controls.update();
  }
  await new Promise(r => setTimeout(r, 500));
  await window.__shot(s.file);
}
const first = Object.values(anims)[0];
if (first) first.select();
return {files: {[args.out]: Codecs.project.compile()}};
