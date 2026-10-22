// src/main/java/com/kn8/client/vfx/draw/Drawn.java
package com.kn8.client.vfx.draw;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;

import com.kn8.core.vfx.DrawMath;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * VFX-R1 (Miguel: "nao quero que nenhum tenha particulas; quero efeitos especiais como raios ou aura"): um efeito
 * desenhado como forma (faixas e discos de cor), com idade e vida em ticks. Cada forma sabe se desenhar; o
 * {@link DrawnRenderer} guarda a lista, envelhece no tick do cliente e desenha no quadro.
 */
public abstract class Drawn {

    protected final int lifetime;
    protected int age;

    protected Drawn(int lifetime) {
        this.lifetime = Math.max(1, lifetime);
    }

    /** Envelhece um tick; falso quando acabou. */
    boolean tick() {
        return ++age < lifetime;
    }

    /** Vida de 0 a 1 no quadro. */
    protected float life(float partial) {
        return Mth.clamp((age + partial) / lifetime, 0.0F, 1.0F);
    }

    /**
     * Desenha a parte desta passada em coordenadas do mundo ({@code m} ja tira a camera). Duas passadas, uma por tipo
     * de render ({@link #shade} = escurece, depois brilho): o buffer compartilhado do vanilla encerra o anterior ao
     * pedir outro tipo, entao cada passada usa um so.
     */
    abstract void render(boolean shade, VertexConsumer vc, Matrix4f m, Vec3 camera, float partial);

    // --- pecas ---------------------------------------------------------------------------------------------------

