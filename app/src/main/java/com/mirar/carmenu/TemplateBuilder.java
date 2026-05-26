package com.mirar.carmenu;

import android.graphics.Bitmap;

import androidx.car.app.CarContext;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.Pane;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.core.graphics.drawable.IconCompat;

/**
 * {@link ServerResponse} → {@link Template}.
 *
 * <p>AA caps: ListTemplate ≤ 6 rows, PaneTemplate ≤ 4 rows + ≤ 2 actions,
 * MessageTemplate has body + optional icon. Server should respect those
 * caps; we trim silently.
 *
 * <p>Icons: built-in registry slug (synchronous) or remote URL (async via
 * {@link IconCache}, with a generic placeholder until the bitmap lands).
 * When an async icon resolves, the registered {@link Listener#onAsyncIcon}
 * fires so the screen can {@code invalidate()}.
 */
public class TemplateBuilder {

    static final int MAX_LIST_ROWS = 6;
    static final int MAX_PANE_ROWS = 4;
    static final int MAX_PANE_ACTIONS = 2;

    public interface Listener {
        /** A remote icon a prior render asked for has just become available. */
        void onAsyncIcon();
    }

    private final CarContext carContext;
    private final IconRegistry icons;
    private final IntentParser.InternalHandler internal;
    private final Listener listener;
    private final IconCache.Listener cacheListener;

    public TemplateBuilder(CarContext carContext, IconRegistry icons,
                           IntentParser.InternalHandler internal,
                           Listener listener) {
        this.carContext = carContext;
        this.icons = icons;
        this.internal = internal;
        this.listener = listener;
        this.cacheListener = (url, bitmap) -> {
            if (this.listener != null) this.listener.onAsyncIcon();
        };
    }

    public Template build(ServerResponse r) {
        switch (r.template) {
            case LIST:    return buildList(r);
            case PANE:    return buildPane(r);
            case MESSAGE: return buildMessage(r);
            default:
                return new MessageTemplate.Builder("Unknown template")
                        .setTitle(titleOrEmpty(r.title))
                        .setHeaderAction(Action.APP_ICON)
                        .build();
        }
    }

    /**
     * AA templates require a title (Builder will throw on empty). We use an
     * empty-string title rather than a client-side default so an omitted
     * server {@code title} leaves the header bar visually empty rather than
     * showing the app name as if the server had supplied it. The header
     * still carries {@code APP_ICON} so the user can identify the app.
     */
    private static String titleOrEmpty(String t) {
        return (t != null && !t.isEmpty()) ? t : " ";
    }

    private Template buildList(ServerResponse r) {
        ItemList.Builder list = new ItemList.Builder();
        int n = Math.min(r.rows.size(), MAX_LIST_ROWS);
        for (int i = 0; i < n; i++) {
            list.addItem(rowToCarRow(r.rows.get(i)));
        }
        if (n == 0) list.setNoItemsMessage("No items");
        return new ListTemplate.Builder()
                .setTitle(titleOrEmpty(r.title))
                .setSingleList(list.build())
                .setHeaderAction(Action.APP_ICON)
                .build();
    }

    private Template buildPane(ServerResponse r) {
        Pane.Builder pane = new Pane.Builder();
        int n = Math.min(r.rows.size(), MAX_PANE_ROWS);
        if (n == 0) {
            pane.setLoading(true);
        } else {
            for (int i = 0; i < n; i++) pane.addRow(rowToCarRow(r.rows.get(i)));
        }
        int actions = Math.min(r.actions.size(), MAX_PANE_ACTIONS);
        for (int i = 0; i < actions; i++) {
            ServerResponse.Row a = r.actions.get(i);
            Action.Builder ab = new Action.Builder().setTitle(a.title);
            CarIcon ci = iconForRow(a);
            if (ci != null) ab.setIcon(ci);
            if (a.tapIntent != null) {
                final String uri = a.tapIntent;
                ab.setOnClickListener(() ->
                        IntentParser.parseAndDispatch(carContext, uri, internal));
            }
            pane.addAction(ab.build());
        }
        return new PaneTemplate.Builder(pane.build())
                .setTitle(titleOrEmpty(r.title))
                .setHeaderAction(Action.APP_ICON)
                .build();
    }

