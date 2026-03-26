package com.easyinject.ui;

import com.easyinject.Main;

import java.awt.*;
import java.io.*;
import java.nio.file.Path;
import java.util.List;
import javax.swing.*;

/**
 * Diagnostic screen that checks each installation component and offers
 * targeted fixes. Placeholder IF clauses for future diagnostic messages.
 */
public class RepairScreen extends InstallerWindow.ScreenPanel {

    private DiagRow dllRow;
    private DiagRow defenderRow;
    private DiagRow prelaunchRow;
    private DiagRow jarRow;

    private JButton fixAllBtn;
    private JButton backBtn;
    private JLabel summaryLabel;

    public RepairScreen(InstallerWindow window) {
        super(window);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

        JPanel body = Theme.createVBox();

        JLabel title = Theme.createLabel("Repair Diagnostics", Theme.HEADING_FONT, Theme.FG);
        body.add(title);
        body.add(Box.createVerticalStrut(4));

        summaryLabel = Theme.createLabel("Checking installation health...", Theme.SMALL_FONT, Theme.SUBTLE);
        body.add(summaryLabel);
        body.add(Box.createVerticalStrut(12));

        dllRow = new DiagRow("DLL Files");
        body.add(dllRow);
        body.add(Theme.createDivider());

        defenderRow = new DiagRow("Defender Exclusion");
        body.add(defenderRow);
        body.add(Theme.createDivider());

        prelaunchRow = new DiagRow("Pre-Launch Command");
        body.add(prelaunchRow);
        body.add(Theme.createDivider());

        jarRow = new DiagRow("Stable JAR File");
        body.add(jarRow);

        body.add(Box.createVerticalGlue());
        add(body, BorderLayout.CENTER);

        // ── Buttons ─────────────────────────────────────────────────────
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setBackground(Theme.BG);

        backBtn = Theme.createButton("Back");
        backBtn.addActionListener(e -> window.showScreen(InstallerWindow.SCREEN_HOME));

        fixAllBtn = Theme.createAccentButton("Fix All", Theme.WARNING);
        fixAllBtn.setVisible(false);
        fixAllBtn.addActionListener(e -> fixAll());

        buttons.add(fixAllBtn);
        buttons.add(backBtn);
        add(buttons, BorderLayout.SOUTH);
    }

    @Override
    public void onShow() {
        runDiagnostics();
    }

