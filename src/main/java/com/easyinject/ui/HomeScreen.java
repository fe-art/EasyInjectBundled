package com.easyinject.ui;

import com.easyinject.Main;

import java.awt.*;
import java.io.File;
import javax.swing.*;

/**
 * Home screen shown when the installer launches.
 * Detects the instance/launcher and offers Install, Repair, and Uninstall actions.
 */
public class HomeScreen extends InstallerWindow.ScreenPanel {

    private final JPanel infoCard;
    private final JLabel launcherLabel;
    private final JLabel instanceLabel;
    private final JLabel pathLabel;
    private final JLabel mcsrGuidance;
    private final JPanel noInstancePanel;
    private final JButton installBtn;
    private final JButton repairBtn;
    private final JButton uninstallBtn;

    public HomeScreen(InstallerWindow window) {
        super(window);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel center = Theme.createVBox();
        center.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ── Instance info card ──────────────────────────────────────────
        infoCard = Theme.createCard();
        // No fixed max height: let the wrapping path/MCSR labels size the card.

        launcherLabel = Theme.createLabel("Launcher: —", Theme.BODY_FONT, Theme.FG);
        instanceLabel = Theme.createLabel("Instance: —", Theme.BODY_FONT, Theme.INFO);
        pathLabel = Theme.createLabel("Path: —", Theme.SMALL_FONT, Theme.SUBTLE);

        mcsrGuidance = new JLabel();
        mcsrGuidance.setAlignmentX(Component.LEFT_ALIGNMENT);
        mcsrGuidance.setVisible(false);

        infoCard.add(launcherLabel);
        infoCard.add(Box.createVerticalStrut(4));
        infoCard.add(instanceLabel);
        infoCard.add(Box.createVerticalStrut(4));
        infoCard.add(pathLabel);
        infoCard.add(mcsrGuidance);

        center.add(infoCard);
        center.add(Box.createVerticalStrut(16));

        // ── No-instance warning ─────────────────────────────────────────
        noInstancePanel = Theme.createCard();
        noInstancePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        JLabel warningTitle = Theme.createLabel("Setup Required", Theme.HEADING_FONT, Theme.WARNING);
        warningTitle.setIcon(Theme.warningIcon());
        warningTitle.setIconTextGap(8);

        JLabel instructions = new JLabel(
            "<html><body style='width: 440px; font-family: Segoe UI, sans-serif; color: #c7ced6;'>" +
            "<p style='margin:6px 0 4px 0;'>This JAR was not found inside a launcher instance folder.</p>" +
            "<p style='margin:0 0 4px 0;'>To install " + window.getProjectName() + ":</p>" +
            "<ol style='margin:0; padding-left:18px;'>" +
            "<li>Open your instance folder in <b>Prism Launcher</b>, <b>MultiMC</b>, or <b>ATLauncher</b></li>" +
            "<li>Copy this JAR file into that folder</li>" +
            "<li>Double-click it from there</li>" +
            "</ol>" +
            "</body></html>"
        );
        instructions.setAlignmentX(Component.LEFT_ALIGNMENT);

        noInstancePanel.add(warningTitle);
        noInstancePanel.add(Box.createVerticalStrut(4));
        noInstancePanel.add(instructions);

        center.add(noInstancePanel);

        add(center, BorderLayout.CENTER);

        // ── Action buttons ──────────────────────────────────────────────
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttonRow.setBackground(Theme.BG);
        buttonRow.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        installBtn = Theme.createAccentButton("Install", Theme.SUCCESS);
        repairBtn = Theme.createButton("Repair");
        uninstallBtn = Theme.createButton("Uninstall");

        installBtn.setPreferredSize(new Dimension(120, 36));
        repairBtn.setPreferredSize(new Dimension(120, 36));
        uninstallBtn.setPreferredSize(new Dimension(120, 36));

        installBtn.addActionListener(e -> {
            if (window.isMcsrBlocked()) {
                Main.showMcsrLauncherWarning();
                return;
            }
            window.showScreen(InstallerWindow.SCREEN_INSTALL);
            InstallFlow flow = window.getScreen(InstallFlow.class);
            if (flow != null) flow.onShow();
        });

        repairBtn.addActionListener(e -> {
            if (window.isMcsrBlocked()) {
                Main.showMcsrLauncherWarning();
                return;
            }
            window.showScreen(InstallerWindow.SCREEN_REPAIR);
            RepairScreen repair = window.getScreen(RepairScreen.class);
            if (repair != null) repair.onShow();
        });

        uninstallBtn.addActionListener(e -> {
            if (window.isMcsrBlocked()) {
                Main.showMcsrLauncherWarning();
                return;
            }
            window.showScreen(InstallerWindow.SCREEN_UNINSTALL);
            UninstallScreen uninstall = window.getScreen(UninstallScreen.class);
            if (uninstall != null) uninstall.onShow();
        });

        buttonRow.add(installBtn);
        buttonRow.add(repairBtn);
        buttonRow.add(uninstallBtn);

        add(buttonRow, BorderLayout.SOUTH);

        // ── Detect instance ─────────────────────────────────────────────
        detectInstance();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        // Make Enter trigger Install when this screen is realized.
        JRootPane rp = getRootPane();
        if (rp != null) rp.setDefaultButton(installBtn);
    }

