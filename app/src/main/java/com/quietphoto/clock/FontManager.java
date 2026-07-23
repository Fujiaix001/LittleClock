package com.quietphoto.clock;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class FontManager {
    public static final String DEFAULT_ID = "system:bold";

    public static final class FontOption {
        public final String id;
        public final String displayName;

        FontOption(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }
    }

    private static final String[][] BUNDLED = {
            { "asset:font_digital.ttf", "DotGothic16" },
            { "asset:font_sans.ttf", "Noto Sans JP" },
            { "asset:font_serif.ttf", "Noto Serif JP" },
            { "asset:font_rounded.ttf", "Zen Maru Gothic" },
            { "asset:font_kai.ttf", "Klee One" },
            { "asset:font_heavy.ttf", "Dela Gothic One" },
            { "asset:font_orbitron.ttf", "Orbitron" },
            { "asset:font_audiowide.ttf", "Audiowide" },
            { "asset:font_oxanium.ttf", "Oxanium" },
            { "asset:font_sairastencil.ttf", "Saira Stencil One" },
            { "asset:font_zendots.ttf", "Zen Dots" }
    };

    private static final Set<String> LATIN_DATE_IDS = new HashSet<String>();
    private static final Map<String, Typeface> CACHE = new HashMap<String, Typeface>();
    private static List<FontOption> optionCache;

    static {
        LATIN_DATE_IDS.add("asset:font_orbitron.ttf");
        LATIN_DATE_IDS.add("asset:font_audiowide.ttf");
        LATIN_DATE_IDS.add("asset:font_oxanium.ttf");
        LATIN_DATE_IDS.add("asset:font_sairastencil.ttf");
        LATIN_DATE_IDS.add("asset:font_zendots.ttf");
        LATIN_DATE_IDS.add("asset:font_storopia.ttf");
    }

    private FontManager() {
    }

    public static synchronized List<FontOption> getOptions(Context context) {
        if (optionCache != null) {
            return new ArrayList<FontOption>(optionCache);
        }

        List<FontOption> options = new ArrayList<FontOption>();
        options.add(new FontOption(DEFAULT_ID, "系統粗體"));
        for (String[] bundled : BUNDLED) {
            options.add(new FontOption(bundled[0], bundled[1]));
        }
        if (BuildConfig.INCLUDE_STOROPIA) {
            options.add(new FontOption("asset:font_storopia.ttf", "Storopia（測試）"));
        }

        options.add(new FontOption("system:sans", "系統 · 無襯線"));
        options.add(new FontOption("system:serif", "系統 · 襯線"));
        options.add(new FontOption("system:mono", "系統 · 等寬"));

        if (Build.VERSION.SDK_INT >= 29) {
            options.addAll(Api29Fonts.listSystemFonts());
        }
        optionCache = options;
        return new ArrayList<FontOption>(optionCache);
    }

    public static String[] getDisplayNames(List<FontOption> options) {
        String[] names = new String[options.size()];
        for (int i = 0; i < options.size(); i++) {
            names[i] = options.get(i).displayName;
        }
        return names;
    }

    public static String getIdForLegacyIndex(int style) {
        if (style <= 0) return DEFAULT_ID;
        if (style >= 1 && style <= BUNDLED.length) return BUNDLED[style - 1][0];
        if (style == 12 && BuildConfig.INCLUDE_STOROPIA) return "asset:font_storopia.ttf";
        return DEFAULT_ID;
    }

    public static int findOptionIndex(List<FontOption> options, String id) {
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).id.equals(id)) return i;
        }
        return 0;
    }

    public static String normalizeId(Context context, String id) {
        if (id == null || id.length() == 0) return DEFAULT_ID;
        if (DEFAULT_ID.equals(id)
                || "system:sans".equals(id)
                || "system:serif".equals(id)
                || "system:mono".equals(id)) {
            return id;
        }
        for (String[] bundled : BUNDLED) {
            if (bundled[0].equals(id)) return id;
        }
        if (BuildConfig.INCLUDE_STOROPIA && "asset:font_storopia.ttf".equals(id)) {
            return id;
        }
        if (Build.VERSION.SDK_INT >= 29 && id.startsWith("system-file:")) {
            File file = new File(id.substring("system-file:".length()));
            if (file.isFile()) return id;
        }
        return DEFAULT_ID;
    }

    public static boolean usesLatinDate(String id) {
        return id != null && (LATIN_DATE_IDS.contains(id) || id.startsWith("system-file:"));
    }

    public static Typeface getFont(Context context, String id) {
        String normalized = normalizeId(context, id);
        if (DEFAULT_ID.equals(normalized)) return Typeface.DEFAULT_BOLD;
        if ("system:sans".equals(normalized)) return Typeface.SANS_SERIF;
        if ("system:serif".equals(normalized)) return Typeface.SERIF;
        if ("system:mono".equals(normalized)) return Typeface.MONOSPACE;

        Typeface cached = CACHE.get(normalized);
        if (cached != null) return cached;

        Typeface fallback = Typeface.SANS_SERIF;
        try {
            Typeface result;
            if (normalized.startsWith("asset:")) {
                result = Typeface.createFromAsset(
                        context.getAssets(), "fonts/" + normalized.substring("asset:".length()));
            } else if (normalized.startsWith("system-file:") && Build.VERSION.SDK_INT >= 29) {
                result = Api29Fonts.load(normalized.substring("system-file:".length()));
            } else {
                result = fallback;
            }
            if (result != null) {
                CACHE.put(normalized, result);
                return result;
            }
        } catch (Throwable ignored) {
            // 字型可能在系統更新後消失；安全回退到系統字型。
        }
        CACHE.put(normalized, fallback);
        return fallback;
    }

    private static String readableSystemName(String fileName) {
        String base = fileName.replaceFirst("(?i)\\.(ttf|otf|ttc)$", "");
        base = base.replace('_', ' ').replace('-', ' ').trim();
        if (base.length() == 0) base = fileName;
        return "系統 · " + base;
    }

    @android.annotation.TargetApi(29)
    private static final class Api29Fonts {
        private Api29Fonts() {
        }

        static List<FontOption> listSystemFonts() {
            List<FontOption> result = new ArrayList<FontOption>();
            Set<String> paths = new HashSet<String>();
            try {
                for (android.graphics.fonts.Font font : android.graphics.fonts.SystemFonts.getAvailableFonts()) {
                    File file = font.getFile();
                    if (file == null || !file.isFile()) continue;
                    String path = file.getAbsolutePath();
                    if (paths.add(path)) {
                        result.add(new FontOption(
                                "system-file:" + path,
                                readableSystemName(file.getName())));
                    }
                }
            } catch (Throwable ignored) {
                return Collections.emptyList();
            }
            Collections.sort(result, new Comparator<FontOption>() {
                @Override
                public int compare(FontOption left, FontOption right) {
                    return left.displayName.toLowerCase(Locale.US)
                            .compareTo(right.displayName.toLowerCase(Locale.US));
                }
            });
            return result;
        }

        static Typeface load(String path) {
            File file = new File(path);
            return file.isFile() ? new Typeface.Builder(file).build() : null;
        }
    }
}