    static void vertex(VertexConsumer vc, Matrix4f m, Vec3 p, int rgb, float alpha) {
        vc.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF,
                rgb & 0xFF, Mth.clamp(Math.round(alpha * 255.0F), 0, 255));
    }

    static void quad(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int rgb, float alpha) {
        vertex(vc, m, a, rgb, alpha);
        vertex(vc, m, b, rgb, alpha);
        vertex(vc, m, c, rgb, alpha);
        vertex(vc, m, d, rgb, alpha);
    }

    /** Faixa entre dois pontos virada para a camera (raio, feixe). */
    static void ribbon(VertexConsumer vc, Matrix4f m, Vec3 from, Vec3 to, double width0, double width1, Vec3 camera,
            int rgb, float alpha0, float alpha1) {
        Vec3 dir = to.subtract(from);
        Vec3 view = camera.subtract(from.add(to).scale(0.5));
        Vec3 side = dir.cross(view);
        if (side.lengthSqr() < 1.0E-9) {
            return;
        }
        side = side.normalize();
        Vec3 s0 = side.scale(width0 * 0.5);
        Vec3 s1 = side.scale(width1 * 0.5);
        vertex(vc, m, from.add(s0), rgb, alpha0);
        vertex(vc, m, from.subtract(s0), rgb, alpha0);
        vertex(vc, m, to.subtract(s1), rgb, alpha1);
        vertex(vc, m, to.add(s1), rgb, alpha1);
    }

    /** Disco virado para a camera, cheio no meio e sumindo na borda (clarao, fumaca). */
    static void disc(VertexConsumer vc, Matrix4f m, Vec3 center, double radius, Vec3 camera, int rgb, float alpha,
            int segments) {
        Vec3 view = camera.subtract(center).normalize();
        Vec3 right = view.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1.0E-6) {
            right = new Vec3(1, 0, 0);
        }
        right = right.normalize();
        Vec3 up = right.cross(view).normalize();
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2 * i / segments;
            double a1 = Math.PI * 2 * (i + 1) / segments;
            Vec3 e0 = center.add(right.scale(Math.cos(a0) * radius)).add(up.scale(Math.sin(a0) * radius));
            Vec3 e1 = center.add(right.scale(Math.cos(a1) * radius)).add(up.scale(Math.sin(a1) * radius));
            vertex(vc, m, center, rgb, alpha);
            vertex(vc, m, center, rgb, alpha);
            vertex(vc, m, e1, rgb, 0.0F);
            vertex(vc, m, e0, rgb, 0.0F);
        }
    }

    /** Anel entre {@code inner} e {@code outer} no plano dado por dois eixos (chao ou virado para a camera). */
    static void annulus(VertexConsumer vc, Matrix4f m, Vec3 center, Vec3 axisA, Vec3 axisB, double inner,
            double outer, int rgb, float alpha, int segments) {
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2 * i / segments;
            double a1 = Math.PI * 2 * (i + 1) / segments;
            Vec3 d0 = axisA.scale(Math.cos(a0)).add(axisB.scale(Math.sin(a0)));
            Vec3 d1 = axisA.scale(Math.cos(a1)).add(axisB.scale(Math.sin(a1)));
            vertex(vc, m, center.add(d0.scale(inner)), rgb, alpha * 0.35F);
            vertex(vc, m, center.add(d0.scale(outer)), rgb, alpha);
            vertex(vc, m, center.add(d1.scale(outer)), rgb, alpha);
            vertex(vc, m, center.add(d1.scale(inner)), rgb, alpha * 0.35F);
        }
    }

    static Vec3 vec(double[] p) {
        return new Vec3(p[0], p[1], p[2]);
    }

    // --- formas --------------------------------------------------------------------------------------------------

    /** Raio continuo em zigue-zague com galhos: fora na cor, miolo claro; tremula. */
    public static final class Bolt extends Drawn {
        private final List<List<Vec3>> paths = new ArrayList<>();
        private final double width;
        private final int color;
        private final int core;

        public Bolt(Vec3 start, Vec3 dir, double length, double width, int color, int core, int lifetime,
                float branches, RandomSource random) {
            super(lifetime);
            this.width = width;
            this.color = color;
            this.core = core;
            List<Vec3> main = path(start, dir, length, 6, random);
            paths.add(main);
            for (int n = 1; n < main.size() - 1; n++) {
                if (random.nextFloat() < branches / 3.0F) {
                    Vec3 branchDir = main.get(n).subtract(main.get(n - 1)).normalize()
                            .add(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5)
                            .normalize();
                    paths.add(path(main.get(n), branchDir, length * 0.35, 3, random));
                }
            }
        }

        private static List<Vec3> path(Vec3 start, Vec3 dir, double length, int segments, RandomSource random) {
            List<Vec3> out = new ArrayList<>();
            for (double[] p : DrawMath.boltPath(new double[] {start.x, start.y, start.z},
                    new double[] {dir.x, dir.y, dir.z}, length, segments, length * 0.16, random::nextDouble)) {
                out.add(vec(p));
            }
            return out;
        }

        @Override
        void render(boolean shade, VertexConsumer vc, Matrix4f m, Vec3 camera, float partial) {
            if (shade) {
                return;
            }
            float alpha = DrawMath.fade(life(partial), 0.0F, 0.6F) * (0.75F + 0.25F * (age % 2));
            for (int p = 0; p < paths.size(); p++) {
                List<Vec3> path = paths.get(p);
                double w = p == 0 ? width : width * 0.6;
                for (int i = 1; i < path.size(); i++) {
                    ribbon(vc, m, path.get(i - 1), path.get(i), w, w, camera, color, alpha, alpha);
                    ribbon(vc, m, path.get(i - 1), path.get(i), w * 0.35, w * 0.35, camera, core, alpha, alpha);
                }
            }
        }
    }

    /**
     * Faixa de corte: arco solido que se abre do comeco ao fim do golpe e some; miolo ({@code core}, com
     * transparencia comum: pode ser preto) e fio brilhante na borda ({@code edge}); fino nas pontas.
     */
    public static final class Band extends Drawn {
        private static final int STEPS = 24;
        private final Vec3 center;
        private final double fx;
        private final double fz;
        private final double tilt;
        private final double radius;
        private final double width;
        private final int core;
        private final int edge;

        public Band(Vec3 center, Vec3 forward, double tilt, double radius, double width, int core, int edge,
                int lifetime) {
            super(lifetime);
            this.center = center;
            this.fx = forward.x;
            this.fz = forward.z;
            this.tilt = tilt;
            this.radius = radius;
            this.width = width;
            this.core = core;
            this.edge = edge;
        }

        @Override
        void render(boolean shade, VertexConsumer vc, Matrix4f m, Vec3 camera, float partial) {
            float life = life(partial);
            float alpha = DrawMath.fade(life, 0.0F, 0.55F);
            // O corte se abre nos primeiros 35% da vida.
            float open = Math.min(1.0F, life / 0.35F);
            int steps = Math.max(2, Math.round(STEPS * open));
            double[] c = {center.x, center.y, center.z};
            for (int i = 0; i < steps; i++) {
                double u0 = i / (double) STEPS;
                double u1 = (i + 1) / (double) STEPS;
                double a0 = -75 + 150 * u0;
                double a1 = -75 + 150 * u1;
                double w0 = width * Math.sin(Math.PI * u0) + width * 0.08;
                double w1 = width * Math.sin(Math.PI * u1) + width * 0.08;
                Vec3 in0 = vec(DrawMath.arcPoint(c, fx, fz, tilt, radius - w0 * 0.5, a0));
                Vec3 mid0 = vec(DrawMath.arcPoint(c, fx, fz, tilt, radius + w0 * 0.25, a0));
                Vec3 out0 = vec(DrawMath.arcPoint(c, fx, fz, tilt, radius + w0 * 0.5, a0));
                Vec3 in1 = vec(DrawMath.arcPoint(c, fx, fz, tilt, radius - w1 * 0.5, a1));
                Vec3 mid1 = vec(DrawMath.arcPoint(c, fx, fz, tilt, radius + w1 * 0.25, a1));
                Vec3 out1 = vec(DrawMath.arcPoint(c, fx, fz, tilt, radius + w1 * 0.5, a1));
                // A parte mais velha do corte (comeco) apaga antes.
                float age0 = alpha * (0.45F + 0.55F * (float) u0);
                if (shade) {
                    quad(vc, m, in0, mid0, mid1, in1, core, age0 * 0.9F);
                } else {
                    quad(vc, m, mid0, out0, out1, mid1, edge, age0);
                }
            }
        }
    }

    /** Onda de choque: anel plano no chao que abre e afina. */
    public static final class Wave extends Drawn {
        private final Vec3 center;
        private final double radius;
        private final double width;
        private final int color;

        public Wave(Vec3 center, double radius, double width, int color, int lifetime) {
            super(lifetime);
            this.center = center;
            this.radius = radius;
            this.width = width;
            this.color = color;
        }

        @Override
        void render(boolean shade, VertexConsumer vc, Matrix4f m, Vec3 camera, float partial) {
            if (shade) {
                return;
            }
            float life = life(partial);
            double grow = 1.0 - Math.pow(1.0 - life, 3);
            double r = radius * (0.15 + 0.85 * grow);
            double w = width * (1.0 - 0.6 * life);
            annulus(vc, m, center, new Vec3(1, 0, 0), new Vec3(0, 0, 1),
                    Math.max(0.0, r - w), r, color, DrawMath.fade(life, 0.05F, 0.6F), 48);
        }
    }

    /** Pulso de ar: anel virado para a camera que abre rapido (soco, rugido, contato). */
    public static final class Pulse extends Drawn {
        private final Vec3 center;
        private final double radius;
        private final double width;
        private final int color;

        public Pulse(Vec3 center, double radius, double width, int color, int lifetime) {
            super(lifetime);
            this.center = center;
            this.radius = radius;
            this.width = width;
            this.color = color;
        }

        @Override
        void render(boolean shade, VertexConsumer vc, Matrix4f m, Vec3 camera, float partial) {
            if (shade) {
                return;
            }
            float life = life(partial);
            double r = radius * (0.2 + 0.8 * Math.sqrt(life));
            Vec3 view = camera.subtract(center).normalize();
            Vec3 right = view.cross(new Vec3(0, 1, 0));
            if (right.lengthSqr() < 1.0E-6) {
                right = new Vec3(1, 0, 0);
            }
            right = right.normalize();
            Vec3 up = right.cross(view).normalize();
            annulus(vc, m, center, right, up, Math.max(0.0, r - width), r,
                    color, DrawMath.fade(life, 0.0F, 0.7F), 32);
        }
    }

    /**
     * Feixe ou tiro: faixa continua com miolo claro. Com {@code travel} &gt; 0 so um trecho de {@code travel} anda
     * do inicio ao fim (bala muito rapida); com 0 a linha inteira acende e some (raio de energia).
     */
    public static final class Beam extends Drawn {
        private final Vec3 from;
        private final Vec3 to;
        private final double width;
        private final double travel;
        private final int color;
        private final int core;

        public Beam(Vec3 from, Vec3 to, double width, double travel, int color, int core, int lifetime) {
            super(lifetime);
            this.from = from;
            this.to = to;
            this.width = width;
            this.travel = travel;
            this.color = color;
            this.core = core;
        }

        @Override
        void render(boolean shade, VertexConsumer vc, Matrix4f m, Vec3 camera, float partial) {
            if (shade) {
                return;
            }
            float life = life(partial);
            Vec3 a = from;
            Vec3 b = to;
            float alpha = DrawMath.fade(life, 0.05F, 0.5F);
            if (travel > 0) {
                double length = from.distanceTo(to);
                double head = Math.min(1.0, life * 1.6) * length;
                double tail = Math.max(0.0, head - travel);
                Vec3 dir = to.subtract(from).normalize();
                a = from.add(dir.scale(tail));
                b = from.add(dir.scale(head));
                alpha = 1.0F - Math.max(0.0F, (life - 0.6F) / 0.4F);
            }
            ribbon(vc, m, a, b, width * 0.4, width, camera, color, alpha * 0.4F, alpha);
            ribbon(vc, m, a, b, width * 0.15, width * 0.4, camera, core, alpha * 0.5F, alpha);
        }
    }

    /** Clarao: disco virado para a camera, forte no meio, abrindo e apagando rapido. */
    public static final class Flash extends Drawn {
        private final Vec3 center;
        private final double radius;
        private final int color;

        public Flash(Vec3 center, double radius, int color, int lifetime) {
            super(lifetime);
            this.center = center;
            this.radius = radius;
            this.color = color;
        }

        @Override
        void render(boolean shade, VertexConsumer vc, Matrix4f m, Vec3 camera, float partial) {
            if (shade) {
                return;
            }
            float life = life(partial);
            disc(vc, m, center, radius * (0.6 + 0.6 * life), camera, color,
                    DrawMath.fade(life, 0.0F, 0.8F), 20);
        }
    }

    /** Fumaca desenhada: discos escuros e macios subindo e abrindo (coluna de fumaca, vapor, esporos). */
    public static final class Smoke extends Drawn {
        private final List<Vec3> puffs = new ArrayList<>();
        private final List<Vec3> drift = new ArrayList<>();
        private final double size;
        private final int color;

        public Smoke(Vec3 center, int count, double spread, double size, double rise, int color, int lifetime,
                RandomSource random) {
            super(lifetime);
            this.size = size;
            this.color = color;
            for (int i = 0; i < count; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                double r = Math.sqrt(random.nextDouble()) * spread;
                puffs.add(center.add(Math.cos(a) * r, random.nextDouble() * spread * 0.5, Math.sin(a) * r));
                drift.add(new Vec3((random.nextDouble() - 0.5) * 0.02, rise * (0.6 + random.nextDouble() * 0.8),
                        (random.nextDouble() - 0.5) * 0.02));
            }
        }

        @Override
        void render(boolean shade, VertexConsumer vc, Matrix4f m, Vec3 camera, float partial) {
            if (!shade) {
                return;
            }
            float life = life(partial);
            float alpha = DrawMath.fade(life, 0.1F, 0.6F) * 0.85F;
            double t = age + partial;
            for (int i = 0; i < puffs.size(); i++) {
                Vec3 at = puffs.get(i).add(drift.get(i).scale(t));
                disc(vc, m, at, size * (0.6 + 0.8 * life), camera, color, alpha, 12);
            }
        }
    }
}
