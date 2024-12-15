package org.map_db;

import org.map_db.ui.MVPanel;
import org.map_db.ui.view.markers.MarkersContainer;

public class Main {
    public static void main(String[] args) {
        DB.EdgesPacket edgesPacket = DB.loadAllNodes();
        MVPanel panel = new MVPanel(edgesPacket.getHead(), edgesPacket.getMarkersContainer());
        panel.render();
    }
}