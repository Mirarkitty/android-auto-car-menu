package com.mirar.carmenu;

import java.util.Collections;
import java.util.List;

/**
 * Parsed form of the server's JSON response. See {@code PROTOCOL.md}.
 *
 * <p>One of three template variants: LIST / PANE / MESSAGE. Discriminator
 * is {@link #template}. Plain immutable Java object — no Gson/Moshi dep.
 */
public class ServerResponse {

    public enum Kind { LIST, PANE, MESSAGE }

    public final Kind template;
    public final String title;
    public final String subtitle;       // PANE / MESSAGE only
    public final String icon;           // MESSAGE only
    public final TintSpec iconTint;     // MESSAGE only — for the title icon
    public final List<Row> rows;
    public final String imageUrl;       // PANE only
    public final List<Row> actions;     // PANE only — action buttons
    public final int refreshSeconds;    // 0 = no auto-refresh

    public static class Row {
        public final String title;
        public final String subtitle;
        /** Server icon slug (built-in registry). Null if {@link #iconUrl} is used. */
        public final String icon;
        /**
         * Absolute remote-icon URL, already resolved against server origin
         * and same-origin checked. Null when {@link #icon} (slug) is used.
         */
        public final String iconUrl;
        /** Optional tint applied to whichever icon is rendered. Null = no tint. */
        public final TintSpec tint;
        public final String tapIntent;

        public Row(String title, String subtitle, String icon, String iconUrl,
                   TintSpec tint, String tapIntent) {
            this.title = title; this.subtitle = subtitle;
            this.icon = icon; this.iconUrl = iconUrl; this.tint = tint;
            this.tapIntent = tapIntent;
        }
    }

    public ServerResponse(Kind template, String title, String subtitle, String icon,
                          TintSpec iconTint, List<Row> rows, String imageUrl,
                          List<Row> actions, int refreshSeconds) {
        this.template = template; this.title = title; this.subtitle = subtitle;
        this.icon = icon; this.iconTint = iconTint;
        this.rows = rows != null ? rows : Collections.emptyList();
        this.imageUrl = imageUrl;
        this.actions = actions != null ? actions : Collections.emptyList();
        this.refreshSeconds = refreshSeconds;
    }

    /** Convenience for synthesizing an error response. */
    public static ServerResponse message(String title, String subtitle) {
        return new ServerResponse(Kind.MESSAGE, title, subtitle, "generic", null,
                null, null, null, 0);
    }
}