    private Template buildMessage(ServerResponse r) {
        MessageTemplate.Builder mb = new MessageTemplate.Builder(
                r.subtitle != null ? r.subtitle : "");
        mb.setTitle(titleOrEmpty(r.title));
        mb.setHeaderAction(Action.APP_ICON);
        if (r.icon != null) {
            CarIcon ci = builtInCarIcon(r.icon, r.iconTint);
            if (ci != null) mb.setIcon(ci);
        }
        mb.setActionStrip(new ActionStrip.Builder()
                .addAction(new Action.Builder()
                        .setIcon(builtInCarIcon("refresh", null))
                        .setOnClickListener(() -> {
                            if (internal != null) internal.onRefresh();
                        })
                        .build())
                .build());
        return mb.build();
    }

    private Row rowToCarRow(ServerResponse.Row r) {
        Row.Builder b = new Row.Builder().setTitle(r.title);
        if (r.subtitle != null && !r.subtitle.isEmpty()) b.addText(r.subtitle);
        CarIcon ci = iconForRow(r);
        if (ci != null) {
            // IMAGE_TYPE_ICON treats the bitmap/vector as a mono glyph and
            // honors CarIcon.setTint. IMAGE_TYPE_SMALL (default) would
            // render the image as-is and silently ignore the tint — the
            // bug that made server-supplied tints look stuck.
            //
            // Server convention: built-in slugs are white vector glyphs
            // designed to be tinted; remote bitmaps are usually multi-color
            // and don't want tinting. We only ask the host to treat the
            // image as a tinted icon when a tint was actually requested or
            // we're showing a built-in slug.
            boolean asTintedIcon = (r.tint != null) || (r.iconUrl == null);
            if (asTintedIcon) {
                b.setImage(ci, Row.IMAGE_TYPE_ICON);
            } else {
                b.setImage(ci);   // IMAGE_TYPE_SMALL — show bitmap as-is
            }
        }
        if (r.tapIntent != null && !r.tapIntent.isEmpty()) {
            final String uri = r.tapIntent;
            b.setOnClickListener(() ->
                    IntentParser.parseAndDispatch(carContext, uri, internal));
        }
        return b.build();
    }

    /** Resolve an icon for a row — remote URL takes precedence over slug. */
    private CarIcon iconForRow(ServerResponse.Row r) {
        if (r.iconUrl != null) {
            Bitmap bm = IconCache.get().get(r.iconUrl, cacheListener);
            if (bm != null) {
                CarIcon.Builder cb = new CarIcon.Builder(
                        IconCompat.createWithBitmap(bm));
                applyTint(cb, r.tint);
                return cb.build();
            }
            // Cache miss → placeholder; we'll re-render via onAsyncIcon.
            return builtInCarIcon("generic", r.tint);
        }
        if (r.icon != null) return builtInCarIcon(r.icon, r.tint);
        return null;
    }

    private CarIcon builtInCarIcon(String slug, TintSpec tint) {
        int resId = icons.resolve(slug);
        CarIcon.Builder cb = new CarIcon.Builder(
                IconCompat.createWithResource(carContext, resId));
        applyTint(cb, tint);
        return cb.build();
    }

    private static void applyTint(CarIcon.Builder cb, TintSpec tint) {
        if (tint == null) return;
        CarColor cc = toCarColor(tint);
        if (cc != null) cb.setTint(cc);
    }

    private static CarColor toCarColor(TintSpec t) {
        if (t == null) return null;
        if (t.isCustom) {
            return CarColor.createCustom(t.argb, t.argb);
        }
        switch (t.named) {
            case DEFAULT:   return CarColor.DEFAULT;
            case PRIMARY:   return CarColor.PRIMARY;
            case SECONDARY: return CarColor.SECONDARY;
            case RED:       return CarColor.RED;
            case GREEN:     return CarColor.GREEN;
            case BLUE:      return CarColor.BLUE;
            case YELLOW:    return CarColor.YELLOW;
            default:        return null;
        }
    }
}
