package org.map_db.ui;

import org.jxmapviewer.viewer.GeoPosition;
import org.map_db.ui.handler.AddressHandler;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public abstract class Utils {
    public static List<AddressHandler.PointData> geocodeAddress(String address) throws IOException {
        List<AddressHandler.PointData> geoPositions = new ArrayList<>();
        String baseUrl = "https://nominatim.openstreetmap.org/search?";
        String query = "q=" + URLEncoder.encode(address, String.valueOf(StandardCharsets.UTF_8)) + "&format=json&limit=5";
        String requestUrl = baseUrl + query;

        try (Scanner scanner = new Scanner(new java.net.URL(requestUrl).openStream(), String.valueOf(StandardCharsets.UTF_8))) {
            String response = scanner.useDelimiter("\\A").next();

            String[] results = response.split("\\},\\{");
            for (String result : results) {
                if (result.contains("\"lat\":\"") && result.contains("\"lon\":\"")) {
                    String lat = result.split("\"lat\":\"")[1].split("\"")[0];
                    String lon = result.split("\"lon\":\"")[1].split("\"")[0];

                    String name = extractNameFromResult(result);

                    geoPositions.add(new AddressHandler.PointData(new GeoPosition(Double.parseDouble(lat), Double.parseDouble(lon)), name));
                }
            }
        }

        return geoPositions;
    }

    private static String extractNameFromResult(String result) {
        String name = "Неизвестно";
        if (result.contains("\"display_name\":\"")) {
            name = result.split("\"display_name\":\"")[1].split("\"")[0];
        }
        return name;
    }
}
