package org.map_db.ui.view.markers;

import org.map_db.DB;

import java.util.*;
import java.util.stream.Collectors;

public class MarkersContainer {
    private final Map<Marker, HashSet<Marker>> markersRelationShip;

    public MarkersContainer() {
        this.markersRelationShip = new HashMap<>();
    }

    public MarkersContainer(Map<Marker, HashSet<Marker>> markersRelationShip) {
        this.markersRelationShip = markersRelationShip;
    }

    public boolean isRelationExists(Marker marker1, Marker marker2) {
        return markersRelationShip.containsKey(marker1) && markersRelationShip.get(marker1).contains(marker2);
    }

    public void removeRelation(Marker marker1, Marker marker2) {
        if (this.isRelationExists(marker1, marker2)) {
            HashSet<Marker> relatedMarkers = markersRelationShip.get(marker1);
            relatedMarkers.remove(marker2);

            if (relatedMarkers.isEmpty()) {
                markersRelationShip.remove(marker1);
            }
        }
    }

    public void removeEdge(Marker marker) {
        this.markersRelationShip.remove(marker);
        for (HashSet<Marker> relatedMarkers : this.markersRelationShip.values()) {
            relatedMarkers.remove(marker);
        }
    }

    public Marker findNearestAvailablePickup(Marker clientMarker) {
        List<Marker> pickupMarkers = this.getMarkers().keySet().stream().filter(e -> e.getMarkerType() != MarkerType.CLIENT).sorted(Comparator.comparingDouble(pickup -> calculateDistance(clientMarker, pickup))).collect(Collectors.toList());
        for (Marker pickup : pickupMarkers) {
            if (!this.isPickupFull(pickup)) {
                return pickup;
            }
        }
        return null;
    }

    private double calculateDistance(Marker marker1, Marker marker2) {
        double lat1 = marker1.getGeoPosition().getLatitude();
        double lon1 = marker1.getGeoPosition().getLongitude();
        double lat2 = marker2.getGeoPosition().getLatitude();
        double lon2 = marker2.getGeoPosition().getLongitude();
        return Math.sqrt(Math.pow(lat2 - lat1, 2) + Math.pow(lon2 - lon1, 2));
    }

    private boolean isPickupFull(Marker pickup) {
        int clientCount = DB.getClientCountAttachedToMarker(pickup);
        return clientCount >= 3;
    }

    public void addRelation(Marker marker1, Marker marker2) {
        markersRelationShip.computeIfAbsent(marker1, k -> new HashSet<>()).add(marker2);
    }

    public Map<Marker, HashSet<Marker>> getMarkers() {
        return this.markersRelationShip;
    }
}
