package com.kn8.core.kaiju;

/**
 * Area de um golpe pesado em preparo, a mesma que o aviso desenha no chao (IA-6), agora como area de verdade (MOV-4,
 * plano de mobilidade v3 §6: "sair de areas de impacto"): disco (pisao, aterrissagem do salto), setor (soco,
 * varredura) ou faixa (investida, tiro). Java puro, testado no JUnit: quem esta dentro e para onde sair.
 */
public record DangerZone(Kind kind, double cx, double cz, double radius, double center, double width, double x1,
        double z1, double halfWidth) {

    public enum Kind {
        DISC, SECTOR, LANE
    }

    /** Disco de raio {@code radius} em volta de (cx, cz). */
    public static DangerZone disc(double cx, double cz, double radius) {
        return new DangerZone(Kind.DISC, cx, cz, radius, 0.0, Math.PI * 2.0, 0.0, 0.0, 0.0);
    }

    /** Setor de raio {@code radius} a partir de (cx, cz), centrado no angulo {@code center} (atan2 de z por x). */
    public static DangerZone sector(double cx, double cz, double radius, double center, double width) {
        return new DangerZone(Kind.SECTOR, cx, cz, radius, center, width, 0.0, 0.0, 0.0);
    }

    /** Faixa de (x0, z0) a (x1, z1) com meia largura {@code halfWidth}. */
    public static DangerZone lane(double x0, double z0, double x1, double z1, double halfWidth) {
        return new DangerZone(Kind.LANE, x0, z0, 0.0, 0.0, 0.0, x1, z1, halfWidth);
    }

    /** O ponto (x, z) esta dentro da area alargada por {@code margin}. */
    public boolean contains(double x, double z, double margin) {
        double dx = x - cx;
        double dz = z - cz;
        switch (kind) {
            case DISC -> {
                return dx * dx + dz * dz <= sq(radius + margin);
            }
            case SECTOR -> {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > radius + margin) {
                    return false;
                }
                if (width >= Math.PI * 2.0 - 1.0E-6 || distance <= margin) {
                    return true;
                }
                double off = Math.abs(wrap(Math.atan2(dz, dx) - center));
                // Fora do angulo, ainda dentro se estiver a menos de margin da borda reta do setor.
                return off <= width * 0.5 || distance * Math.sin(Math.min(off - width * 0.5, Math.PI * 0.5)) <= margin;
            }
            default -> {
                return laneDistance(x, z) <= halfWidth + margin;
            }
        }
    }

    /**
     * Direcao (unitaria, x e z) do caminho mais curto para fora da area a partir de (x, z): para fora do centro no
     * disco e no setor (de lado se estiver mais perto da borda reta), de lado na faixa. Ponto exatamente no centro:
     * {@code fallbackX}/{@code fallbackZ}.
     */
    public double[] exit(double x, double z, double fallbackX, double fallbackZ) {
        double ex;
        double ez;
        switch (kind) {
            case LANE -> {
                double lx = x1 - cx;
                double lz = z1 - cz;
                double length = Math.sqrt(lx * lx + lz * lz);
                if (length < 1.0E-6) {
                    ex = x - cx;
                    ez = z - cz;
                } else {
                    // Para o lado da faixa em que ja esta (perpendicular a ela).
                    double nx = -lz / length;
                    double nz = lx / length;
                    double side = (x - cx) * nx + (z - cz) * nz;
                    ex = side >= 0 ? nx : -nx;
                    ez = side >= 0 ? nz : -nz;
                }
            }
            case SECTOR -> {
                double dx = x - cx;
                double dz = z - cz;
                double distance = Math.sqrt(dx * dx + dz * dz);
                double off = wrap(Math.atan2(dz, dx) - center);
                double toArc = radius - distance;
                double toEdge = distance * Math.sin(Math.max(0.0, width * 0.5 - Math.abs(off)));
                if (width < Math.PI * 2.0 - 1.0E-6 && toEdge < toArc) {
                    // Mais perto da borda reta: de lado, girando para o lado em que ja esta.
                    double angle = center + Math.signum(off == 0 ? 1.0 : off) * (width * 0.5 + Math.PI * 0.5);
                    ex = Math.cos(angle);
                    ez = Math.sin(angle);
                } else {
                    ex = dx;
                    ez = dz;
                }
            }
            default -> {
                ex = x - cx;
                ez = z - cz;
            }
        }
        double length = Math.sqrt(ex * ex + ez * ez);
        if (length < 1.0E-6) {
            double fallback = Math.sqrt(fallbackX * fallbackX + fallbackZ * fallbackZ);
            return fallback < 1.0E-6 ? new double[] {1.0, 0.0}
                    : new double[] {fallbackX / fallback, fallbackZ / fallback};
        }
        return new double[] {ex / length, ez / length};
    }

    /** Quanto andar na direcao de {@link #exit} para sair, com {@code margin} de folga (aproximado, por cima). */
    public double exitDistance(double x, double z, double margin) {
        double distance = Math.sqrt(sq(x - cx) + sq(z - cz));
        return switch (kind) {
            case DISC -> Math.max(0.0, radius - distance) + margin;
            case SECTOR -> {
                double toArc = Math.max(0.0, radius - distance);
                if (width >= Math.PI * 2.0 - 1.0E-6) {
                    yield toArc + margin;
                }
                double off = Math.abs(wrap(Math.atan2(z - cz, x - cx) - center));
                yield Math.min(toArc, distance * Math.sin(Math.max(0.0, width * 0.5 - off))) + margin;
            }
            default -> Math.max(0.0, halfWidth - laneDistance(x, z)) + margin;
        };
    }

    private double laneDistance(double x, double z) {
        double lx = x1 - cx;
        double lz = z1 - cz;
        double length2 = lx * lx + lz * lz;
        double t = length2 < 1.0E-9 ? 0.0 : Math.max(0.0, Math.min(1.0, ((x - cx) * lx + (z - cz) * lz) / length2));
        return Math.sqrt(sq(x - (cx + lx * t)) + sq(z - (cz + lz * t)));
    }

    private static double wrap(double angle) {
        double a = angle % (Math.PI * 2.0);
        if (a > Math.PI) {
            a -= Math.PI * 2.0;
        } else if (a < -Math.PI) {
            a += Math.PI * 2.0;
        }
        return a;
    }

    private static double sq(double v) {
        return v * v;
    }
}
