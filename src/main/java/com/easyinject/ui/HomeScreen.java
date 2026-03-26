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
        infoCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        launcherLabel = Theme.createLabel("Launcher: —", Theme.BODY_FONT, Theme.FG);
        instanceLabel = Theme.createLabel("Instance: —", Theme.BODY_FONT, Theme.INFO);
        pathLabel = Theme.createLabel("Path: —", Theme.SMALL_FONT, Theme.SUBTLE);

        infoCard.add(launcherLabel);
        infoCard.add(Box.createVerticalStrut(4));
        infoCard.add(instanceLabel);
        infoCard.add(Box.createVerticalStrut(4));
        infoCard.add(pathLabel);

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
            window.showScreen(InstallerWindow.SCREEN_INSTALL);
            InstallFlow flow = window.getScreen(InstallFlow.class);
            if (flow != null) flow.onShow();
        });

        repairBtn.addActionListener(e -> {
            window.showScreen(InstallerWindow.SCREEN_REPAIR);
            RepairScreen repair = window.getScreen(RepairScreen.class);
            if (repair != null) repair.onShow();
        });

        uninstallBtn.addActionListener(e -> {
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

    /**
     * Detect the launcher instance from the current JAR location and populate shared state.
     */
    private void detectInstance() {
        try {
            String jarPath = Main.getJarPath();
            File jarFile = new File(jarPath);
            if (!jarFile.isFile()) {
                showNoInstance();
                return;
            }

            File jarDir = jarFile.getParentFile();
            window.setJarDir(jarDir);

            // Create/copy stable JAR
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
            window.setStableJar(stableJar);

            // Resolve instance root directory
            File instanceDir = jarDir;
            String subfolderPrefix = "";
            String dirName = jarDir.getName().toLowerCase();
            if (dirName.equals("minecraft") || dirName.equals(".minecraft")) {
                instanceDir = jarDir.getParentFile();
                subfolderPrefix = jarDir.getName() + "/";
            }

            // Look for instance config
            File instanceCfg = new File(instanceDir, "instance.cfg");
            File instanceJson = new File(instanceDir, "instance.json");

            if (instanceCfg.exists() && instanceCfg.isFile()) {
                window.setInstanceDir(instanceDir);
                window.setInstanceConfig(instanceCfg);
                window.setInstanceName(instanceDir.getName());
                window.setDetectedLauncher(detectLauncherType(instanceCfg));
                showInstanceInfo();
            } else if (instanceJson.exists() && instanceJson.isFile()) {
                window.setInstanceDir(instanceDir);
                window.setInstanceConfig(instanceJson);
                window.setInstanceName(instanceDir.getName());
                window.setDetectedLauncher("ATLauncher");
                showInstanceInfo();
            } else {
                showNoInstance();
            }
        } catch (Exception e) {
            showNoInstance();
        }
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
        pathLabel.setText("Path:  " + window.getInstanceDir().getAbsolutePath());

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
}
