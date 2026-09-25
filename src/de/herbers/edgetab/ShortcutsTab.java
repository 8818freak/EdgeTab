package de.herbers.edgetab;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Frei gestaltbare Registerkarte mit App-Verknuepfungen: Raster (einstellbare
 * Spaltenzahl) oder Liste, in frei benannte Gruppen sortierbar, eigener Name
 * je Verknuepfung. Tippen oeffnet die App direkt (ueber Launcher.launchApp -
 * kein Bind/keine Berechtigung noetig, nur PackageManager).
 *
 * "Echte" tiefe App-Verknuepfungen (z.B. direkt zu einem Chat) sind fuer
 * Drittanbieter-Apps auf modernem Android praktisch nicht zu bekommen - das
 * Anheften solcher Verknuepfungen (ShortcutManager/LauncherApps) landet fast
 * immer exklusiv beim Standard-Launcher. Diese Karte zeigt darum bewusst
 * reine App-Symbole.
 */
public class ShortcutsTab extends BaseTab {

    // Zustand, der einen Panel-Neuaufbau ueberleben muss - je Karten-Id, weil
    // es mehrere Verknuepfungs-Karten geben kann. Statisch, weil bei jedem
    // Aufbau ein neues ShortcutsTab-Objekt entsteht (wie SettingsTab.savedScrollY).
    private static final Set<String> ADDING = new HashSet<>();
    private static final Set<String> EDITING = new HashSet<>();
    private static final Set<String> MENU_OPEN = new HashSet<>();
    private static final Map<String, Integer> SAVED_SCROLL = new HashMap<>();
    // Waehrend eines Hinzufuegen-Durchgangs angehakte Pakete - je Karten-Id,
    // damit man mehrere Apps auswaehlen und auf einmal hinzufuegen kann statt
    // pro App wieder in den Dialog zu muessen (Mathias' Wunsch, wie beim
    // Original-Hub-Konto-Dialog wo man immer nur eine wahl konnte - nur besser).
    private static final Map<String, Set<String>> PICK_SELECTED = new HashMap<>();

    ShortcutsTab(TabInstance inst) { super(inst, R.string.tab_shortcuts, R.drawable.ic_shortcuts); }

    public View buildContent(Context ctx, Runnable closePanel, Runnable refreshContent) {
        int d = Math.round(ctx.getResources().getDisplayMetrics().density);
        Runnable close = effectiveClose(closePanel);

        ScrollView scroll = new ScrollView(ctx);
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(root);

        final Runnable refresh = () -> {
            SAVED_SCROLL.put(inst.id, scroll.getScrollY());
            if (refreshContent != null) refreshContent.run();
        };

        boolean adding = ADDING.contains(inst.id);
        boolean editing = EDITING.contains(inst.id);
        boolean menuOpen = MENU_OPEN.contains(inst.id);

        // Nur EIN Zahnrad im Ruhezustand statt zweier Knoepfe nebeneinander -
        // Tippen klappt ein kleines Menue mit den Aktionen auf. Ist gerade
        // eine Aktion aktiv (Hinzufuegen/Bearbeiten), zeigt sich stattdessen
        // nur deren eigener Abbrechen/Fertig-Knopf.
        LinearLayout tools = new LinearLayout(ctx);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        tools.setGravity(Gravity.CENTER_VERTICAL);

        Set<String> pickSel = PICK_SELECTED.computeIfAbsent(inst.id, k -> new HashSet<>());
        if (adding) {
            Button cancel = new Button(ctx);
            cancel.setText(R.string.cancel_button);
            cancel.setOnClickListener(v -> { ADDING.remove(inst.id); pickSel.clear(); refresh.run(); });
            tools.addView(cancel);

            Button addSel = new Button(ctx);
            addSel.setText(pickSel.isEmpty() ? ctx.getString(R.string.add_button) : ctx.getString(R.string.add_button_count, pickSel.size()));
            addSel.setOnClickListener(v -> {
                if (!pickSel.isEmpty()) {
                    Tabs.update(ctx, inst.id, t -> {
                        for (String pkg : pickSel) t.shortcuts.add(new TabInstance.ShortcutRef(pkg, null, null));
                    });
                }
                pickSel.clear();
                ADDING.remove(inst.id);
                refresh.run();
            });
            tools.addView(addSel);
        } else if (editing) {
            Button done = new Button(ctx);
            done.setText(R.string.done_button);
            done.setOnClickListener(v -> { EDITING.remove(inst.id); refresh.run(); });
            tools.addView(done);
        } else {
            TextView gear = new TextView(ctx);
            gear.setText("⚙");
            gear.setTextColor(Color.parseColor("#B0B0B0"));
            gear.setTextSize(20);
            gear.setPadding(8 * d, 6 * d, 8 * d, 6 * d);
            gear.setOnClickListener(v -> {
                if (menuOpen) MENU_OPEN.remove(inst.id); else MENU_OPEN.add(inst.id);
                refresh.run();
            });
            tools.addView(gear);

            if (menuOpen) {
                Button addBtn = new Button(ctx);
                addBtn.setText(R.string.add_app_button);
                addBtn.setOnClickListener(v -> {
                    ADDING.add(inst.id);
                    MENU_OPEN.remove(inst.id);
                    refresh.run();
                });
                tools.addView(addBtn);

                if (!inst.shortcuts.isEmpty()) {
                    Button editBtn = new Button(ctx);
                    editBtn.setText(R.string.edit_button);
                    editBtn.setOnClickListener(v -> {
                        EDITING.add(inst.id);
                        MENU_OPEN.remove(inst.id);
                        refresh.run();
                    });
                    tools.addView(editBtn);
                }
            }
        }
        root.addView(tools);

        if (adding) {
            root.addView(appPicker(ctx, refresh, d, pickSel));
        } else if (inst.shortcuts.isEmpty()) {
            TextView hint = new TextView(ctx);
            hint.setText(R.string.shortcuts_empty_hint);
            hint.setTextColor(Color.parseColor("#9E9E9E"));
            hint.setTextSize(14);
            hint.setPadding(0, 12 * d, 0, 0);
            root.addView(hint);
        } else if (editing) {
            root.addView(editList(ctx, refresh, d));
        } else {
            root.addView(display(ctx, close, d));
        }

        Integer y = SAVED_SCROLL.remove(inst.id);
        if (y != null) { final int yy = y; scroll.post(() -> scroll.scrollTo(0, yy)); }
        return scroll;
    }