    private void runDiagnostics() {
        dllRow.reset();
        defenderRow.reset();
        prelaunchRow.reset();
        jarRow.reset();
        fixAllBtn.setVisible(false);
        summaryLabel.setText("Checking installation health...");

        new SwingWorker<Void, Void>() {
            boolean dllOk, defenderOk, prelaunchOk, jarOk;
            String dllHint, defenderHint, prelaunchHint, jarHint;
            String dllDetail, defenderDetail, prelaunchDetail, jarDetail;

            @Override
            protected Void doInBackground() {
                // ── Check 1: DLL files ──────────────────────────────────
                File dllDir = Main.getPreferredPersistentDllDir();
                if (dllDir.exists() && dllDir.isDirectory()) {
                    File[] dlls = dllDir.listFiles((dir, name) -> name.endsWith(".dll"));
                    if (dlls != null && dlls.length > 0) {
                        dllOk = true;
                        dllDetail = dllDir.getAbsolutePath() + " (" + dlls.length + " file" + (dlls.length != 1 ? "s" : "") + ")";
                    } else {
                        dllOk = false;
                        dllHint = "No DLL files found. Antivirus may have quarantined them.";
                        dllDetail = dllDir.getAbsolutePath();
                    }
                } else {
                    dllOk = false;
                    dllHint = "DLL directory does not exist.";
                    dllDetail = dllDir.getAbsolutePath();
                }

                // ── Check 2: Defender exclusion ─────────────────────────
                if (Main.isWindows()) {
                    String exclusionsKey = "HKLM\\SOFTWARE\\Microsoft\\Windows Defender\\Exclusions\\Paths";
                    boolean dirExcluded = Main.isDefenderExclusionPresent(exclusionsKey, dllDir.getAbsolutePath());
                    File stableJar = window.getStableJar();
                    boolean jarExcluded = stableJar == null ||
                        Main.isDefenderExclusionPresent(exclusionsKey,
                            Main.normalizePathForDefenderExclusionCheck(stableJar.getAbsolutePath()));
                    defenderOk = dirExcluded && jarExcluded;
                    if (!defenderOk) {
                        defenderHint = "Exclusion missing. Defender may quarantine files on next launch.";
                        if (!dirExcluded) defenderDetail = "Missing: " + dllDir.getAbsolutePath();
                        else if (!jarExcluded) defenderDetail = "Missing: " + stableJar.getAbsolutePath();
                    } else {
                        defenderDetail = "Exclusions are active";
                    }
                } else {
                    defenderOk = true;
                    defenderDetail = "Not applicable (not Windows)";
                }

                // ── Check 3: Pre-launch command ─────────────────────────
                File config = window.getInstanceConfig();
                if (config != null && config.exists()) {
                    try {
                        String existing = readExistingPrelaunch(config);
                        if (existing != null && !existing.isEmpty()) {
                            boolean hasOur = false;
                            List<String> parts = Main.splitPreLaunchCommands(existing);
                            for (String part : parts) {
                                if (Main.isOurPreLaunchSegment(part)) {
                                    hasOur = true;
                                    break;
                                }
                            }
                            prelaunchOk = hasOur;
                            if (!hasOur) {
                                prelaunchHint = "Pre-launch command exists but does not contain " + window.getProjectName() + ".";
                            }
                            prelaunchDetail = truncate(existing, 60);
                        } else {
                            prelaunchOk = false;
                            prelaunchHint = "No pre-launch command configured.";
                            prelaunchDetail = config.getAbsolutePath();
                        }
                    } catch (Exception e) {
                        prelaunchOk = false;
                        prelaunchHint = "Could not read config: " + e.getMessage();
                    }
                } else {
                    prelaunchOk = false;
                    prelaunchHint = "Instance config file not found.";
                }

                // ── Check 4: Stable JAR ─────────────────────────────────
                File stableJar = window.getStableJar();
                if (stableJar != null && stableJar.exists() && stableJar.isFile()) {
                    jarOk = true;
                    jarDetail = stableJar.getAbsolutePath();
                } else {
                    jarOk = false;
                    jarHint = "Stable JAR file is missing. It may have been deleted or moved.";
                    jarDetail = stableJar != null ? stableJar.getAbsolutePath() : "Unknown path";
                }

                return null;
            }

            @Override
            protected void done() {
                updateRow(dllRow, dllOk, dllHint, dllDetail, "Re-extract DLLs", () -> fixDlls());
                updateRow(defenderRow, defenderOk, defenderHint, defenderDetail, "Add Exclusion", () -> fixDefender());
                updateRow(prelaunchRow, prelaunchOk, prelaunchHint, prelaunchDetail, "Re-install Command", () -> fixPrelaunch());
                updateRow(jarRow, jarOk, jarHint, jarDetail, "Re-copy JAR", () -> fixJar());

                int issues = (dllOk ? 0 : 1) + (defenderOk ? 0 : 1) + (prelaunchOk ? 0 : 1) + (jarOk ? 0 : 1);
                if (issues == 0) {
                    summaryLabel.setText("All checks passed. Installation looks healthy.");
                    summaryLabel.setForeground(Theme.SUCCESS);
                    fixAllBtn.setVisible(false);
                } else {
                    summaryLabel.setText(issues + " issue" + (issues != 1 ? "s" : "") + " found.");
                    summaryLabel.setForeground(Theme.WARNING);
                    fixAllBtn.setVisible(true);
                }
            }
        }.execute();
    }

    private void updateRow(DiagRow row, boolean ok, String hint, String detail, String fixLabel, Runnable fixAction) {
        if (ok) {
            row.setState(DiagRow.State.OK, "OK");
        } else {
            row.setState(DiagRow.State.PROBLEM, hint != null ? hint : "Issue detected");
            row.addFix(fixLabel, fixAction);
        }
        if (detail != null) row.setDetail(detail);
    }

