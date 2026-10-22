package com.kn8.core.vfx;

/** Cores dos perfis visuais: "#RRGGBB" para inteiro e de volta, e componentes 0-1 para as particulas. */
public final class VfxColors {

    private VfxColors() {
    }

    /** "#RRGGBB" ou "RRGGBB" -> 0xRRGGBB; invalido -> -1. */
    public static int parse(String hex) {
        String clean = hex.startsWith("#") ? hex.substring(1) : hex;
        if (clean.length() != 6) {
            return -1;
        }
        try {
            return Integer.parseInt(clean, 16);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String format(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    public static float red(int rgb) {
        return ((rgb >> 16) & 0xFF) / 255.0F;
    }

    public static float green(int rgb) {
        return ((rgb >> 8) & 0xFF) / 255.0F;
    }

    public static float blue(int rgb) {
        return (rgb & 0xFF) / 255.0F;
    }

    /** Clareia/escurece misturando com branco ({@code amount} > 0) ou preto (< 0). */
    public static int shade(int rgb, float amount) {
        float target = amount >= 0 ? 255.0F : 0.0F;
        float k = Math.min(1.0F, Math.abs(amount));
        int r = Math.round(((rgb >> 16) & 0xFF) + (target - ((rgb >> 16) & 0xFF)) * k);
        int g = Math.round(((rgb >> 8) & 0xFF) + (target - ((rgb >> 8) & 0xFF)) * k);
        int b = Math.round((rgb & 0xFF) + (target - (rgb & 0xFF)) * k);
        return (r << 16) | (g << 8) | b;
    }
}