    // ---------- Gruppierung (je Gruppe eigene Ansicht: Raster/Liste + Spalten) ----------

    private static String normGroup(String g) {
        return (g == null || g.trim().isEmpty()) ? "" : g.trim();
    }

    /** Verknuepfungen nach Gruppe sortiert, in der Reihenfolge ihres ersten
     *  Auftauchens (LinkedHashMap) - liefert die INDIZES in inst.shortcuts,
     *  damit man sie zum Bearbeiten wieder eindeutig zuordnen kann. */
    private LinkedHashMap<String, List<Integer>> groupIndices() {
        LinkedHashMap<String, List<Integer>> groups = new LinkedHashMap<>();
        for (int i = 0; i < inst.shortcuts.size(); i++) {
            String g = normGroup(inst.shortcuts.get(i).group);
            List<Integer> list = groups.get(g);
            if (list == null) { list = new ArrayList<>(); groups.put(g, list); }
            list.add(i);
        }
        return groups;
    }

    /** Reihenfolge der Gruppen aendern: da Gruppen nicht als eigene Objekte
     *  gespeichert sind, sondern sich rein aus der Reihenfolge der Eintraege
     *  in inst.shortcuts ergeben (erstes Auftauchen), wird beim Verschieben
     *  einfach der ganze zusammenhaengende Eintrags-Block dieser Gruppe mit
     *  dem des Nachbarn getauscht. */
    private void moveGroup(Context ctx, String groupKey, boolean up, Runnable refresh) {
        Tabs.update(ctx, inst.id, t -> {
            LinkedHashMap<String, List<TabInstance.ShortcutRef>> byGroup = new LinkedHashMap<>();
            for (TabInstance.ShortcutRef r : t.shortcuts) {
                String g = normGroup(r.group);
                List<TabInstance.ShortcutRef> list = byGroup.get(g);
                if (list == null) { list = new ArrayList<>(); byGroup.put(g, list); }
                list.add(r);
            }
            List<String> order = new ArrayList<>(byGroup.keySet());
            int idx = order.indexOf(groupKey);
            int swapWith = up ? idx - 1 : idx + 1;
            if (idx < 0 || swapWith < 0 || swapWith >= order.size()) return;
            java.util.Collections.swap(order, idx, swapWith);
            List<TabInstance.ShortcutRef> rebuilt = new ArrayList<>();
            for (String g : order) rebuilt.addAll(byGroup.get(g));
            t.shortcuts.clear();
            t.shortcuts.addAll(rebuilt);
        });
        refresh.run();
    }

