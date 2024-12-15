package org.map_db.ui.view.markers;

import java.awt.*;

public enum MarkerType {
    HEAD(Color.GREEN, "HeadCenter"),
    CLIENT(Color.BLUE, "Client"),
    SUPPLY(Color.RED, "Supply");

    final String name;
    final Color color;

    MarkerType(Color color, String name) {
        this.name = name;
        this.color = color;
    }

    public Color getColor() {
        return this.color;
    }

    public String getName() {
        return this.name;
    }
}
