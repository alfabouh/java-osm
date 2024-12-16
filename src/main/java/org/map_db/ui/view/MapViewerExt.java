package org.map_db.ui.view;

import org.jxmapviewer.JXMapViewer;
import org.map_db.ui.view.markers.Marker;
import org.map_db.ui.view.markers.MarkerType;
import org.map_db.ui.view.markers.MarkersContainer;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MapViewerExt extends JXMapViewer {
    private final MarkersContainer markersContainer;

    public MapViewerExt(MarkersContainer markersContainer) {
        this.markersContainer = markersContainer;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        Font boldFont = new Font("Arial", Font.BOLD, 12);
        g2d.setFont(boldFont);
        BasicStroke stroke = new BasicStroke(3);
        g2d.setStroke(stroke);

        for (Map.Entry<Marker, HashSet<Marker>> m : this.getMarkersContainer().getMarkers().entrySet()) {
            Point2D markerPixel = this.getTileFactory().geoToPixel(m.getKey().getGeoPosition(), this.getZoom());

            Point2D mapCenter = this.getCenter();
            double offsetX = mapCenter.getX() - markerPixel.getX();
            double offsetY = mapCenter.getY() - markerPixel.getY();

            int x = (int) (this.getWidth() / 2f - offsetX) - 10;
            int y = (int) (this.getHeight() / 2f - offsetY) - 10;

            g2d.setColor(Color.BLACK);
            g2d.drawString(m.getKey().getAddress().substring(0, 50), x, y + 40);

            g2d.setColor(m.getKey().getMarkerType().getColor());
            g2d.fillRect(x, y, 20, 20);

            for (Marker marker : m.getValue()) {
                if (marker == null || marker.getMarkerType() == MarkerType.CLIENT) {
                    continue;
                }
                Point2D markerPixel2 = this.getTileFactory().geoToPixel(marker.getGeoPosition(), this.getZoom());
                double offsetX2 = mapCenter.getX() - markerPixel2.getX();
                double offsetY2 = mapCenter.getY() - markerPixel2.getY();

                int x2 = (int) (this.getWidth() / 2f - offsetX2) - 10;
                int y2 = (int) (this.getHeight() / 2f - offsetY2) - 10;
                g2d.drawLine(x, y, x2, y2);
            }
        }
    }

    public MarkersContainer getMarkersContainer() {
        return this.markersContainer;
    }
}
