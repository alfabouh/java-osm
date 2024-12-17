package org.map_db;

import org.jxmapviewer.viewer.GeoPosition;
import org.map_db.ui.handler.AddressHandler;
import org.map_db.ui.view.markers.Marker;
import org.map_db.ui.view.markers.MarkerType;
import org.map_db.ui.view.markers.MarkersContainer;
import org.neo4j.driver.*;
import org.neo4j.driver.internal.shaded.reactor.util.annotation.NonNull;
import org.neo4j.driver.internal.shaded.reactor.util.annotation.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public abstract class DB {
    public static final String ip = "localhost";
    public static final String port = "7687";
    public static final String name = "neo4j";
    public static final String pass = "12345678";

    public static Pair<Marker, Double> getClientsSupplyP(@NonNull Marker client) {
        String label1 = MarkerType.CLIENT.getName();
        String label2 = MarkerType.SUPPLY.getName();

        try (Driver driver = GraphDatabase.driver("bolt://" + ip + ":" + port, AuthTokens.basic(name, pass));
             Session session = driver.session()) {

            String query = String.format(
                    "MATCH (client:%s)-[r:CONNECTED_TO]->(supply:%s) " +
                            "WHERE client.latitude = $lat AND client.longitude = $lon " +
                            "RETURN supply.name AS name, supply.latitude AS latitude, supply.longitude AS longitude, supply.address AS address, r.distance AS distance", label1, label2);

            Map<String, Object> params = new HashMap<>();
            params.put("lat", client.getGeoPosition().getLatitude());
            params.put("lon", client.getGeoPosition().getLongitude());

            Result result = session.run(query, params);

            if (result.hasNext()) {
                Record record = result.next();

                MarkerType markerType = MarkerType.SUPPLY;
                GeoPosition position = new GeoPosition(record.get("latitude").asDouble(), record.get("longitude").asDouble());

                Marker supply = new Marker(record.get("address").asString(), markerType, position);

                double distance = record.get("distance").asDouble();

                return new Pair<>(supply, distance);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

        return null;
    }

    public static void deleteEdge(@NonNull Marker marker) {
        String label1 = marker.getMarkerType().getName();

        try (Driver driver = GraphDatabase.driver("bolt://" + ip + ":" + port, AuthTokens.basic(name, pass));
             Session session = driver.session()) {

            session.writeTransaction((TransactionWork<Void>) tx -> {
                String query = String.format("MATCH (n:%s) ", label1) +
                        "WHERE n.latitude = $latitude AND n.longitude = $longitude " +
                        "DETACH DELETE n";
                tx.run(query, Values.parameters("latitude", marker.getGeoPosition().getLatitude(), "longitude", marker.getGeoPosition().getLongitude()));

                return null;
            });

            System.out.println("Узел успешно удалён!");
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.out.println("Ошибка при удалении узла.");
        }
    }

    public static void createEdge(@NonNull Marker marker) {
        try (Driver driver = GraphDatabase.driver("bolt://" + ip + ":" + port, AuthTokens.basic(name, pass));
             Session session = driver.session()) {

            String address = marker.getAddress();
            if (address.length() > 80) {
                address = address.substring(0, 80);
            } else {
                address = marker.getMarkerType().getName().toUpperCase();
            }

            String finalAddress = address;
            session.writeTransaction((TransactionWork<Void>) tx -> {
                String query = String.format("CREATE (center:%s {name: $name, latitude: $latitude, longitude: $longitude, address: $address})", marker.getMarkerType().getName());
                tx.run(query, Values.parameters("name", marker.getMarkerType().getName().toUpperCase(), "latitude", marker.getGeoPosition().getLatitude(), "longitude", marker.getGeoPosition().getLongitude(), "address", finalAddress));
                return null;
            });
            System.out.println("Центр успешно создан!");
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.out.println("Ошибка при создании центра.");
        }
    }

    public static EdgesPacket loadAllNodes() {
        AtomicReference<AddressHandler.PointData> head = new AtomicReference<>();
        MarkersContainer markersContainer = new MarkersContainer();
        HashSet<Marker> isolatedMarkers = new HashSet<>();

        try (Driver driver = GraphDatabase.driver("bolt://" + ip + ":" + port, AuthTokens.basic(name, pass));
             Session session = driver.session()) {

            String query = "MATCH (n) " +
                    "OPTIONAL MATCH (n)-[r:CONNECTED_TO]->(m) " +
                    "RETURN n.latitude AS latitude, n.longitude AS longitude, n.name AS name, n.address AS address, " +
                    "labels(n) AS nodeLabels, " +
                    "m.latitude AS neighborLatitude, m.longitude AS neighborLongitude, m.name AS neighborName, " +
                    "m.address AS neighborAddress, labels(m) AS neighborLabels";

            session.readTransaction(tx -> {
                Result result = tx.run(query);
                while (result.hasNext()) {
                    Record record = result.next();

                    double latitude = record.get("latitude").asDouble();
                    double longitude = record.get("longitude").asDouble();
                    String nodeName = record.get("name").asString();
                    String address = record.get("address").asString();
                    AddressHandler.PointData pointData = new AddressHandler.PointData(new GeoPosition(latitude, longitude), nodeName);

                    MarkerType markerType = getMarkerType(record, false);

                    if (nodeName.equalsIgnoreCase(MarkerType.HEAD.getName())) {
                        head.set(pointData);
                    }

                    double neighborLatitude = record.containsKey("neighborLatitude") && !record.get("neighborLatitude").isNull()
                            ? record.get("neighborLatitude").asDouble()
                            : Double.NaN;

                    double neighborLongitude = record.containsKey("neighborLongitude") && !record.get("neighborLongitude").isNull()
                            ? record.get("neighborLongitude").asDouble()
                            : Double.NaN;

                    String neighborName = record.containsKey("neighborName") && !record.get("neighborName").isNull()
                            ? record.get("neighborName").asString()
                            : null;

                    String neighborAddress = record.containsKey("neighborAddress") && !record.get("neighborAddress").isNull()
                            ? record.get("neighborAddress").asString()
                            : null;

                    AddressHandler.PointData neighborData = (neighborName != null)
                            ? new AddressHandler.PointData(new GeoPosition(neighborLatitude, neighborLongitude), neighborName)
                            : null;

                    Marker neighborMarker = (neighborData != null)
                            ? new Marker(neighborAddress, getMarkerType(record, true), neighborData.getGeoPosition())
                            : null;

                    Marker marker = new Marker(address, markerType, pointData.getGeoPosition());

                    if (neighborName == null) {
                        isolatedMarkers.add(marker);
                    } else {
                        markersContainer.addRelation(marker, neighborMarker);
                    }
                }
                return null;
            });

            for (Marker isolatedMarker : isolatedMarkers) {
                markersContainer.addRelation(isolatedMarker, null);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

        return new EdgesPacket(head.get(), markersContainer);
    }

    private static MarkerType getMarkerType(Record record) {
        return getMarkerType(record, false);
    }

    private static MarkerType getMarkerType(Record record, boolean isNeighbor) {
        if (isNeighbor) {
            if (record.get("neighborLabels").asList().contains("HeadCenter")) {
                return MarkerType.HEAD;
            } else if (record.get("neighborLabels").asList().contains("Client")) {
                return MarkerType.CLIENT;
            } else {
                return MarkerType.SUPPLY;
            }
        } else {
            if (record.get("nodeLabels").asList().contains("HeadCenter")) {
                return MarkerType.HEAD;
            } else if (record.get("nodeLabels").asList().contains("Client")) {
                return MarkerType.CLIENT;
            } else {
                return MarkerType.SUPPLY;
            }
        }
    }

    public static int getClientCountAttachedToMarker(Marker marker) {
        String label1 = MarkerType.SUPPLY.getName();
        String label2 = MarkerType.CLIENT.getName();

        try (Driver driver = GraphDatabase.driver("bolt://" + ip + ":" + port, AuthTokens.basic(name, pass))) {
            try (Session session = driver.session()) {
                String query = String.format("MATCH (a:%s)-[r:CONNECTED_TO]->(b:%s) ", label1, label2) +
                        "WHERE a.latitude = $lat AND a.longitude = $lon " +
                        "RETURN COUNT(b) AS clientCount";

                Map<String, Object> params = new HashMap<>();
                params.put("lat", marker.getGeoPosition().getLatitude());
                params.put("lon", marker.getGeoPosition().getLongitude());

                Result result = session.run(query, params);
                if (result.hasNext()) {
                    return result.next().get("clientCount").asInt();
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return 0;
    }

    public static void deleteRelation(@NonNull Marker marker1, @NonNull Marker marker2) {
        String label1 = marker1.getMarkerType().getName();
        String label2 = marker2.getMarkerType().getName();

        try (Driver driver = GraphDatabase.driver("bolt://" + ip + ":" + port, AuthTokens.basic(name, pass))) {
            try (Session session = driver.session()) {
                String query = String.format("MATCH (a:%s)-[r:CONNECTED_TO]->(b:%s) ", label1, label2) +
                        "WHERE a.latitude = $lat1 AND a.longitude = $lon1 " +
                        "AND b.latitude = $lat2 AND b.longitude = $lon2 " +
                        "WITH r " +
                        "WHERE r IS NOT NULL " +
                        "DELETE r";

                Map<String, Object> params = new HashMap<>();
                params.put("lat1", marker1.getGeoPosition().getLatitude());
                params.put("lon1", marker1.getGeoPosition().getLongitude());
                params.put("lat2", marker2.getGeoPosition().getLatitude());
                params.put("lon2", marker2.getGeoPosition().getLongitude());

                session.writeTransaction(tx -> tx.run(query, params));
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }

    public static void createRelation(@NonNull Marker marker1, @NonNull Marker marker2) {
        String label1 = marker1.getMarkerType().getName();
        String label2 = marker2.getMarkerType().getName();

        try (Driver driver = GraphDatabase.driver("bolt://" + ip + ":" + port, AuthTokens.basic(name, pass))) {
            double distance = calculateDistance(marker1.getGeoPosition(), marker2.getGeoPosition());

            try (Session session = driver.session()) {
                String query = String.format("MATCH (a:%s), (b:%s) ", label1, label2) +
                        "WHERE a.latitude = $lat1 AND a.longitude = $lon1 " +
                        "AND b.latitude = $lat2 AND b.longitude = $lon2 " +
                        "MERGE (a)-[r:CONNECTED_TO {distance: $distance}]->(b)";

                Map<String, Object> params = new HashMap<>();
                params.put("lat1", marker1.getGeoPosition().getLatitude());
                params.put("lon1", marker1.getGeoPosition().getLongitude());
                params.put("lat2", marker2.getGeoPosition().getLatitude());
                params.put("lon2", marker2.getGeoPosition().getLongitude());
                params.put("distance", distance);

                session.writeTransaction(tx -> tx.run(query, params));
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public static double calculateDistance(GeoPosition point1, GeoPosition point2) {
        double lat1 = point1.getLatitude();
        double lon1 = point1.getLongitude();
        double lat2 = point2.getLatitude();
        double lon2 = point2.getLongitude();

        lat1 = Math.toRadians(lat1);
        lon1 = Math.toRadians(lon1);
        lat2 = Math.toRadians(lat2);
        lon2 = Math.toRadians(lon2);

        double dlat = lat2 - lat1;
        double dlon = lon2 - lon1;

        double a = Math.sin(dlat / 2) * Math.sin(dlat / 2) + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dlon / 2) * Math.sin(dlon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double radius = 6371.0;

        return radius * c;
    }

    public static class EdgesPacket {
        private final AddressHandler.PointData head;
        private final MarkersContainer markersContainer;

        public EdgesPacket(@Nullable AddressHandler.PointData head, @Nullable MarkersContainer markersContainer) {
            this.head = head;
            this.markersContainer = markersContainer;
        }

        public MarkersContainer getMarkersContainer() {
            return this.markersContainer;
        }

        public AddressHandler.PointData getHead() {
            return this.head;
        }
    }
}