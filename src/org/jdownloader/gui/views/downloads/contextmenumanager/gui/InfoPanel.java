package org.jdownloader.gui.views.downloads.contextmenumanager.gui;

import java.awt.Rectangle;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSeparator;

import org.appwork.swing.MigPanel;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuContainer;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuItemData;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuItemProperty;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuLink;
import org.jdownloader.gui.views.downloads.contextmenumanager.SeparatorData;
import org.jdownloader.images.NewTheme;

public class InfoPanel extends MigPanel {

    private JLabel    label;
    private JLabel    type;
    private JCheckBox hideIfDisabled;
    private JCheckBox hideIfOpenFileIsUnsupported;
    private JCheckBox hideIfOutputNotExists;
    private JCheckBox linkContext;
    private JCheckBox packageContext;

    public InfoPanel() {
        super("ins 5,wrap 2", "[grow,fill][]", "[22!][]");

        label = SwingUtils.toBold(new JLabel());
        type = new JLabel();
        add(label);
        add(type);
        add(new JSeparator(), "spanx");
        add(SwingUtils.toBold(new JLabel(_GUI._.InfoPanel_InfoPanel_properties_())), "spanx");
        // MenuItemProperty.HIDE_IF_DISABLED;
        // MenuItemProperty.HIDE_IF_OPENFILE_IS_UNSUPPORTED;
        // MenuItemProperty.HIDE_IF_OUTPUT_NOT_EXISTING;
        hideIfDisabled = new JCheckBox(_GUI._.InfoPanel_InfoPanel_hideIfDisabled());
        hideIfOpenFileIsUnsupported = new JCheckBox(_GUI._.InfoPanel_InfoPanel_hideIfOpenFileIsUnsupported());
        hideIfOutputNotExists = new JCheckBox(_GUI._.InfoPanel_InfoPanel_hideIfFileNotExists());
        linkContext = new JCheckBox(_GUI._.InfoPanel_InfoPanel_linkContext());
        packageContext = new JCheckBox(_GUI._.InfoPanel_InfoPanel_packageContext());
        add(hideIfDisabled, "spanx");
        add(hideIfOpenFileIsUnsupported, "spanx");
        add(hideIfOutputNotExists, "spanx");
        add(linkContext, "spanx");
        add(packageContext, "spanx");
    }

    /**
     * @param lastPathComponent
     */
    public void updateInfo(final MenuItemData value) {
        if (value == null) {

            type.setText("");
            label.setText("");
            return;
        }
        MenuItemData mid = ((MenuItemData) value).lazyReal();
        Rectangle bounds = null;

        link(mid, hideIfDisabled, MenuItemProperty.HIDE_IF_DISABLED);
        link(mid, hideIfOpenFileIsUnsupported, MenuItemProperty.HIDE_IF_OPENFILE_IS_UNSUPPORTED);

        link(mid, hideIfOutputNotExists, MenuItemProperty.HIDE_IF_OUTPUT_NOT_EXISTING);

        link(mid, linkContext, MenuItemProperty.LINK_CONTEXT);

        link(mid, packageContext, MenuItemProperty.PACKAGE_CONTEXT);

        // renderer.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.RED));
        if (mid instanceof MenuContainer) {
            type.setText(_GUI._.InfoPanel_update_submenu());
            if (mid.getIconKey() != null) {
                label.setIcon(NewTheme.I().getIcon(mid.getIconKey(), 20));
            } else {
                label.setIcon(null);

            }

            label.setText(_GUI._.Renderer_getTreeCellRendererComponent_submenu(mid.getName()));

        } else if (mid instanceof SeparatorData) {
            type.setText(_GUI._.InfoPanel_update_seperator_line());
            label.setIcon(null);
            label.setText(_GUI._.Renderer_getTreeCellRendererComponent_seperator());

        } else {
            if (mid instanceof MenuLink) {
                type.setText(_GUI._.InfoPanel_update_link());
                label.setText(_GUI._.Renderer_getTreeCellRendererComponent_link(mid.getName()));
                if (mid.getIconKey() != null) {
                    label.setIcon(NewTheme.I().getIcon(mid.getIconKey(), 20));
                } else {
                    label.setIcon(null);
                }

            } else {
                type.setText(_GUI._.InfoPanel_update_action());
                AppAction action = mid.createAction(null);

                label.setText(action.getName());
                label.setIcon(action.getSmallIcon());

            }

        }

    }

    private void link(MenuItemData mid, JCheckBox hideIfDisabled, MenuItemProperty hideIfDisabled3) {

        hideIfDisabled.setSelected(mid.mergeProperties().contains(hideIfDisabled3));
        hideIfDisabled.setEnabled(mid.getActionData() == null || mid.getActionData().getProperties() == null || !mid.getActionData().getProperties().contains(hideIfDisabled3));
    }
}