    private String readExistingPrelaunch(File config) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(config));
        String line;
        String result = null;
        while ((line = reader.readLine()) != null) {
            if (config.getName().endsWith(".cfg") && line.startsWith("PreLaunchCommand=")) {
                result = line.substring("PreLaunchCommand=".length()).trim();
                break;
            } else if (config.getName().endsWith(".json") && line.trim().startsWith("\"preLaunchCommand\"")) {
                result = Main.extractJsonStringValue(line);
                break;
            }
        }
        reader.close();
        return result;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    // ── Fix actions ─────────────────────────────────────────────────────

    private void fixDlls() {
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                try {
                    List<Path> dlls = Main.extractEmbeddedDlls();
                    return dlls != null && !dlls.isEmpty();
                } catch (Exception e) { return false; }
            }
            @Override protected void done() { runDiagnostics(); }
        }.execute();
    }

    private void fixDefender() {
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                Main.DefenderExclusionResult r = Main.ensureDefenderExclusionWithSingleUac(
                    Main.getPreferredPersistentDllDir(), window.getStableJar());
                return r != null && r.success;
            }
            @Override protected void done() { runDiagnostics(); }
        }.execute();
    }

    private void fixPrelaunch() {
        // Re-run the install flow — redirect to install
        window.showScreen(InstallerWindow.SCREEN_INSTALL);
        InstallFlow flow = window.getScreen(InstallFlow.class);
        if (flow != null) flow.onShow();
    }

    private void fixJar() {
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                try {
                    String jarPath = Main.getJarPath();
                    File jarFile = new File(jarPath);
                    File stableJar = window.getStableJar();
                    if (jarFile.isFile() && stableJar != null) {
                        java.nio.file.Files.copy(jarFile.toPath(), stableJar.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        return true;
                    }
                    return false;
                } catch (Exception e) { return false; }
            }
            @Override protected void done() { runDiagnostics(); }
        }.execute();
    }

    private void fixAll() {
        fixAllBtn.setEnabled(false);
        summaryLabel.setText("Fixing issues...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                // Fix JAR first
                try {
                    String jarPath = Main.getJarPath();
                    File jarFile = new File(jarPath);
                    File stableJar = window.getStableJar();
                    if (jarFile.isFile() && stableJar != null) {
                        java.nio.file.Files.copy(jarFile.toPath(), stableJar.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (Exception ignored) {}

                // Fix DLLs
                try { Main.extractEmbeddedDlls(); } catch (Exception ignored) {}

                // Fix Defender
                if (Main.isWindows()) {
                    Main.ensureDefenderExclusionWithSingleUac(
                        Main.getPreferredPersistentDllDir(), window.getStableJar());
                }

                return null;
            }

            @Override
            protected void done() {
                fixAllBtn.setEnabled(true);
                runDiagnostics();
            }
        }.execute();
    }

    // ═════════════════════════════════════════════════════════════════════
    //  DiagRow widget
    // ═════════════════════════════════════════════════════════════════════

    private static class DiagRow extends JPanel {
        enum State { CHECKING, OK, PROBLEM }

        private final JLabel iconLabel;
        private final JLabel titleLabel;
        private final JLabel statusLabel;
        private final JLabel detailLabel;
        private final JPanel actionPanel;

        DiagRow(String title) {
            setLayout(new BorderLayout(8, 0));
            setBackground(Theme.BG);
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
            setAlignmentX(Component.LEFT_ALIGNMENT);

            iconLabel = new JLabel(Theme.pendingIcon());
            iconLabel.setPreferredSize(new Dimension(20, 20));
            add(iconLabel, BorderLayout.WEST);

            JPanel center = new JPanel();
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            center.setBackground(Theme.BG);

            titleLabel = new JLabel(title);
            titleLabel.setFont(Theme.BODY_FONT);
            titleLabel.setForeground(Theme.FG);
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            center.add(titleLabel);

            statusLabel = new JLabel("Checking...");
            statusLabel.setFont(Theme.SMALL_FONT);
            statusLabel.setForeground(Theme.MUTED);
            statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            center.add(statusLabel);

            detailLabel = new JLabel(" ");
            detailLabel.setFont(Theme.SMALL_FONT);
            detailLabel.setForeground(Theme.SUBTLE);
            detailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            detailLabel.setVisible(false);
            center.add(detailLabel);

            add(center, BorderLayout.CENTER);

            actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            actionPanel.setBackground(Theme.BG);
            add(actionPanel, BorderLayout.EAST);
        }

        void reset() {
            iconLabel.setIcon(Theme.pendingIcon());
            statusLabel.setText("Checking...");
            statusLabel.setForeground(Theme.MUTED);
            detailLabel.setVisible(false);
            actionPanel.removeAll();
            actionPanel.revalidate();
        }

        void setState(State state, String status) {
            statusLabel.setText(status);
            switch (state) {
                case CHECKING:
                    iconLabel.setIcon(Theme.infoIcon());
                    statusLabel.setForeground(Theme.INFO);
                    break;
                case OK:
                    iconLabel.setIcon(Theme.successIcon());
                    statusLabel.setForeground(Theme.SUCCESS);
                    break;
                case PROBLEM:
                    iconLabel.setIcon(Theme.warningIcon());
                    statusLabel.setForeground(Theme.WARNING);
                    break;
            }
        }

        void setDetail(String text) {
            if (text != null && !text.isEmpty()) {
                detailLabel.setText(text);
                detailLabel.setVisible(true);
            } else {
                detailLabel.setVisible(false);
            }
        }

        void addFix(String label, Runnable action) {
            JButton btn = Theme.createButton(label);
            btn.setFont(Theme.SMALL_FONT);
            btn.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
            btn.addActionListener(e -> action.run());
            actionPanel.add(btn);
            actionPanel.revalidate();
        }
    }
}
