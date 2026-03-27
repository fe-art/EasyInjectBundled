package com.easyinject.ui;

import com.easyinject.Main;

import java.awt.*;
import javax.swing.*;

/**
 * Uninstall screen: confirmation → progress → done.
 */
public class UninstallScreen extends InstallerWindow.ScreenPanel {

    private static final String VIEW_CONFIRM  = "confirm";
    private static final String VIEW_PROGRESS = "progress";
    private static final String VIEW_DONE     = "done";

    private final CardLayout viewLayout;
    private final JPanel viewPanel;

    private final JLabel doneIcon;
    private final JLabel doneMessage;
    private final JLabel doneDetail;

    public UninstallScreen(InstallerWindow window) {
        super(window);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        viewLayout = new CardLayout();
        viewPanel = new JPanel(viewLayout);
        viewPanel.setBackground(Theme.BG);

        // ── Confirm view ────────────────────────────────────────────────
        JPanel confirmView = Theme.createVBox();
        confirmView.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        JLabel title = Theme.createLabel("Uninstall " + window.getProjectName(), Theme.HEADING_FONT, Theme.FG);
        confirmView.add(title);
        confirmView.add(Box.createVerticalStrut(12));

        JLabel info = new JLabel(
            "<html><body style='width: 440px; font-family: Segoe UI, sans-serif; color: #c7ced6;'>" +
            "<p>This will remove:</p>" +
            "<ul style='margin:4px 0; padding-left:16px;'>" +
            "<li>The pre-launch command from the instance config</li>" +
            "</ul>" +
            "<p style='margin-top:8px; color: #9e9e9e;'>The DLL folder and Windows Defender exclusions " +
            "will be preserved (they may be used by other instances).</p>" +
            "</body></html>"
        );
        info.setAlignmentX(Component.LEFT_ALIGNMENT);
        confirmView.add(info);

        confirmView.add(Box.createVerticalGlue());

        JPanel confirmButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        confirmButtons.setBackground(Theme.BG);

        JButton cancelBtn = Theme.createButton("Cancel");
        cancelBtn.addActionListener(e -> { window.setHeaderVisible(false); window.showScreen(InstallerWindow.SCREEN_HOME); });

        JButton uninstallBtn = Theme.createAccentButton("Uninstall", Theme.ERROR);
        uninstallBtn.addActionListener(e -> runUninstall());

        confirmButtons.add(cancelBtn);
        confirmButtons.add(uninstallBtn);
        confirmView.add(confirmButtons);

        viewPanel.add(confirmView, VIEW_CONFIRM);

        // ── Progress view ───────────────────────────────────────────────
        JPanel progressView = Theme.createVBox();
        progressView.setBorder(BorderFactory.createEmptyBorder(40, 0, 0, 0));

        JPanel progressCenter = new JPanel(new FlowLayout(FlowLayout.CENTER));
        progressCenter.setBackground(Theme.BG);
        JLabel progressLabel = Theme.createLabel("Removing pre-launch command...", Theme.BODY_FONT, Theme.SUBTLE);
        progressCenter.add(progressLabel);
        progressView.add(progressCenter);
        progressView.add(Box.createVerticalGlue());

        viewPanel.add(progressView, VIEW_PROGRESS);

        // ── Done view ───────────────────────────────────────────────────
        JPanel doneView = Theme.createVBox();
        doneView.setBorder(BorderFactory.createEmptyBorder(30, 0, 0, 0));

        JPanel iconPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        iconPanel.setBackground(Theme.BG);
        doneIcon = new JLabel();
        iconPanel.add(doneIcon);
        doneView.add(iconPanel);
        doneView.add(Box.createVerticalStrut(12));

        doneMessage = Theme.createLabel("", Theme.HEADING_FONT, Theme.FG);
        doneMessage.setHorizontalAlignment(SwingConstants.CENTER);
        doneMessage.setAlignmentX(Component.CENTER_ALIGNMENT);
        doneView.add(doneMessage);
        doneView.add(Box.createVerticalStrut(8));

        doneDetail = Theme.createLabel("", Theme.BODY_FONT, Theme.SUBTLE);
        doneDetail.setHorizontalAlignment(SwingConstants.CENTER);
        doneDetail.setAlignmentX(Component.CENTER_ALIGNMENT);
        doneView.add(doneDetail);

        doneView.add(Box.createVerticalGlue());

        JPanel doneButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        doneButtons.setBackground(Theme.BG);

        JButton closeBtn = Theme.createButton("Close");
        closeBtn.addActionListener(e -> {
            Main.restartSavedLaunchersAfterConfirmation();
            window.dispose();
            System.exit(0);
        });
        doneButtons.add(closeBtn);
        doneView.add(doneButtons);

        viewPanel.add(doneView, VIEW_DONE);

        add(viewPanel, BorderLayout.CENTER);
    }

    @Override
    public void onShow() {
        viewLayout.show(viewPanel, VIEW_CONFIRM);
    }

    private void runUninstall() {
        viewLayout.show(viewPanel, VIEW_PROGRESS);
        viewPanel.revalidate();
        viewPanel.repaint();

        new SwingWorker<Main.InstallResult, Void>() {
            @Override
            protected Main.InstallResult doInBackground() {
                java.io.File config = window.getInstanceConfig();
                if (config == null) {
                    return new Main.InstallResult(false, "No instance config found.");
                }

                // Close launchers first
                Main.InstallResult closeResult = Main.closeLaunchersBeforePreLaunchUpdate();
                if (!closeResult.success) {
                    return closeResult;
                }

                // Clear the prelaunch command
                if (config.getName().toLowerCase().endsWith(".json")) {
                    return Main.installPreLaunchCommandJsonResolved(config, "");
                } else {
                    return Main.installPreLaunchCommandResolved(config, "");
                }
            }

            @Override
            protected void done() {
                try {
                    Main.InstallResult result = get();
                    if (result.success) {
                        doneIcon.setIcon(Theme.largeSuccessIcon());
                        doneMessage.setText("Uninstall Complete");
                        doneMessage.setForeground(Theme.FG);
                        doneDetail.setText(window.getProjectName() + " has been removed from this instance.");
                    } else {
                        doneIcon.setIcon(Theme.largeErrorIcon());
                        doneMessage.setText("Uninstall Failed");
                        doneMessage.setForeground(Theme.ERROR);
                        doneDetail.setText(result.error != null ? result.error : "Unknown error");
                    }
                } catch (Exception e) {
                    doneIcon.setIcon(Theme.largeErrorIcon());
                    doneMessage.setText("Uninstall Failed");
                    doneMessage.setForeground(Theme.ERROR);
                    doneDetail.setText(e.getMessage());
                }

                viewLayout.show(viewPanel, VIEW_DONE);
                viewPanel.revalidate();
                viewPanel.repaint();
            }
        }.execute();
    }
}