    /**
     * Small holder carrying the detection result from the background worker
     * back to the EDT.
     */
    private static class DetectionResult {
        String launcherType;
        String instanceName;
        File instanceDir;
        File instanceConfig;
        File stableJar;
        File jarDir;
        Main.ModrinthInstance modrinth;
        boolean mcsrBlocked;
        boolean noInstance;
    }

    /**
     * Render an initial "detecting" state then kick off background detection.
     */
    private void detectInstance() {
        // Initial neutral state while detection runs off the EDT.
        infoCard.setVisible(true);
        noInstancePanel.setVisible(false);
        launcherLabel.setText("Detecting instance…");
        instanceLabel.setText("Instance: —");
        pathLabel.setText("Path: —");
        pathLabel.setToolTipText(null);
        pathLabel.setVisible(true);
        mcsrGuidance.setVisible(false);
        installBtn.setEnabled(false);
        repairBtn.setEnabled(false);
        uninstallBtn.setEnabled(false);

        new SwingWorker<DetectionResult, Void>() {
            @Override
            protected DetectionResult doInBackground() {
                DetectionResult r = new DetectionResult();
                try {
                    String jarPath = Main.getJarPath();
                    File jarFile = new File(jarPath);
                    if (!jarFile.isFile()) {
                        r.noInstance = true;
                        return r;
                    }

                    File jarDir = jarFile.getParentFile();
                    r.jarDir = jarDir;

                    // Create/copy stable JAR (I/O — stays off the EDT)
                    String stableJarName = Main.getStableSelfJarFileName();
                    File stableJar = new File(jarDir, stableJarName);
                    if (!stableJar.getAbsolutePath().equalsIgnoreCase(jarFile.getAbsolutePath())) {
                        try {
                            java.nio.file.Files.copy(jarFile.toPath(), stableJar.toPath(),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        } catch (Throwable copyErr) {
                            // If we can't copy, use the original jar
                            stableJar = jarFile;
                        }
                    }
                    r.stableJar = stableJar;

                    // Resolve instance root directory
                    File instanceDir = jarDir;
                    if (Main.isMinecraftDir(jarDir)) {
                        instanceDir = jarDir.getParentFile();
                    }
                    r.instanceDir = instanceDir;

                    // (b) MCSR Launcher first — block manual install.
                    if (Main.isMcsrLauncherInstance(instanceDir)) {
                        r.mcsrBlocked = true;
                        r.instanceName = instanceDir != null ? instanceDir.getName() : null;
                        r.launcherType = "MCSR Launcher";
                        return r;
                    }

                    // (c) Modrinth App.
                    Main.ModrinthInstance modrinth = Main.detectModrinthInstance(stableJar.getParentFile());
                    if (modrinth != null) {
                        r.modrinth = modrinth;
                        r.launcherType = "Modrinth App";
                        r.instanceName = modrinth.profilePath;
                        return r;
                    }

                    // (d) Prism/MultiMC (instance.cfg) vs ATLauncher (instance.json).
                    File instanceCfg = new File(instanceDir, "instance.cfg");
                    File instanceJson = new File(instanceDir, "instance.json");

                    if (instanceCfg.exists() && instanceCfg.isFile()) {
                        r.instanceConfig = instanceCfg;
                        r.instanceName = instanceDir.getName();
                        r.launcherType = detectLauncherType(instanceCfg);
                    } else if (instanceJson.exists() && instanceJson.isFile()) {
                        r.instanceConfig = instanceJson;
                        r.instanceName = instanceDir.getName();
                        r.launcherType = "ATLauncher";
                    } else {
                        // (e) Nothing recognised.
                        r.noInstance = true;
                    }
                } catch (Exception e) {
                    r.noInstance = true;
                }
                return r;
            }

            @Override
            protected void done() {
                DetectionResult r;
                try {
                    r = get();
                } catch (Exception e) {
                    showNoInstance();
                    return;
                }

                // Apply shared state to the window.
                window.setJarDir(r.jarDir);
                window.setStableJar(r.stableJar);
                window.setInstanceDir(r.instanceDir);
                window.setInstanceConfig(r.instanceConfig);
                window.setInstanceName(r.instanceName);
                window.setDetectedLauncher(r.launcherType);
                window.setModrinthInstance(r.modrinth);
                window.setMcsrBlocked(r.mcsrBlocked);

                if (r.mcsrBlocked) {
                    showMcsrManaged();
                } else if (r.noInstance) {
                    showNoInstance();
                } else {
                    showInstanceInfo();
                }
            }
        }.execute();
    }

    /**
     * Try to determine if this is Prism Launcher or MultiMC from instance.cfg.
     */
    private String detectLauncherType(File instanceCfg) {
        try {
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(instanceCfg));
            String line;
            while ((line = reader.readLine()) != null) {
                // Prism Launcher instance.cfg often contains "ManagedPack" or different keys
                // Both MultiMC and Prism use instance.cfg, but Prism is more common now
                if (line.startsWith("ManagedPackType=")) {
                    reader.close();
                    return "Prism Launcher";
                }
            }
            reader.close();
        } catch (Exception ignored) {}

        // Check parent folders for hints
        File parent = instanceCfg.getParentFile();
        while (parent != null) {
            String name = parent.getName().toLowerCase();
            if (name.contains("prism")) return "Prism Launcher";
            if (name.contains("multimc")) return "MultiMC";
            parent = parent.getParentFile();
        }

        return "Prism Launcher / MultiMC";
    }

