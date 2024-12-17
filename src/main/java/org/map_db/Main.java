package org.map_db;

import org.map_db.ui.MVPanel;

public class Main {
    public static void main(String[] args) {
        DB.EdgesPacket edgesPacket = DB.loadAllNodes();
        MVPanel panel = new MVPanel(edgesPacket.getHead(), edgesPacket.getMarkersContainer());
        panel.render();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (DB.DRIVER != null) {
            DB.DRIVER.close();
        }
    }
}