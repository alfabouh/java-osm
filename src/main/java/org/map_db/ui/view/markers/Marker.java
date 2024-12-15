package org.map_db.ui.view.markers;

import org.jxmapviewer.viewer.GeoPosition;

import java.util.Objects;

public class Marker {
    private final String address;
    private final GeoPosition geoPosition;
    private final MarkerType markerType;

    public Marker(String address, MarkerType markerType, GeoPosition geoPosition) {
        this.geoPosition = geoPosition;
        this.markerType = markerType;
        this.address = address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Marker marker = (Marker) o;
        return Objects.equals(geoPosition, marker.geoPosition);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(geoPosition);
    }

    public String getAddress() {
        return this.address;
    }

    public MarkerType getMarkerType() {
        return this.markerType;
    }

    public GeoPosition getGeoPosition() {
        return this.geoPosition;
    }
}
