package org.mirar.carmenu;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Shows the bundled protocol spec ({@code assets/protocol.txt}) in a
 * monospaced scrolling view, with a "Save to file…" button that uses the
 * Storage Access Framework so the user can pick where to drop a copy on
 * the phone filesystem. No permissions needed; works on API 24+.
 */
public class ProtocolActivity extends AppCompatActivity {

    private static final String ASSET_NAME = "protocol.txt";
    private static final String SAVE_DEFAULT = "carmenu-protocol.txt";

    private String spec = "";
    private ActivityResultLauncher<String> createDocLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createDocLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("text/plain"),
                this::onPickedSaveLocation);

        spec = readAsset();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (12 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        TextView header = new TextView(this);
        header.setText("Server protocol");
        header.setTextSize(18);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setPadding(0, 0, 0, pad / 2);
        root.addView(header);

        TextView note = new TextView(this);
        note.setText("How CarMenu talks to your server. Bundled with the app — "
                + "no network needed to view this.");
        note.setTextSize(13);
        note.setPadding(0, 0, 0, pad);
        root.addView(note);

        // Action buttons row
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        Button save = new Button(this);
        save.setText("Save to file…");
        save.setOnClickListener(v -> createDocLauncher.launch(SAVE_DEFAULT));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.rightMargin = pad / 2;
        actions.addView(save, lp);

        Button share = new Button(this);
        share.setText("Share");
        share.setOnClickListener(v -> doShare());
        LinearLayout.LayoutParams lpr = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lpr.leftMargin = pad / 2;
        actions.addView(share, lpr);

        LinearLayout.LayoutParams actionsLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        actionsLp.bottomMargin = pad;
        root.addView(actions, actionsLp);

        // Scrollable monospace body
        ScrollView scroll = new ScrollView(this);
        TextView body = new TextView(this);
        body.setText(spec);
        body.setTypeface(Typeface.MONOSPACE);
        body.setTextSize(12);
        body.setTextIsSelectable(true);
        body.setHorizontallyScrolling(false);
        scroll.addView(body);

        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0, 1f);
        root.addView(scroll, scrollLp);

        setContentView(root);
    }

    private String readAsset() {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = getAssets().open(ASSET_NAME);
             BufferedReader br = new BufferedReader(
                     new InputStreamReader(is, StandardCharsets.UTF_8))) {
            char[] buf = new char[4096];
            int n;
            while ((n = br.read(buf)) > 0) sb.append(buf, 0, n);
        } catch (IOException e) {
            return "Failed to read bundled " + ASSET_NAME + ": " + e.getMessage();
        }
        return sb.toString();
    }

    private void onPickedSaveLocation(Uri uri) {
        if (uri == null) return;   // user cancelled
        ContentResolver cr = getContentResolver();
        try (OutputStream os = cr.openOutputStream(uri)) {
            if (os == null) throw new IOException("openOutputStream returned null");
            os.write(spec.getBytes(StandardCharsets.UTF_8));
            Toast.makeText(this, "Saved " + uri.getLastPathSegment(),
                    Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Save failed: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void doShare() {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, spec);
        send.putExtra(Intent.EXTRA_SUBJECT, "CarMenu server protocol");
        startActivity(Intent.createChooser(send, "Share protocol"));
    }
}
