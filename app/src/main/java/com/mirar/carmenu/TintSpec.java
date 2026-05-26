package com.mirar.carmenu;

/**
 * Server-supplied color tint for an icon. Two forms:
 *
 * <ul>
 *   <li>Named slug — {@code "primary"}, {@code "red"}, {@code "green"},
 *       {@code "blue"}, {@code "yellow"}, {@code "default"}. Maps to a
 *       predefined {@code CarColor} the AA host renders in its theme.</li>
 *   <li>Hex literal — {@code "#RRGGBB"} or {@code "#AARRGGBB"}. Becomes a
 *       custom {@code CarColor} with the same value for day + night.</li>
 * </ul>
 *
 * <p>Parsed once at JSON-decode time; resolved to a CarColor at render time
 * (kept Android-free here so the parser is unit-testable).
 */
public final class TintSpec {

    public enum Named { DEFAULT, PRIMARY, SECONDARY, RED, GREEN, BLUE, YELLOW }

    /** Set if named. Null when custom hex. */
    public final Named named;
    /** Set if custom. 0xAARRGGBB. Ignored when {@code named != null}. */
    public final int argb;
    public final boolean isCustom;

    private TintSpec(Named named, int argb, boolean isCustom) {
        this.named = named; this.argb = argb; this.isCustom = isCustom;
    }

    public static TintSpec ofNamed(Named n)    { return new TintSpec(n, 0, false); }
    public static TintSpec ofArgb(int argb)    { return new TintSpec(null, argb, true); }

    /**
     * @return parsed TintSpec, or {@code null} if {@code raw} is null/empty/invalid.
     */
    public static TintSpec parse(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;
        if (s.charAt(0) == '#') {
            return parseHex(s);
        }
        switch (s.toLowerCase()) {
            case "default":   return ofNamed(Named.DEFAULT);
            case "primary":   return ofNamed(Named.PRIMARY);
            case "secondary": return ofNamed(Named.SECONDARY);
            case "red":       return ofNamed(Named.RED);
            case "green":     return ofNamed(Named.GREEN);
            case "blue":      return ofNamed(Named.BLUE);
            case "yellow":    return ofNamed(Named.YELLOW);
            default:          return null;
        }
    }

    private static TintSpec parseHex(String s) {
        // s starts with '#'
        String hex = s.substring(1);
        try {
            if (hex.length() == 6) {
                int rgb = Integer.parseInt(hex, 16);
                return ofArgb(0xFF000000 | rgb);
            }
            if (hex.length() == 8) {
                long v = Long.parseLong(hex, 16);
                return ofArgb((int) v);
            }
            return null;
        } catch (NumberFormatException nfe) {
            return null;
        }
    }
}
