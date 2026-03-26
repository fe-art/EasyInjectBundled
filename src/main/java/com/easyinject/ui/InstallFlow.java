package com.easyinject.ui;

import com.easyinject.Main;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/**
 * Three-step install wizard: Security → Installation → Done.
 * Uses a nested CardLayout to switch between steps.
 */
public class InstallFlow extends InstallerWindow.ScreenPanel {

    private static final String STEP_SECURITY = "step-security";
    private static final String STEP_INSTALL  = "step-install";
    private static final String STEP_DONE     = "step-done";

    private final CardLayout stepLayout;
    private final JPanel stepContent;
    private final StepIndicator stepIndicator;

    // Panels for each step
    private SecurityStepPanel securityPanel;
    private InstallStepPanel installPanel;
    private DoneStepPanel donePanel;

    public InstallFlow(InstallerWindow window) {
        super(window);
        setLayout(new BorderLayout());

        // ── Step indicator (breadcrumb) ─────────────────────────────────
        stepIndicator = new StepIndicator();
        stepIndicator.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.DIVIDER),
            BorderFactory.createEmptyBorder(10, 24, 10, 24)
        ));
        add(stepIndicator, BorderLayout.NORTH);

        // ── Step content ────────────────────────────────────────────────
        stepLayout = new CardLayout();
        stepContent = new JPanel(stepLayout);
        stepContent.setBackground(Theme.BG);

        securityPanel = new SecurityStepPanel();
        installPanel = new InstallStepPanel();
        donePanel = new DoneStepPanel();

        stepContent.add(securityPanel, STEP_SECURITY);
        stepContent.add(installPanel, STEP_INSTALL);
        stepContent.add(donePanel, STEP_DONE);

        add(stepContent, BorderLayout.CENTER);
    }

    @Override
    public void onShow() {
        goToStep(1);
        securityPanel.startChecks();
    }

    private void goToStep(int step) {
        stepIndicator.setCurrentStep(step);
        switch (step) {
            case 1: stepLayout.show(stepContent, STEP_SECURITY); break;
            case 2: stepLayout.show(stepContent, STEP_INSTALL); installPanel.onShow(); break;
            case 3: stepLayout.show(stepContent, STEP_DONE); break;
        }
        stepContent.revalidate();
        stepContent.repaint();
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Step Indicator widget
    // ═════════════════════════════════════════════════════════════════════

    private class StepIndicator extends JPanel {
        private int currentStep = 1;
        private final String[] stepNames = {"Security", "Installation", "Done"};
        private final int[] stepStates = {0, 0, 0}; // 0=pending, 1=active, 2=complete, 3=error

        StepIndicator() {
            setBackground(Theme.BG_DARKER);
            setOpaque(true);
            setPreferredSize(new Dimension(0, 36));
        }

        void setCurrentStep(int step) {
            currentStep = step;
            for (int i = 0; i < stepStates.length; i++) {
                if (i < step - 1) stepStates[i] = 2; // completed
                else if (i == step - 1) stepStates[i] = 1; // active
                else stepStates[i] = 0; // pending
            }
            repaint();
        }

        void setStepError(int step) {
            if (step >= 1 && step <= stepStates.length) {
                stepStates[step - 1] = 3;
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            int totalWidth = getWidth();
            int y = getHeight() / 2;
            int segWidth = totalWidth / stepNames.length;

            for (int i = 0; i < stepNames.length; i++) {
                int cx = segWidth * i + segWidth / 2;
                String label = (i + 1) + ". " + stepNames[i];

                Font font;
                Color color;
                switch (stepStates[i]) {
                    case 1: // active
                        font = Theme.HEADING_FONT;
                        color = Theme.FG;
                        break;
                    case 2: // complete
                        font = Theme.BODY_FONT;
                        color = Theme.SUCCESS;
                        label = "\u2713 " + stepNames[i]; // checkmark
                        break;
                    case 3: // error
                        font = Theme.BODY_FONT;
                        color = Theme.ERROR;
                        label = "\u2717 " + stepNames[i]; // X
                        break;
                    default: // pending
                        font = Theme.BODY_FONT;
                        color = Theme.MUTED;
                        break;
                }

                g2.setFont(font);
                g2.setColor(color);
                FontMetrics fm = g2.getFontMetrics();
                int textX = cx - fm.stringWidth(label) / 2;
                int textY = y + fm.getAscent() / 2 - 1;
                g2.drawString(label, textX, textY);

                // Draw arrow between steps
                if (i < stepNames.length - 1) {
                    int arrowX = segWidth * (i + 1);
                    g2.setColor(Theme.MUTED);
                    g2.setFont(Theme.BODY_FONT);
                    g2.drawString("\u2192", arrowX - 8, textY); // →
                }
            }

            g2.dispose();
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  StatusRow widget — reusable status line with icon + label + action
    // ═════════════════════════════════════════════════════════════════════

    private static class StatusRow extends JPanel {
        enum State { PENDING, CHECKING, SUCCESS, WARNING, ERROR }

        private final JLabel iconLabel;
        private final JLabel titleLabel;
        private final JLabel statusLabel;
        private final JLabel detailLabel;
        private final JPanel actionPanel;

        StatusRow(String title) {
            setLayout(new BorderLayout(8, 0));
            setBackground(Theme.BG);
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            setAlignmentX(Component.LEFT_ALIGNMENT);

            iconLabel = new JLabel();
            iconLabel.setPreferredSize(new Dimension(20, 20));
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            add(iconLabel, BorderLayout.WEST);

            JPanel center = new JPanel();
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            center.setBackground(Theme.BG);
            center.setOpaque(true);

            titleLabel = new JLabel(title);
            titleLabel.setFont(Theme.BODY_FONT);
            titleLabel.setForeground(Theme.FG);
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            center.add(titleLabel);

            statusLabel = new JLabel(" ");
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
            actionPanel.setOpaque(true);
            add(actionPanel, BorderLayout.EAST);

            setState(State.PENDING, "Waiting...");
        }

        void setState(State state, String status) {
            statusLabel.setText(status);
            switch (state) {
                case PENDING:
                    iconLabel.setIcon(Theme.pendingIcon());
                    statusLabel.setForeground(Theme.MUTED);
                    break;
                case CHECKING:
                    iconLabel.setIcon(Theme.infoIcon());
                    statusLabel.setForeground(Theme.INFO);
                    break;
                case SUCCESS:
                    iconLabel.setIcon(Theme.successIcon());
                    statusLabel.setForeground(Theme.SUCCESS);
                    break;
                case WARNING:
                    iconLabel.setIcon(Theme.warningIcon());
                    statusLabel.setForeground(Theme.WARNING);
                    break;
                case ERROR:
                    iconLabel.setIcon(Theme.errorIcon());
                    statusLabel.setForeground(Theme.ERROR);
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

        void clearActions() {
            actionPanel.removeAll();
            actionPanel.revalidate();
        }

        void addAction(JButton btn) {
            actionPanel.add(btn);
            actionPanel.revalidate();
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Step 1: Security Checks
    // ═════════════════════════════════════════════════════════════════════

    private class SecurityStepPanel extends JPanel {
        private final StatusRow sacRow;
        private final StatusRow defenderRow;
        private final JButton continueBtn;
        private final JButton skipBtn;
        private final JLabel skipWarning;

        private boolean sacPassed = false;
        private boolean defenderPassed = false;
        private boolean defenderSkipped = false;

        SecurityStepPanel() {
            setLayout(new BorderLayout());
            setBackground(Theme.BG);
            setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

            JPanel body = Theme.createVBox();

            JLabel title = Theme.createLabel("Security Exceptions", Theme.HEADING_FONT, Theme.FG);
            body.add(title);
            body.add(Box.createVerticalStrut(4));
            JLabel desc = Theme.createLabel(
                "We need to exclude " + window.getProjectName() + " from security software to prevent false positives.",
                Theme.SMALL_FONT, Theme.SUBTLE);
            body.add(desc);
            body.add(Box.createVerticalStrut(12));

            sacRow = new StatusRow("Smart App Control");
            body.add(sacRow);
            body.add(Theme.createDivider());

            defenderRow = new StatusRow("Windows Defender Exclusion");
            body.add(defenderRow);

            body.add(Box.createVerticalGlue());

            add(body, BorderLayout.CENTER);

            // ── Bottom buttons ──────────────────────────────────────────
            JPanel bottom = Theme.createVBox();

            skipWarning = Theme.createLabel(
                "Skipping may cause Windows Defender to quarantine files and break the installation.",
                Theme.SMALL_FONT, Theme.WARNING);
            skipWarning.setVisible(false);
            skipWarning.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
            bottom.add(skipWarning);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            buttons.setBackground(Theme.BG);

            JButton backBtn = Theme.createButton("Back");
            backBtn.addActionListener(e -> window.showScreen(InstallerWindow.SCREEN_HOME));

            skipBtn = Theme.createButton("Skip");
            skipBtn.addActionListener(e -> {
                skipWarning.setVisible(true);
                defenderSkipped = true;
                defenderRow.setState(StatusRow.State.WARNING, "Skipped by user");
                updateContinueState();
                bottom.revalidate();
                bottom.repaint();
            });

            continueBtn = Theme.createAccentButton("Continue", Theme.SUCCESS);
            continueBtn.setEnabled(false);
            continueBtn.addActionListener(e -> goToStep(2));

            buttons.add(backBtn);
            buttons.add(skipBtn);
            buttons.add(continueBtn);
            bottom.add(buttons);

            add(bottom, BorderLayout.SOUTH);
        }

        void startChecks() {
            sacPassed = false;
            defenderPassed = false;
            defenderSkipped = false;
            skipWarning.setVisible(false);
            continueBtn.setEnabled(false);
            skipBtn.setEnabled(true);

            sacRow.setState(StatusRow.State.CHECKING, "Checking...");
            sacRow.clearActions();
            defenderRow.setState(StatusRow.State.PENDING, "Waiting...");
            defenderRow.clearActions();

            // Check SAC on background thread
            new SwingWorker<Main.SmartAppControlState, Void>() {
                @Override
                protected Main.SmartAppControlState doInBackground() {
                    return Main.getSmartAppControlState();
                }

                @Override
                protected void done() {
                    try {
                        Main.SmartAppControlState state = get();
                        handleSacResult(state);
                    } catch (Exception e) {
                        sacPassed = true; // Unknown = don't block
                        sacRow.setState(StatusRow.State.SUCCESS, "Not applicable");
                        startDefenderCheck();
                    }
                }
            }.execute();
        }

        private void handleSacResult(Main.SmartAppControlState state) {
            switch (state) {
                case DISABLED:
                case UNKNOWN:
                    sacPassed = true;
                    sacRow.setState(StatusRow.State.SUCCESS,
                        state == Main.SmartAppControlState.DISABLED ? "Disabled" : "Not detected");
                    startDefenderCheck();
                    break;

                case ENABLED:
                case EVALUATION:
                    sacPassed = false;
                    sacRow.setState(StatusRow.State.ERROR,
                        state == Main.SmartAppControlState.ENABLED
                            ? "Enabled \u2014 must be disabled to continue"
                            : "Evaluation mode \u2014 should be disabled");
                    sacRow.setDetail("Smart App Control blocks unsigned DLLs and cannot be bypassed with exclusions.");

                    JButton openBtn = Theme.createButton("Open Settings");
                    openBtn.addActionListener(e -> Main.openWindowsSecuritySmartAppControlUi());
                    sacRow.addAction(openBtn);

                    JButton recheckBtn = Theme.createButton("Re-check");
                    recheckBtn.addActionListener(e -> {
                        sacRow.clearActions();
                        sacRow.setDetail(null);
                        sacRow.setState(StatusRow.State.CHECKING, "Re-checking...");
                        startChecks(); // restart from the top
                    });
                    sacRow.addAction(recheckBtn);

                    if (state == Main.SmartAppControlState.EVALUATION) {
                        // Evaluation mode: allow continue but warn
                        sacPassed = true;
                        sacRow.setState(StatusRow.State.WARNING, "Evaluation mode \u2014 may block unsigned DLLs");
                        startDefenderCheck();
                    }
                    break;
            }
            updateContinueState();
        }

        private void startDefenderCheck() {
            defenderRow.setState(StatusRow.State.CHECKING, "Checking...");
            defenderRow.clearActions();

            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    if (!Main.isWindows()) return true;
                    File dllDir = Main.getPreferredPersistentDllDir();
                    File jar = window.getStableJar();
                    // Ensure DLL dir exists
                    if (!dllDir.exists()) dllDir.mkdirs();

                    String exclusionsKey = "HKLM\\SOFTWARE\\Microsoft\\Windows Defender\\Exclusions\\Paths";
                    boolean dirExcluded = Main.isDefenderExclusionPresent(exclusionsKey, dllDir.getAbsolutePath());
                    boolean jarExcluded = jar == null || Main.isDefenderExclusionPresent(exclusionsKey,
                        Main.normalizePathForDefenderExclusionCheck(jar.getAbsolutePath()));
                    return dirExcluded && jarExcluded;
                }

                @Override
                protected void done() {
                    try {
                        boolean excluded = get();
                        if (excluded) {
                            defenderPassed = true;
                            defenderRow.setState(StatusRow.State.SUCCESS, "Exclusion already present");
                            updateContinueState();
                        } else {
                            showDefenderConsentUI();
                        }
                    } catch (Exception e) {
                        showDefenderConsentUI();
                    }
                }
            }.execute();
        }

        private void showDefenderConsentUI() {
            defenderRow.setState(StatusRow.State.WARNING,
                "Exclusion needed for the DLL folder and JAR file");

            File dllDir = Main.getPreferredPersistentDllDir();
            defenderRow.setDetail("Folder: " + dllDir.getAbsolutePath());

            JButton addBtn = Theme.createAccentButton("Add Exclusion", Theme.INFO);
            addBtn.addActionListener(e -> {
                defenderRow.clearActions();
                defenderRow.setState(StatusRow.State.CHECKING, "Adding exclusion (UAC prompt)...");
                defenderRow.setDetail("Please accept the Windows elevation prompt.");

                new SwingWorker<Main.DefenderExclusionResult, Void>() {
                    @Override
                    protected Main.DefenderExclusionResult doInBackground() {
                        return Main.ensureDefenderExclusionWithSingleUac(
                            Main.getPreferredPersistentDllDir(), window.getStableJar());
                    }

                    @Override
                    protected void done() {
                        try {
                            Main.DefenderExclusionResult result = get();
                            if (result != null && result.success) {
                                defenderPassed = true;
                                defenderRow.setState(StatusRow.State.SUCCESS, "Exclusion added");
                                defenderRow.setDetail(null);
                            } else {
                                handleDefenderFailure(result != null ? result.details : "Unknown error");
                            }
                        } catch (Exception ex) {
                            handleDefenderFailure(ex.getMessage());
                        }
                        updateContinueState();
                    }
                }.execute();
            });
            defenderRow.addAction(addBtn);
        }

        private void handleDefenderFailure(String details) {
            defenderRow.setState(StatusRow.State.ERROR, "Failed to add exclusion");
            defenderRow.setDetail(details);
            defenderRow.clearActions();

            JButton openSecBtn = Theme.createButton("Open Windows Security");
            openSecBtn.addActionListener(e -> Main.openWindowsSecurityExclusionsUi());
            defenderRow.addAction(openSecBtn);

            JButton retryBtn = Theme.createButton("Retry");
            retryBtn.addActionListener(e -> {
                defenderRow.clearActions();
                defenderRow.setDetail(null);
                startDefenderCheck();
            });
            defenderRow.addAction(retryBtn);
        }

        private void updateContinueState() {
            boolean canContinue = sacPassed && (defenderPassed || defenderSkipped);
            continueBtn.setEnabled(canContinue);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Step 2: Installation
    // ═════════════════════════════════════════════════════════════════════

    private class InstallStepPanel extends JPanel {
        private final JLabel launcherInfo;
        private final JComboBox<String> launcherOverride;
        private final JPanel mergePanel;
        private final JRadioButton mergeKeep;
        private final JRadioButton mergeReplace;
        private final JLabel mergeExisting;

        private final StatusRow copyRow;
        private final StatusRow launcherCloseRow;
        private final StatusRow writeRow;
        private final StatusRow dllRow;

        private final JButton installBtn;
        private final JButton backBtn;

        private String existingPrelaunch = null;
        private boolean hasConflict = false;

        InstallStepPanel() {
            setLayout(new BorderLayout());
            setBackground(Theme.BG);
            setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

            JPanel body = Theme.createVBox();

            // ── Launcher info ───────────────────────────────────────────
            JLabel title = Theme.createLabel("Installation", Theme.HEADING_FONT, Theme.FG);
            body.add(title);
            body.add(Box.createVerticalStrut(8));

            JPanel launcherRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            launcherRow.setBackground(Theme.BG);
            launcherRow.setAlignmentX(Component.LEFT_ALIGNMENT);

            launcherInfo = Theme.createLabel("Detected: —", Theme.BODY_FONT, Theme.FG);
            launcherRow.add(launcherInfo);

            launcherOverride = new JComboBox<>(new String[]{"Prism Launcher", "MultiMC", "ATLauncher"});
            launcherOverride.setVisible(false);
            launcherOverride.setFont(Theme.BODY_FONT);
            launcherRow.add(launcherOverride);

            JButton changeBtn = Theme.createButton("Change");
            changeBtn.setFont(Theme.SMALL_FONT);
            changeBtn.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            changeBtn.addActionListener(e -> {
                launcherOverride.setVisible(!launcherOverride.isVisible());
                launcherRow.revalidate();
            });
            launcherRow.add(changeBtn);

            body.add(launcherRow);
            body.add(Box.createVerticalStrut(12));

            // ── Merge conflict panel (hidden by default) ────────────────
            mergePanel = Theme.createCard();
            mergePanel.setVisible(false);
            mergePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

            JLabel mergeTitle = Theme.createLabel("Existing Pre-Launch Command Detected", Theme.BODY_FONT, Theme.WARNING);
            mergeTitle.setIcon(Theme.warningIcon());
            mergeTitle.setIconTextGap(6);
            mergePanel.add(mergeTitle);
            mergePanel.add(Box.createVerticalStrut(4));

            mergeExisting = Theme.createLabel("", Theme.MONO_FONT, Theme.SUBTLE);
            mergePanel.add(mergeExisting);
            mergePanel.add(Box.createVerticalStrut(6));

            mergeKeep = new JRadioButton("Include existing command");
            mergeKeep.setFont(Theme.SMALL_FONT);
            mergeKeep.setForeground(Theme.FG);
            mergeKeep.setBackground(Theme.BG_DARKER);
            mergeKeep.setSelected(true);

            mergeReplace = new JRadioButton("Replace existing command");
            mergeReplace.setFont(Theme.SMALL_FONT);
            mergeReplace.setForeground(Theme.FG);
            mergeReplace.setBackground(Theme.BG_DARKER);

            ButtonGroup mergeGroup = new ButtonGroup();
            mergeGroup.add(mergeKeep);
            mergeGroup.add(mergeReplace);

            mergePanel.add(mergeKeep);
            mergePanel.add(mergeReplace);

            body.add(mergePanel);
            body.add(Box.createVerticalStrut(12));
            body.add(Theme.createDivider());
            body.add(Box.createVerticalStrut(8));

            // ── Progress rows ───────────────────────────────────────────
            copyRow = new StatusRow("Copy JAR to stable location");
            launcherCloseRow = new StatusRow("Close running launchers");
            writeRow = new StatusRow("Write pre-launch command");
            dllRow = new StatusRow("Extract DLLs");

            body.add(copyRow);
            body.add(launcherCloseRow);
            body.add(writeRow);
            body.add(dllRow);

            body.add(Box.createVerticalGlue());
            add(body, BorderLayout.CENTER);

            // ── Bottom buttons ──────────────────────────────────────────
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            buttons.setBackground(Theme.BG);

            backBtn = Theme.createButton("Back");
            backBtn.addActionListener(e -> goToStep(1));

            installBtn = Theme.createAccentButton("Install", Theme.SUCCESS);
            installBtn.addActionListener(e -> runInstall());

            buttons.add(backBtn);
            buttons.add(installBtn);
            add(buttons, BorderLayout.SOUTH);
        }

        void onShow() {
            // Reset rows
            copyRow.setState(StatusRow.State.PENDING, "Ready");
            launcherCloseRow.setState(StatusRow.State.PENDING, "Ready");
            writeRow.setState(StatusRow.State.PENDING, "Ready");
            dllRow.setState(StatusRow.State.PENDING, "Ready");

            // Update launcher info
            String launcher = window.getDetectedLauncher();
            if (launcher != null) {
                launcherInfo.setText("Detected: " + launcher + "  \u2022  " + window.getInstanceName());
                // Pre-select in dropdown
                for (int i = 0; i < launcherOverride.getItemCount(); i++) {
                    if (launcherOverride.getItemAt(i).toLowerCase().contains(launcher.toLowerCase().split("/")[0].trim())) {
                        launcherOverride.setSelectedIndex(i);
                        break;
                    }
                }
            }

            // Check for existing prelaunch command conflict
            checkForMergeConflict();
        }

        private void checkForMergeConflict() {
            File config = window.getInstanceConfig();
            if (config == null) return;

            existingPrelaunch = null;
            hasConflict = false;

            try {
                if (config.getName().endsWith(".cfg")) {
                    // Read instance.cfg
                    BufferedReader reader = new BufferedReader(new FileReader(config));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("PreLaunchCommand=")) {
                            existingPrelaunch = line.substring("PreLaunchCommand=".length()).trim();
                            break;
                        }
                    }
                    reader.close();
                } else if (config.getName().endsWith(".json")) {
                    BufferedReader reader = new BufferedReader(new FileReader(config));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String trimmed = line.trim();
                        if (trimmed.startsWith("\"preLaunchCommand\"")) {
                            existingPrelaunch = Main.extractJsonStringValue(line);
                            break;
                        }
                    }
                    reader.close();
                }
            } catch (Exception ignored) {}

            // Check if the existing command is ours or external
            if (existingPrelaunch != null && !existingPrelaunch.isEmpty()) {
                java.util.List<String> parts = Main.splitPreLaunchCommands(existingPrelaunch);
                boolean hasExternal = false;
                for (String part : parts) {
                    if (!Main.isOurPreLaunchSegment(part)) {
                        hasExternal = true;
                        break;
                    }
                }
                if (hasExternal) {
                    hasConflict = true;
                    mergeExisting.setText(truncate(existingPrelaunch, 80));
                    mergePanel.setVisible(true);
                } else {
                    mergePanel.setVisible(false);
                }
            } else {
                mergePanel.setVisible(false);
            }
        }

        private String truncate(String s, int max) {
            if (s.length() <= max) return s;
            return s.substring(0, max) + "...";
        }

        private void runInstall() {
            installBtn.setEnabled(false);
            backBtn.setEnabled(false);

            new SwingWorker<String, String>() {
                @Override
                protected String doInBackground() {
                    try {
                        // Step 1: Copy JAR
                        publish("copy");
                        File jarDir = window.getJarDir();
                        File stableJar = window.getStableJar();
                        String stableJarName = stableJar.getName();
                        String jarPath = Main.getJarPath();
                        File jarFile = new File(jarPath);

                        if (!stableJar.getAbsolutePath().equalsIgnoreCase(jarFile.getAbsolutePath())) {
                            Files.copy(jarFile.toPath(), stableJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }

                        // Determine subfolder prefix
                        String subfolderPrefix = "";
                        File instanceDir = window.getInstanceDir();
                        String dirName = jarDir.getName().toLowerCase();
                        if (dirName.equals("minecraft") || dirName.equals(".minecraft")) {
                            subfolderPrefix = jarDir.getName() + "/";
                        }

                        String jarRelativePath = subfolderPrefix + stableJarName;
                        File instanceConfig = window.getInstanceConfig();
                        boolean isJson = instanceConfig.getName().toLowerCase().endsWith(".json");

                        // Build commands
                        String prelaunchCommand;
                        if (isJson) {
                            prelaunchCommand = "\"$INST_JAVA\" -jar \"$INST_DIR/" + jarRelativePath + "\" --prelaunch";
                        } else {
                            prelaunchCommand = "\\\"$INST_JAVA\\\" -jar \\\"$INST_DIR/" + jarRelativePath + "\\\"";
                        }

                        // Step 2: Resolve merge if conflict
                        String resolvedCommand = prelaunchCommand;
                        if (hasConflict && existingPrelaunch != null) {
                            int mergeChoice = mergeKeep.isSelected() ? 0 : 1;
                            Main.MergeResult mr = Main.resolvePrismPreLaunchCommandNoUi(
                                existingPrelaunch, prelaunchCommand, mergeChoice);
                            if (!mr.proceed) {
                                return "Installation cancelled.";
                            }
                            resolvedCommand = mr.mergedCommand;
                        }

                        // Step 3: Close launchers
                        publish("close");
                        Main.InstallResult closeResult = Main.closeLaunchersBeforePreLaunchUpdate();
                        if (!closeResult.success) {
                            return closeResult.error;
                        }

                        // Step 4: Write prelaunch command
                        publish("write");
                        Main.InstallResult installResult;
                        if (isJson) {
                            installResult = Main.installPreLaunchCommandJsonResolved(instanceConfig, resolvedCommand);
                        } else {
                            installResult = Main.installPreLaunchCommandResolved(instanceConfig, resolvedCommand);
                        }
                        if (!installResult.success) {
                            return installResult.error;
                        }

                        // Step 5: Extract DLLs + create prelaunch.txt
                        publish("dll");
                        Main.extractEmbeddedDlls();
                        Main.ensurePrelaunchTxtExists(instanceDir);

                        return null; // success
                    } catch (Exception e) {
                        return e.getMessage() != null ? e.getMessage() : e.toString();
                    }
                }

                @Override
                protected void process(java.util.List<String> chunks) {
                    for (String step : chunks) {
                        switch (step) {
                            case "copy":
                                copyRow.setState(StatusRow.State.CHECKING, "Copying...");
                                break;
                            case "close":
                                copyRow.setState(StatusRow.State.SUCCESS, "Done");
                                launcherCloseRow.setState(StatusRow.State.CHECKING, "Closing launchers...");
                                break;
                            case "write":
                                launcherCloseRow.setState(StatusRow.State.SUCCESS, "Done");
                                writeRow.setState(StatusRow.State.CHECKING, "Writing...");
                                break;
                            case "dll":
                                writeRow.setState(StatusRow.State.SUCCESS, "Done");
                                dllRow.setState(StatusRow.State.CHECKING, "Extracting...");
                                break;
                        }
                    }
                }

                @Override
                protected void done() {
                    try {
                        String error = get();
                        if (error == null) {
                            dllRow.setState(StatusRow.State.SUCCESS, "Done");
                            donePanel.showSuccess();
                            goToStep(3);
                        } else {
                            // Mark the failed step
                            donePanel.showError(error);
                            stepIndicator.setStepError(3);
                            goToStep(3);
                        }
                    } catch (Exception e) {
                        donePanel.showError(e.getMessage());
                        goToStep(3);
                    }
                    installBtn.setEnabled(true);
                    backBtn.setEnabled(true);
                }
            }.execute();
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Step 3: Done
    // ═════════════════════════════════════════════════════════════════════

    private class DoneStepPanel extends JPanel {
        private final JLabel iconLabel;
        private final JLabel messageLabel;
        private final JLabel detailLabel;
        private final JPanel actionButtons;

        DoneStepPanel() {
            setLayout(new BorderLayout());
            setBackground(Theme.BG);
            setBorder(BorderFactory.createEmptyBorder(30, 24, 20, 24));

            JPanel center = Theme.createVBox();
            center.setAlignmentX(Component.CENTER_ALIGNMENT);

            // Icon + message centered
            JPanel iconPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            iconPanel.setBackground(Theme.BG);
            iconLabel = new JLabel();
            iconPanel.add(iconLabel);
            center.add(iconPanel);
            center.add(Box.createVerticalStrut(12));

            messageLabel = Theme.createLabel("", Theme.HEADING_FONT, Theme.FG);
            messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            center.add(messageLabel);
            center.add(Box.createVerticalStrut(8));

            detailLabel = Theme.createLabel("", Theme.BODY_FONT, Theme.SUBTLE);
            detailLabel.setHorizontalAlignment(SwingConstants.CENTER);
            detailLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            center.add(detailLabel);

            center.add(Box.createVerticalGlue());
            add(center, BorderLayout.CENTER);

            actionButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
            actionButtons.setBackground(Theme.BG);
            add(actionButtons, BorderLayout.SOUTH);
        }

        void showSuccess() {
            iconLabel.setIcon(Theme.largeSuccessIcon());
            messageLabel.setText(window.getProjectName() + " Installed Successfully!");
            messageLabel.setForeground(Theme.SUCCESS);
            detailLabel.setText("Instance: " + window.getInstanceName()
                + "  \u2022  You can now launch Minecraft from your launcher.");

            actionButtons.removeAll();

            // Discord CTA
            String discordUrl = Main.getDiscordUrl();
            if (discordUrl != null && !discordUrl.isEmpty()) {
                JButton discordBtn = Theme.createAccentButton("Join Discord", Theme.INFO);
                discordBtn.addActionListener(e -> {
                    try {
                        Desktop.getDesktop().browse(new URI(discordUrl));
                    } catch (Exception ignored) {}
                });
                actionButtons.add(discordBtn);
            }

            JButton closeBtn = Theme.createButton("Close");
            closeBtn.addActionListener(e -> {
                Main.restartSavedLaunchersAfterConfirmation();
                window.dispose();
                System.exit(0);
            });
            actionButtons.add(closeBtn);

            actionButtons.revalidate();
            actionButtons.repaint();
        }

        void showError(String error) {
            iconLabel.setIcon(Theme.largeErrorIcon());
            messageLabel.setText("Installation Failed");
            messageLabel.setForeground(Theme.ERROR);
            detailLabel.setText(error != null ? error : "An unknown error occurred.");

            actionButtons.removeAll();

            JButton retryBtn = Theme.createAccentButton("Retry", Theme.WARNING);
            retryBtn.addActionListener(e -> {
                goToStep(2);
                installPanel.onShow();
            });
            actionButtons.add(retryBtn);

            JButton closeBtn = Theme.createButton("Close");
            closeBtn.addActionListener(e -> {
                Main.restartSavedLaunchersAfterConfirmation();
                window.dispose();
                System.exit(0);
            });
            actionButtons.add(closeBtn);

            actionButtons.revalidate();
            actionButtons.repaint();
        }
    }
}