    private void showInstanceInfo() {
        launcherLabel.setText("Launcher:  " + window.getDetectedLauncher());
        instanceLabel.setText("Instance:  " + window.getInstanceName());

        String fullPath = window.getInstanceDir().getAbsolutePath();
        // HTML label with fixed width so long Windows paths wrap instead of clipping.
        pathLabel.setText("<html><div style='width:480px;'>Path:&nbsp;&nbsp;"
            + Theme.escapeHtml(fullPath) + "</div></html>");
        pathLabel.setToolTipText(fullPath);
        pathLabel.setVisible(true);
        mcsrGuidance.setVisible(false);

        infoCard.setVisible(true);
        noInstancePanel.setVisible(false);
        installBtn.setEnabled(true);
        repairBtn.setEnabled(true);
        uninstallBtn.setEnabled(true);
    }

    private void showNoInstance() {
        infoCard.setVisible(false);
        noInstancePanel.setVisible(true);
        installBtn.setEnabled(false);
        repairBtn.setEnabled(false);
        uninstallBtn.setEnabled(false);
    }

    /**
     * Detected an MCSR Launcher instance: manual install is not supported here.
     * Show an info state and let the buttons surface the warning dialog.
     */
    private void showMcsrManaged() {
        launcherLabel.setText("Launcher:  MCSR Launcher");
        instanceLabel.setText("Instance:  " + (window.getInstanceName() != null ? window.getInstanceName() : "—"));

        // Show the actual path (or hide if unknown) and put the guidance in its
        // own wrapping label instead of stuffing a sentence into the path slot.
        File mcsrDir = window.getInstanceDir();
        if (mcsrDir != null) {
            String fullPath = mcsrDir.getAbsolutePath();
            pathLabel.setText("<html><div style='width:480px;'>Path:&nbsp;&nbsp;"
                + Theme.escapeHtml(fullPath) + "</div></html>");
            pathLabel.setToolTipText(fullPath);
            pathLabel.setVisible(true);
        } else {
            pathLabel.setVisible(false);
        }

        mcsrGuidance.setText("<html><div style='width:480px; color:#c7ced6;'>"
            + "Managed by MCSR Launcher &mdash; enable Toolscreen from the launcher's instance tools."
            + "</div></html>");
        mcsrGuidance.setFont(Theme.SMALL_FONT);
        mcsrGuidance.setForeground(Theme.SUBTLE);
        mcsrGuidance.setVisible(true);

        infoCard.setVisible(true);
        noInstancePanel.setVisible(false);
        // Buttons stay enabled so their handlers can show the MCSR warning dialog.
        installBtn.setEnabled(true);
        repairBtn.setEnabled(true);
        uninstallBtn.setEnabled(true);
    }
}
