package com.easyinject.ui;

import java.awt.*;
import javax.swing.*;

/**
 * Main installer window. Single persistent JFrame with CardLayout
 * that hosts all installer screens.
 */
public class InstallerWindow extends JFrame {

    public static final String SCREEN_HOME      = "home";
    public static final String SCREEN_INSTALL   = "install";
    public static final String SCREEN_REPAIR    = "repair";
    public static final String SCREEN_UNINSTALL = "uninstall";

    private final CardLayout cardLayout;
    private final JPanel contentPanel;
    private final JLabel subtitleLabel;

    // Shared state set by HomeScreen after detection
    private java.io.File instanceDir;
    private java.io.File instanceConfig;   // instance.cfg or instance.json
    private String instanceName;
    private String detectedLauncher;       // "Prism Launcher", "MultiMC", "ATLauncher", or null
    private java.io.File stableJar;
    private java.io.File jarDir;

    private final String projectName;
    private final String version;

    public InstallerWindow(String projectName, String version) {
        super(projectName + " v" + version);
        this.projectName = projectName;
        this.version = version;

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                handleWindowClose();
            }
        });

        setMinimumSize(new Dimension(580, 420));
        setPreferredSize(new Dimension(580, 420));
        setResizable(false);

        // Root panel with full-bg paint to avoid ghosting
        JPanel root = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Theme.BG);
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        root.setOpaque(true);
        root.setBackground(Theme.BG);
        root.setDoubleBuffered(true);

        // ── Header ──────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.BG_DARKER);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.DIVIDER),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        JLabel titleLabel = new JLabel(projectName);
        titleLabel.setFont(Theme.TITLE_FONT);
        titleLabel.setForeground(Theme.FG);
        header.add(titleLabel, BorderLayout.WEST);

        subtitleLabel = new JLabel("v" + version);
        subtitleLabel.setFont(Theme.SMALL_FONT);
        subtitleLabel.setForeground(Theme.MUTED);
        subtitleLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        header.add(subtitleLabel, BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);

        // ── Content (CardLayout) ────────────────────────────────────────
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(Theme.BG);
        contentPanel.setOpaque(true);

        root.add(contentPanel, BorderLayout.CENTER);
        setContentPane(root);

        // ── Add screens ─────────────────────────────────────────────────
        HomeScreen homeScreen = new HomeScreen(this);
        contentPanel.add(homeScreen, SCREEN_HOME);

        InstallFlow installFlow = new InstallFlow(this);
        contentPanel.add(installFlow, SCREEN_INSTALL);

        RepairScreen repairScreen = new RepairScreen(this);
        contentPanel.add(repairScreen, SCREEN_REPAIR);

        UninstallScreen uninstallScreen = new UninstallScreen(this);
        contentPanel.add(uninstallScreen, SCREEN_UNINSTALL);

        // Start on home
        showScreen(SCREEN_HOME);

        pack();
        setLocationRelativeTo(null);
    }

    /** Switch to a named screen. */
    public void showScreen(String name) {
        cardLayout.show(contentPanel, name);

        // Notify screen that it's being shown
        for (Component c : contentPanel.getComponents()) {
            if (c instanceof ScreenPanel) {
                // Not visible yet when switching — check by card name
            }
        }

        // Refresh the newly-visible panel
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /** Get a screen by class type. */
    @SuppressWarnings("unchecked")
    public <T extends JPanel> T getScreen(Class<T> type) {
        for (Component c : contentPanel.getComponents()) {
            if (type.isInstance(c)) return (T) c;
        }
        return null;
    }

    public void setSubtitle(String text) {
        subtitleLabel.setText(text);
    }

    private void handleWindowClose() {
        // Simple exit — if we want a "are you sure?" during operations we can add it later
        com.easyinject.Main.restartSavedLaunchersAfterConfirmation();
        dispose();
        System.exit(0);
    }

    // ── Shared state accessors ──────────────────────────────────────────

    public String getProjectName()      { return projectName; }
    public String getVersion()          { return version; }

    public java.io.File getInstanceDir()    { return instanceDir; }
    public java.io.File getInstanceConfig() { return instanceConfig; }
    public String getInstanceName()         { return instanceName; }
    public String getDetectedLauncher()     { return detectedLauncher; }
    public java.io.File getStableJar()      { return stableJar; }
    public java.io.File getJarDir()         { return jarDir; }

    public void setInstanceDir(java.io.File f)    { this.instanceDir = f; }
    public void setInstanceConfig(java.io.File f) { this.instanceConfig = f; }
    public void setInstanceName(String s)         { this.instanceName = s; }
    public void setDetectedLauncher(String s)     { this.detectedLauncher = s; }
    public void setStableJar(java.io.File f)      { this.stableJar = f; }
    public void setJarDir(java.io.File f)         { this.jarDir = f; }

    /**
     * Abstract base for screens that want to be notified when shown.
     */
    public static abstract class ScreenPanel extends JPanel {
        protected final InstallerWindow window;

        protected ScreenPanel(InstallerWindow window) {
            this.window = window;
            setBackground(Theme.BG);
            setOpaque(true);
        }

        /** Called when this screen becomes visible. Override to refresh. */
        public void onShow() {}
    }
}
