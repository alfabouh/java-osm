package org.map_db.ui.handler;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;
import org.map_db.ui.MVPanel;
import org.neo4j.driver.internal.shaded.reactor.util.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Objects;

public class AddressHandler {
    private final MVPanel mvPanel;
    private JXMapViewer mapViewer;
    private PointData headCenter;
    private PointData currentPointData;

    public AddressHandler(@Nullable PointData head, MVPanel mvPanel) {
        this.currentPointData = null;
        this.mvPanel = mvPanel;
        this.headCenter = head;
    }

    public void setMapViewer(JXMapViewer mapViewer) {
        this.mapViewer = mapViewer;
    }

    public void processResult(List<PointData> geoPositionList) {
        if (geoPositionList.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Адрес не найден", "Warn", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (geoPositionList.size() > 1) {
            JPanel buttonPanel = this.createJPanel(geoPositionList);
            JOptionPane.showConfirmDialog(null, buttonPanel, "Найдено несколько точек", JOptionPane.DEFAULT_OPTION);
            return;
        }
        this.setPoint(geoPositionList.get(0));
    }

    private JPanel createJPanel(List<PointData> geoPositionList) {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(0, 1));

        for (int i = 0; i < geoPositionList.size(); i++) {
            PointData position = geoPositionList.get(i);
            String label = "Точка " + (i + 1) + ": " + position.getAddress();

            JButton button = new JButton(label);
            final int index = i;
            button.addActionListener(e -> {
                this.setPoint(geoPositionList.get(index));
                JOptionPane.getRootFrame().dispose();
            });
            buttonPanel.add(button);
        }
        return buttonPanel;
    }

    public void setPoint(PointData pointData) {
        if (pointData == this.getCurrentPointData()) {
            return;
        }
        this.getMapViewer().setAddressLocation(pointData.getGeoPosition());
        this.getMapViewer().setZoom(2);
        this.setCurrentPointData(pointData);
        this.getMvPanel().repaint(this.getMapViewer());
    }

    public PointData getCurrentPointData() {
        return this.currentPointData;
    }

    public void setCurrentPointData(PointData currentPointData) {
        this.currentPointData = currentPointData;
    }

    public PointData getHeadCenter() {
        return this.headCenter;
    }

    public AddressHandler setHeadCenter(PointData headCenter) {
        this.headCenter = headCenter;
        return this;
    }

    public MVPanel getMvPanel() {
        return this.mvPanel;
    }

    public JXMapViewer getMapViewer() {
        return this.mapViewer;
    }

    public static class PointData {
        private final GeoPosition geoPosition;
        private final String name;

        public PointData(GeoPosition geoPosition, String name) {
            this.geoPosition = geoPosition;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            PointData pointData = (PointData) o;
            return Objects.equals(name, pointData.name);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(name);
        }

        public String getAddress() {
            return this.name;
        }

        public GeoPosition getGeoPosition() {
            return this.geoPosition;
        }
    }
}
