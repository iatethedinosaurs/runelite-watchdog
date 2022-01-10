package com.adamk33n3r.runelite.watchdog;

import com.adamk33n3r.runelite.watchdog.alerts.*;
import com.adamk33n3r.runelite.watchdog.dropdownbutton.DropDownButtonFactory;
import com.adamk33n3r.runelite.watchdog.panels.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.plugins.timetracking.TimeTrackingPlugin;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.MultiplexingPluginPanel;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import org.apache.commons.text.WordUtils;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class WatchdogPanel extends PluginPanel {
    @Getter
    private MultiplexingPluginPanel muxer;
    @Inject
    private Client client;
    @Inject
    private WatchdogPlugin plugin;

    private final Map<Class<? extends Alert>, TriggerType> alertTriggerTypeMap = new HashMap<Class<? extends Alert>, TriggerType>() {
        {
            put(ChatAlert.class, TriggerType.CHAT);
            put(NotificationFiredAlert.class, TriggerType.NOTIFICATION_FIRED);
        }
    };

    private ChatMonitorFrame chatMonitorFrame;

    public static final ImageIcon ADD_ICON;
    private static final ImageIcon ADD_ICON_HOVER;
    static {
        BufferedImage addIcon = ImageUtil.loadImageResource(TimeTrackingPlugin.class, "add_icon.png");
        ADD_ICON = new ImageIcon(addIcon);
        ADD_ICON_HOVER = new ImageIcon(ImageUtil.alphaOffset(addIcon, 0.53f));
    }

    @Inject
    public void init() {
        log.info("panel init");
        this.muxer = new MultiplexingPluginPanel(this);
        this.rebuild();
    }

    public void rebuild() {
        this.removeAll();
//        this.setLayout(new DynamicGridLayout(0, 1, 3, 3));
        this.setLayout(new BorderLayout(0, 5));
        this.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel newAlertPanel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Watchdog");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        title.setHorizontalAlignment(JLabel.LEFT);
        title.setForeground(Color.WHITE);
        newAlertPanel.add(title);

        JPopupMenu popupMenu = new JPopupMenu();
        ActionListener actionListener = e -> {
            JMenuItem menuItem = (JMenuItem) e.getSource();
            TriggerType tType = (TriggerType) menuItem.getClientProperty(TriggerType.class);
            this.createAlert(tType);
        };
        for (TriggerType tType : TriggerType.values()) {
            JMenuItem c = new JMenuItem(WordUtils.capitalizeFully(tType.name().replaceAll("_", " ")));
            c.putClientProperty(TriggerType.class, tType);
            c.addActionListener(actionListener);
            popupMenu.add(c);
        }
//        JButton inspector = new JButton("Inspector");
//        this.chatMonitorFrame = new ChatMonitorFrame();
//        inspector.addActionListener(ev -> {
//            this.chatMonitorFrame.open();
//        });
//        newAlertPanel.add(inspector);
        JButton addDropDownButton = DropDownButtonFactory.createDropDownButton(ADD_ICON, popupMenu, true);
        addDropDownButton.setToolTipText("Create New Alert");
        newAlertPanel.add(addDropDownButton, BorderLayout.EAST);
        this.add(newAlertPanel, BorderLayout.NORTH);

//        JPanel alertPanel = new JPanel(new DynamicGridLayout(0, 1, 3, 3));
        JPanel alertPanel = new JPanel(new BorderLayout());
//        alertPanel.setBorder(new TitledBorder(new EtchedBorder(), "Alert Panel", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, Color.WHITE));
        JPanel alertWrapperWrapper = new JPanel(new DynamicGridLayout(0, 1, 3, 3));
//        alertWrapperWrapper.setBorder(new TitledBorder(new EtchedBorder(), "Alert Wrapper Wrapper", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, Color.WHITE));
        alertPanel.add(alertWrapperWrapper, BorderLayout.NORTH);
        for (Alert alert : this.plugin.getAlerts()) {
            log.info(alert.toString());
            final JButton alertButton = new JButton(alert.getName());
//            alertButton.setPreferredSize(new Dimension(10, 1));
            alertButton.addActionListener(ev -> {
                this.openAlert(alert);
            });
//            JPanel alertWrapper = new JPanel(new DynamicGridLayout(1, 2, 3, 3));
            JPanel alertWrapper = new JPanel(new BorderLayout());
            alertWrapper.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH, 30));
//            JPanel alertWrapper = new JPanel();
            alertWrapperWrapper.add(alertWrapper);
//            alertWrapper.setBorder(new TitledBorder(new EtchedBorder(), "Alert Wrapper", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, Color.WHITE));
            alertWrapper.add(alertButton, BorderLayout.CENTER);
            final JButton deleteButton = new JButton("x");
            deleteButton.setBorder(null);
            deleteButton.setPreferredSize(new Dimension(30, 0));
            deleteButton.addActionListener(ev -> {
                int result = JOptionPane.showConfirmDialog(alertWrapper, "Are you sure you want to delete the " + alert.getName() + " alert?", "Delete?", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
                if (result == JOptionPane.YES_OPTION) {
                    List<Alert> alerts = plugin.getAlerts();
                    alerts.remove(alert);
                    plugin.saveAlerts(alerts);
                    // Rebuild called automatically when alerts config changed
                }
            });
            alertWrapper.add(deleteButton, BorderLayout.LINE_END);
        }
        this.add(new JScrollPane(alertPanel), BorderLayout.CENTER);
        // Need this for rebuild for some reason
        this.revalidate();
    }

    private void createAlert(TriggerType triggerType) {
        switch (triggerType) {
            case CHAT:
                this.openAlert(new ChatAlert());
                break;
            case NOTIFICATION_FIRED:
                this.openAlert(new NotificationFiredAlert());
                break;
            case STAT_DRAIN:
                this.openAlert(new StatDrainAlert());
                break;
            case IDLE:
                this.openAlert(new IdleAlert());
                break;
            case RESOURCE:
                this.openAlert(new ResourceAlert());
                break;
        }
    }

    private void openAlert(Alert alert) {
        PluginPanel panel = this.createPluginPanel(alert);
        if (panel != null)
            this.muxer.pushState(panel);
        else
            log.error(String.format("Tried to open an alert of type %s that doesn't have a panel.", alert.getClass()));
    }

    private PluginPanel createPluginPanel(Alert alert) {
        if (alert instanceof ChatAlert) {
            ChatAlert chatAlert = (ChatAlert) alert;
            return AlertPanel.create(this.muxer, alert)
                .addTextField("Name", chatAlert.getName(), chatAlert::setName)
                .addTextArea("Message", chatAlert.getMessage(), chatAlert::setMessage)
                .build();
        } else if (alert instanceof IdleAlert) {
            IdleAlert idleAlert = (IdleAlert) alert;
            return AlertPanel.create(this.muxer, alert)
                .addTextField("Name", idleAlert.getName(), idleAlert::setName)
                .addSelect("Action", IdleAlert.IdleAction.class, idleAlert.getIdleAction(), idleAlert::setIdleAction)
                .build();
        } else if (alert instanceof NotificationFiredAlert) {
            NotificationFiredAlert notificationFiredAlert = (NotificationFiredAlert) alert;
            return AlertPanel.create(this.muxer, alert)
                .addTextField("Name", notificationFiredAlert.getName(), notificationFiredAlert::setName)
                .addTextArea("Message", notificationFiredAlert.getMessage(), notificationFiredAlert::setMessage)
                .build();
        } else if (alert instanceof ResourceAlert) {
            ResourceAlert resourceAlert = (ResourceAlert) alert;
            return AlertPanel.create(this.muxer, alert)
                .addTextField("Name", resourceAlert.getName(), resourceAlert::setName)
                .addSelect("Resource", ResourceAlert.ResourceType.class, resourceAlert.getResourceType(), resourceAlert::setResourceType)
                .build();
        } else if (alert instanceof StatDrainAlert) {
            StatDrainAlert statDrainAlert = (StatDrainAlert) alert;
            return AlertPanel.create(this.muxer, alert)
                .addTextField("Name", statDrainAlert.getName(), statDrainAlert::setName)
                .addSelect("Skill", Skill.class, Skill.ATTACK, statDrainAlert::setSkill)
                .addSpinner("Drain Amount", statDrainAlert.getDrainAmount(), statDrainAlert::setDrainAmount)
                .build();
        }

        return null;
    }
}