    /** Kopfzeile einer Gruppe im Bearbeiten-Modus: Name + Reihenfolge-Pfeile +
     *  eigener Ansicht-Umschalter (Raster/Liste) + Spaltenzahl (nur bei Raster). */
    private View groupHeaderEdit(Context ctx, String gk, Runnable refresh, int d, boolean multi) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 14 * d, 0, 4 * d);

        if (multi) {
            TextView up = stepButton(ctx, "▲");
            up.setOnClickListener(v -> moveGroup(ctx, gk, true, refresh));
            row.addView(up);
            TextView down = stepButton(ctx, "▼");
            down.setOnClickListener(v -> moveGroup(ctx, gk, false, refresh));
            row.addView(down);
        }

        TextView label = new TextView(ctx);
        label.setText(gk.isEmpty() ? ctx.getString(R.string.no_group_label) : gk);
        label.setTextColor(Color.parseColor("#2E9BE6"));
        label.setTextSize(13);
        label.setPadding(6 * d, 0, 0, 0);
        label.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(label);

        TabInstance.GroupSettings gs = inst.settingsFor(gk);
        Button viewToggle = new Button(ctx);
        viewToggle.setText(gs.grid ? R.string.view_grid : R.string.view_list);
        viewToggle.setOnClickListener(v -> {
            Tabs.update(ctx, inst.id, t -> {
                TabInstance.GroupSettings g = t.mutableSettingsFor(gk);
                g.grid = !g.grid;
            });
            refresh.run();
        });
        row.addView(viewToggle);

        if (gs.grid) {
            TextView colLabel = new TextView(ctx);
            colLabel.setText("  " + gs.columns + "  ");
            colLabel.setTextColor(Color.parseColor("#DDDDDD"));
            colLabel.setTextSize(13);
            row.addView(colLabel);

            TextView minus = stepButton(ctx, "−");
            minus.setOnClickListener(v -> {
                Tabs.update(ctx, inst.id, t -> {
                    TabInstance.GroupSettings g = t.mutableSettingsFor(gk);
                    g.columns = Math.max(2, g.columns - 1);
                });
                refresh.run();
            });
            row.addView(minus);
            TextView plus = stepButton(ctx, "+");
            plus.setOnClickListener(v -> {
                Tabs.update(ctx, inst.id, t -> {
                    TabInstance.GroupSettings g = t.mutableSettingsFor(gk);
                    g.columns = Math.min(6, g.columns + 1);
                });
                refresh.run();
            });
            row.addView(plus);
        }
        return row;
    }

    private TextView stepButton(Context ctx, String s) {
        TextView t = new TextView(ctx);
        t.setText("  " + s + "  ");
        t.setTextColor(Color.parseColor("#2E9BE6"));
        t.setTextSize(16);
        return t;
    }

    // ---------- App-Auswahl ----------

    private View appPicker(Context ctx, Runnable refresh, int d, Set<String> pickSel) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(10 * d, 8 * d, 10 * d, 8 * d);
        // Eigener, UNDURCHSICHTIGER Hintergrund - die App-Liste haengt sonst in
        // der (je nach Einstellung stark durchsichtigen) Leiste selbst und ist
        // dort schlecht lesbar (Mathias: "eher lästig"). Bewusst unabhaengig
        // von Settings.transparency(), die nur fuer den Leisten-Hintergrund gilt.
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#FF1C1C1E"));
        bg.setCornerRadius(10 * d);
        box.setBackground(bg);

        PackageManager pm = ctx.getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(main, 0);
        // Nach Anzeigename sortieren - ueber TreeMap statt Comparator (d8-Fallstrick).
        TreeMap<String, ResolveInfo> sorted = new TreeMap<>();
        int idx = 0;
        if (apps != null) for (ResolveInfo ri : apps) {
            if (ri.activityInfo == null) continue;
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(ctx.getPackageName())) continue;
            String label;
            try { label = ri.loadLabel(pm).toString(); } catch (Throwable t) { label = pkg; }
            sorted.put(label.toLowerCase() + "" + (idx++), ri);
        }

        for (Map.Entry<String, ResolveInfo> e : sorted.entrySet()) {
            ResolveInfo ri = e.getValue();
            String pkg = ri.activityInfo.packageName;
            String label;
            try { label = ri.loadLabel(pm).toString(); } catch (Throwable t) { label = pkg; }

            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, 6 * d, 0, 6 * d);

            CheckBox cb = new CheckBox(ctx);
            cb.setButtonTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#2E9BE6")));
            cb.setChecked(pickSel.contains(pkg));
            cb.setOnCheckedChangeListener((v, on) -> {
                if (on) pickSel.add(pkg); else pickSel.remove(pkg);
                refresh.run();
            });
            row.addView(cb);

            ImageView icon = new ImageView(ctx);
            try { icon.setImageDrawable(ri.loadIcon(pm)); } catch (Throwable ignored) {}
            int s = 28 * d;
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(s, s);
            ilp.leftMargin = 4 * d; ilp.rightMargin = 10 * d;
            icon.setLayoutParams(ilp);
            row.addView(icon);

            TextView t = new TextView(ctx);
            t.setText(label);
            t.setTextColor(Color.WHITE);
            t.setTextSize(14);
            row.addView(t);

            row.setClickable(true);
            row.setOnClickListener(v -> cb.setChecked(!cb.isChecked()));
            box.addView(row);
        }
        if (sorted.isEmpty()) {
            TextView none = new TextView(ctx);
            none.setText(R.string.no_apps_found);
            none.setTextColor(Color.parseColor("#9E9E9E"));
            box.addView(none);
        }
        return box;
    }

    // ---------- Anzeige (Raster/Liste, gruppiert) ----------

    private View display(Context ctx, Runnable close, int d) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, 8 * d, 0, 0);

        PackageManager pm = ctx.getPackageManager();
        LinkedHashMap<String, List<TabInstance.ShortcutRef>> groups = new LinkedHashMap<>();
        for (TabInstance.ShortcutRef r : inst.shortcuts) {
            String g = normGroup(r.group);
            List<TabInstance.ShortcutRef> list = groups.get(g);
            if (list == null) { list = new ArrayList<>(); groups.put(g, list); }
            list.add(r);
        }

        // Mehrere Gruppen: jede in einen eigenen Rahmen - macht auf einen
        // Blick klar, welche Verknuepfungen zusammengehoeren (Mathias' Wunsch).
        // Bei nur einer (oder keiner) Gruppe waere ein Rahmen nur Ballast.
        boolean framed = groups.size() > 1;
        for (Map.Entry<String, List<TabInstance.ShortcutRef>> e : groups.entrySet()) {
            LinearLayout target = box;
            if (framed) {
                LinearLayout frame = new LinearLayout(ctx);
                frame.setOrientation(LinearLayout.VERTICAL);
                frame.setPadding(8 * d, 4 * d, 8 * d, 8 * d);
                GradientDrawable frameBg = new GradientDrawable();
                frameBg.setColor(Color.parseColor("#171719"));
                frameBg.setCornerRadius(10 * d);
                frameBg.setStroke((int) (1 * d), Color.parseColor("#3A3A3C"));
                frame.setBackground(frameBg);
                LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                flp.topMargin = 8 * d;
                frame.setLayoutParams(flp);
                box.addView(frame);
                target = frame;
            }
            if (!e.getKey().isEmpty()) {
                TextView head = new TextView(ctx);
                head.setText(e.getKey());
                head.setTextColor(Color.parseColor("#2E9BE6"));
                head.setTextSize(13);
                head.setPadding(0, framed ? 4 * d : 10 * d, 0, 4 * d);
                target.addView(head);
            }
            TabInstance.GroupSettings gs = inst.settingsFor(e.getKey());
            if (gs.grid) target.addView(grid(ctx, pm, e.getValue(), close, d, gs.columns));
            else target.addView(list(ctx, pm, e.getValue(), close, d));
        }
        return box;
    }

    private View grid(Context ctx, PackageManager pm, List<TabInstance.ShortcutRef> items,
                       Runnable close, int d, int columns) {
        GridLayout g = new GridLayout(ctx);
        g.setColumnCount(columns);
        // Zellbreite (und damit Icongroesse) aus der verfuegbaren Breite und
        // der Spaltenzahl ableiten, statt fest zu verdrahten - sonst aendert
        // sich beim Aendern der Spaltenzahl nichts sichtbar, und mehr Spalten
        // passen bei fester Breite oft gar nicht erst nebeneinander.
        int availableDp = Math.max(120, Settings.panelWidth(ctx) - 70);
        int cellWDp = Math.max(48, availableDp / columns);
        int cellW = cellWDp * d;
        int iconSDp = Math.max(20, Math.min(56, Math.round(cellWDp * 0.55f)));
        for (TabInstance.ShortcutRef r : items) {
            LinearLayout cell = new LinearLayout(ctx);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(4 * d, 6 * d, 4 * d, 6 * d);
            GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
            glp.width = cellW;
            cell.setLayoutParams(glp);

            ImageView icon = new ImageView(ctx);
            icon.setImageDrawable(appIcon(ctx, pm, r.pkg));
            int s = iconSDp * d;
            icon.setLayoutParams(new LinearLayout.LayoutParams(s, s));
            cell.addView(icon);

            TextView label = new TextView(ctx);
            label.setText(appLabel(pm, r));
            label.setTextColor(Color.WHITE);
            label.setTextSize(11);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(2);
            label.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            cell.addView(label);

            cell.setOnClickListener(new LaunchClick(ctx, r.pkg, close));
            g.addView(cell);
        }
        return g;
    }

    private View list(Context ctx, PackageManager pm, List<TabInstance.ShortcutRef> items,
                       Runnable close, int d) {
        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        for (TabInstance.ShortcutRef r : items) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, 8 * d, 0, 8 * d);

            ImageView icon = new ImageView(ctx);
            icon.setImageDrawable(appIcon(ctx, pm, r.pkg));
            int s = 30 * d;
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(s, s);
            ilp.rightMargin = 12 * d;
            icon.setLayoutParams(ilp);
            row.addView(icon);

            TextView label = new TextView(ctx);
            label.setText(appLabel(pm, r));
            label.setTextColor(Color.WHITE);
            label.setTextSize(14);
            row.addView(label);

            row.setClickable(true);
            row.setOnClickListener(new LaunchClick(ctx, r.pkg, close));
            col.addView(row);
        }
        return col;
    }

    private Drawable appIcon(Context ctx, PackageManager pm, String pkg) {
        try { return pm.getApplicationIcon(pkg); }
        catch (Throwable t) { return ctx.getDrawable(R.drawable.ic_shortcuts); }
    }

    private String appLabel(PackageManager pm, TabInstance.ShortcutRef r) {
        if (r.label != null && !r.label.trim().isEmpty()) return r.label;
        try { return pm.getApplicationLabel(pm.getApplicationInfo(r.pkg, 0)).toString(); }
        catch (Throwable t) { return r.pkg; }
    }

    static final class LaunchClick implements View.OnClickListener {
        private final Context ctx; private final String pkg; private final Runnable close;
        LaunchClick(Context ctx, String pkg, Runnable close) { this.ctx = ctx; this.pkg = pkg; this.close = close; }
        public void onClick(View v) {
            Launcher.launchApp(ctx.getApplicationContext(), pkg, Launcher.bgAllowed());
            if (close != null) close.run();
        }
    }

    // ---------- Bearbeiten: umbenennen, Gruppe zuweisen, entfernen ----------

    private View editList(Context ctx, Runnable refresh, int d) {
        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(0, 0, 0, 0);
        PackageManager pm = ctx.getPackageManager();

        LinkedHashMap<String, List<Integer>> groups = groupIndices();
        boolean multi = groups.size() > 1;
        for (Map.Entry<String, List<Integer>> e : groups.entrySet()) {
            col.addView(groupHeaderEdit(ctx, e.getKey(), refresh, d, multi));
            List<Integer> idxs = e.getValue();
            for (int pos = 0; pos < idxs.size(); pos++) {
                col.addView(itemEditor(ctx, pm, idxs, pos, refresh, d));
            }
        }
        return col;
    }

    /** Position INNERHALB der eigenen Gruppe aendern (nicht die Gruppe
     *  selbst) - vertauscht mit dem Vorgaenger/Nachfolger in derselben
     *  Gruppen-Indexliste, egal wo die beiden tatsaechlich in inst.shortcuts
     *  stehen (Mathias' Wunsch, nach den Gruppen-Pfeilen). */
    private void moveWithinGroup(Context ctx, int i, int other, Runnable refresh) {
        Tabs.update(ctx, inst.id, t -> java.util.Collections.swap(t.shortcuts, i, other));
        refresh.run();
    }

    private View itemEditor(Context ctx, PackageManager pm, List<Integer> groupIdxs, int pos, Runnable refresh, int d) {
        int i = groupIdxs.get(pos);
        TabInstance.ShortcutRef r = inst.shortcuts.get(i);
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, 8 * d, 0, 8 * d);

        LinearLayout head = new LinearLayout(ctx);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        if (groupIdxs.size() > 1) {
            if (pos > 0) {
                TextView up = stepButton(ctx, "▲");
                int other = groupIdxs.get(pos - 1);
                up.setOnClickListener(v -> moveWithinGroup(ctx, i, other, refresh));
                head.addView(up);
            }
            if (pos < groupIdxs.size() - 1) {
                TextView down = stepButton(ctx, "▼");
                int other = groupIdxs.get(pos + 1);
                down.setOnClickListener(v -> moveWithinGroup(ctx, i, other, refresh));
                head.addView(down);
            }
        }

        ImageView icon = new ImageView(ctx);
        icon.setImageDrawable(appIcon(ctx, pm, r.pkg));
        int s = 26 * d;
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(s, s);
        ilp.rightMargin = 8 * d;
        icon.setLayoutParams(ilp);
        head.addView(icon);

        EditText name = new EditText(ctx);
        name.setText(r.label);
        name.setHint(appLabel(pm, r));
        name.setTextColor(Color.WHITE);
        name.setTextSize(13);
        name.setSingleLine(true);
        name.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        name.addTextChangedListener(new FieldWatcher(ctx, inst.id, i, FieldWatcher.LABEL));
        head.addView(name);

        // Grosszuegiges Polster - die reine Glyphe war ein zu kleines Ziel.
        TextView del = new TextView(ctx);
        del.setText("✕");
        del.setTextColor(Color.parseColor("#E06666"));
        del.setTextSize(17);
        del.setPadding(14 * d, 10 * d, 4 * d, 10 * d);
        del.setOnClickListener(new RemoveClick(ctx, inst.id, i, refresh));
        head.addView(del);
        row.addView(head);

        // Gruppenname: speichert bei jeder Aenderung (kein Neuaufbau, sonst
        // Fokusverlust); baut die (gruppierte) Ansicht erst beim Verlassen
        // des Feldes neu auf, damit der Eintrag sichtbar in seine neue
        // Gruppe wandert.
        EditText group = new EditText(ctx);
        group.setText(r.group);
        group.setHint(R.string.group_hint);
        group.setTextColor(Color.parseColor("#B0B0B0"));
        group.setTextSize(12);
        group.setSingleLine(true);
        group.addTextChangedListener(new FieldWatcher(ctx, inst.id, i, FieldWatcher.GROUP));
        group.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus && refresh != null) refresh.run(); });
        row.addView(group);

        return row;
    }

    /** Speichert Name oder Gruppe bei jeder Aenderung, ohne Panel-Neuaufbau
     *  (sonst verliert das Feld den Fokus) - wie SettingsTab.RenameWatcher. */
    private static final class FieldWatcher implements TextWatcher {
        static final int LABEL = 0, GROUP = 1;
        private final Context ctx; private final String tabId; private final int index; private final int field;
        FieldWatcher(Context ctx, String tabId, int index, int field) {
            this.ctx = ctx; this.tabId = tabId; this.index = index; this.field = field;
        }
        public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
        public void onTextChanged(CharSequence s, int a, int b, int c) {}
        public void afterTextChanged(Editable e) {
            String v = e.toString().trim().isEmpty() ? null : e.toString();
            Tabs.update(ctx, tabId, t -> {
                if (index < 0 || index >= t.shortcuts.size()) return;
                if (field == LABEL) t.shortcuts.get(index).label = v;
                else t.shortcuts.get(index).group = v;
            });
        }
    }

    static final class RemoveClick implements View.OnClickListener {
        private final Context ctx; private final String tabId; private final int index; private final Runnable after;
        RemoveClick(Context ctx, String tabId, int index, Runnable after) {
            this.ctx = ctx; this.tabId = tabId; this.index = index; this.after = after;
        }
        public void onClick(View v) {
            Tabs.update(ctx, tabId, t -> { if (index >= 0 && index < t.shortcuts.size()) t.shortcuts.remove(index); });
            if (after != null) after.run();
        }
    }
}
