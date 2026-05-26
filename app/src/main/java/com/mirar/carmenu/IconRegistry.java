package com.mirar.carmenu;

import java.util.HashMap;
import java.util.Map;

/**
 * Server icon slug → packaged drawable resource id.
 *
 * <p>To add an icon: drop a vector at {@code res/drawable/ic_<slug>.xml} and
 * register the slug here. Old clients that don't know a new slug fall through
 * to {@code ic_generic}.
 */
public class IconRegistry {

    private final Map<String, Integer> map = new HashMap<>();
    private final int fallback;

    public IconRegistry() {
        this(R.drawable.ic_generic);
        map.put("generic",  R.drawable.ic_generic);
        map.put("home",     R.drawable.ic_home);
        map.put("work",     R.drawable.ic_work);
        map.put("lunch",    R.drawable.ic_lunch);
        map.put("grocery",  R.drawable.ic_grocery);
        map.put("charging", R.drawable.ic_charging);
        map.put("parking",  R.drawable.ic_parking);
        map.put("fuel",     R.drawable.ic_fuel);
        map.put("fav",      R.drawable.ic_fav);
        map.put("school",   R.drawable.ic_school);
        map.put("friends",  R.drawable.ic_friends);
        map.put("mall",     R.drawable.ic_mall);
        map.put("shop",     R.drawable.ic_shop);
        map.put("spinner",  R.drawable.ic_spinner);
        map.put("refresh",  R.drawable.ic_refresh);
        map.put("back",     R.drawable.ic_back);
    }

    /** Test seam: build a registry with a custom fallback (used by unit tests). */
    IconRegistry(int fallback) {
        this.fallback = fallback;
    }

    /** Manually register a slug. Used by unit tests; production constructor pre-populates. */
    void register(String slug, int resId) {
        map.put(slug, resId);
    }

    /** Returns the resource id, or the fallback (ic_generic) if slug is unknown/null. */
    public int resolve(String slug) {
        if (slug == null) return fallback;
        Integer r = map.get(slug);
        return r != null ? r : fallback;
    }
}
