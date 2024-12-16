package org.map_db.ui;

import javafx.util.Pair;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.viewer.AbstractTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactory;
import org.jxmapviewer.viewer.TileFactoryInfo;
import org.map_db.DB;
import org.map_db.ui.handler.AddressHandler;
import org.map_db.ui.view.MapViewerExt;
import org.map_db.ui.view.markers.Marker;
import org.map_db.ui.view.markers.MarkerType;
import org.map_db.ui.view.markers.MarkersContainer;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class MVPanel {
    private final MarkersContainer markersContainer;
    private final AddressHandler addressHandler;
    private final JFrame frame;
    private JPanel inputPanel;

    public MVPanel(AddressHandler.PointData pointData, MarkersContainer markersContainer) {
        this.frame = new JFrame("map_viewer");
        this.addressHandler = new AddressHandler(pointData, this);
        this.markersContainer = markersContainer;
    }

    public void repaint(final JXMapViewer mapViewer) {
        this.inputPanel.removeAll();
        this.addContent(mapViewer, this.inputPanel);
        this.inputPanel.revalidate();
        this.inputPanel.repaint();
        mapViewer.revalidate();
        mapViewer.repaint();
    }

    public void render() {
        final JXMapViewer mapViewer = new MapViewerExt(this.getMarkersContainer());

        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frame.setSize(800, 600);

        JPanel mainPanel = new JPanel();
        this.inputPanel = new JPanel();

        JPanel mapPanel = new JPanel();
        this.getAddressHandler().setMapViewer(mapViewer);

        this.init(mapViewer, mainPanel, mapPanel, this.inputPanel);

        this.OSMMapRender(mapViewer, mapPanel);
        frame.add(mainPanel);
        this.frame.setVisible(true);
    }

    private void init(final JXMapViewer mapViewer, JPanel mainPanel, JPanel mapPanel, JPanel inputPanel) {
        mainPanel.setLayout(new BorderLayout());
        inputPanel.setLayout(new FlowLayout());
        mapPanel.setLayout(new BorderLayout());
        this.addContent(mapViewer, inputPanel);
        JScrollPane scrollPane = new JScrollPane(inputPanel);
        mainPanel.add(scrollPane, BorderLayout.WEST);
        mainPanel.add(mapPanel, BorderLayout.CENTER);
    }

    private void createRelation(final JXMapViewer mapViewer, Marker m1, Marker m2) {
        this.getMarkersContainer().addRelation(m1, m2);
        DB.createRelation(m1, m2);
        this.repaint(mapViewer);
    }

    private Marker createEdge(final JXMapViewer mapViewer, AddressHandler.PointData pointData, String address, MarkerType markerType) {
        Marker marker = new Marker(address, markerType, pointData.getGeoPosition());
        if (this.getMarkersContainer().getMarkers().containsKey(marker)) {
            JOptionPane.showMessageDialog(null, "Точка уже отмечена на карте", "Warn", JOptionPane.WARNING_MESSAGE);
            return marker;
        }
        if (markerType == MarkerType.HEAD) {
            this.getAddressHandler().setHeadCenter(pointData);
        }
        DB.createEdge(marker);
        this.getMarkersContainer().getMarkers().put(marker, new HashSet<>());
        this.repaint(mapViewer);
        return marker;
    }

    private void addContent(final JXMapViewer mapViewer, JPanel inputPanel) {
        final KeyAdapter keyAdapter = new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isDigit(c) && c != KeyEvent.VK_BACK_SPACE) {
                    e.consume();
                }
            }
        };

        final AddressHandler.PointData data = this.getAddressHandler().getCurrentPointData();
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new FlowLayout(FlowLayout.LEFT));

        JTextField addressField = new JTextField(20);
        JButton searchButton = new JButton("Найти адрес");
        JButton reload = new JButton("Перезагрузка");

        searchButton.addActionListener(e -> {
            String txt = addressField.getText();
            try {
                this.getAddressHandler().processResult(Utils.geocodeAddress(txt));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        reload.addActionListener(e -> {
            this.repaint(mapViewer);
        });

        mainContent.add(addressField);
        mainContent.add(searchButton);
        mainContent.add(reload);

        JPanel addressInfoPanel = new JPanel();
        addressInfoPanel.setLayout(new BoxLayout(addressInfoPanel, BoxLayout.Y_AXIS));

        JButton addSupplyButton = new JButton("Добавить пункт выдачи");
        JButton addClientButton = new JButton("Добавить получателя");
        JButton addHeadButton = new JButton("Создать центр");

        addHeadButton.addActionListener(e -> { this.createEdge(mapViewer, data, data.getAddress(), MarkerType.HEAD); });
        addSupplyButton.addActionListener(e -> { this.createEdge(mapViewer, data, data.getAddress(), MarkerType.SUPPLY); });
        addClientButton.addActionListener(e -> {
            Marker marker = this.createEdge(mapViewer, data, data.getAddress(), MarkerType.CLIENT);
            Marker markerSp = this.getMarkersContainer().findNearestAvailablePickup(marker);
            this.createRelation(mapViewer, marker, markerSp);
            this.createRelation(mapViewer, markerSp, marker);
        });

        JLabel addressLabel = new JLabel("Адрес: <пусто>");
        addressLabel.setPreferredSize(new Dimension(300, 30));

        if (data != null) {
            addClientButton.setEnabled(true);
            addSupplyButton.setEnabled(true);
            addHeadButton.setEnabled(true);
            addressLabel.setText(data.getAddress());
        } else {
            addClientButton.setEnabled(false);
            addSupplyButton.setEnabled(false);
            addHeadButton.setEnabled(false);
        }

        addressInfoPanel.add(addressLabel);

        if (this.getAddressHandler().getHeadCenter() == null) {
            addressInfoPanel.add(addHeadButton);
        } else {
            addressInfoPanel.add(addSupplyButton);
            addressInfoPanel.add(addClientButton);
        }

        Border border = BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1);
        addressInfoPanel.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        JPanel supplierInfoPanel = new JPanel();
        supplierInfoPanel.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        supplierInfoPanel.setLayout(new BoxLayout(supplierInfoPanel, BoxLayout.Y_AXIS));

        JButton addRelation = new JButton("Связать");
        JButton removeRelation = new JButton("Удалить связь");

        JTextField numberField1 = new JTextField(3);
        numberField1.setPreferredSize(new Dimension(50, 30));
        JTextField numberField2 = new JTextField(3);
        numberField2.setPreferredSize(new Dimension(50, 30));

        numberField1.addKeyListener(keyAdapter);
        numberField2.addKeyListener(keyAdapter);

        JPanel markersPanel = new JPanel();
        markersPanel.add(new JLabel("Поставка:"));

        markersPanel.setLayout(new BoxLayout(markersPanel, BoxLayout.Y_AXIS));
        Set<Marker> markers = this.getMarkersContainer().getMarkers().keySet();

        final HashMap<Integer, Marker> idMap = new HashMap<>();
        int i = 0;
        for (Marker marker : markers) {
            if (marker.getMarkerType() == MarkerType.CLIENT) {
                continue;
            }
            i += 1;
            JLabel markerLabel = new JLabel("id: " + i + ", " + marker.getMarkerType().getName() + ": " + marker.getAddress());
            markersPanel.add(markerLabel);
            idMap.put(i, marker);
        }

        JScrollPane markersScrollPane = new JScrollPane(markersPanel);
        markersScrollPane.setPreferredSize(new Dimension(300, 200));
        supplierInfoPanel.add(markersScrollPane);

        supplierInfoPanel.add(new JLabel("ID 1:"));
        supplierInfoPanel.add(numberField1);
        supplierInfoPanel.add(new JLabel("ID 2:"));
        supplierInfoPanel.add(numberField2);
        supplierInfoPanel.add(addRelation);
        addRelation.setEnabled(idMap.size() >= 2);
        supplierInfoPanel.add(removeRelation);
        removeRelation.setEnabled(idMap.size() >= 2);

        removeRelation.addActionListener(e -> {
            int id1 = Integer.parseInt(numberField1.getText());
            int id2 = Integer.parseInt(numberField2.getText());

            if (!idMap.containsKey(id1) || !idMap.containsKey(id2)) {
                JOptionPane.showMessageDialog(null, "Такого айди не существует", "Warn", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Marker m1 = idMap.get(id1);
            Marker m2 = idMap.get(id2);
            if (!this.getMarkersContainer().isRelationExists(m1, m2)) {
                JOptionPane.showMessageDialog(null, "Такой связи нет существует", "Warn", JOptionPane.WARNING_MESSAGE);
                return;
            }
            this.getMarkersContainer().removeRelation(m1, m2);
            DB.deleteRelation(m1, m2);
            this.repaint(mapViewer);
        });

        addRelation.addActionListener(e -> {
            int id1 = Integer.parseInt(numberField1.getText());
            int id2 = Integer.parseInt(numberField2.getText());

            if (!idMap.containsKey(id1) || !idMap.containsKey(id2)) {
                JOptionPane.showMessageDialog(null, "Такого айди не существует", "Warn", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Marker m1 = idMap.get(id1);
            Marker m2 = idMap.get(id2);
            if (this.getMarkersContainer().isRelationExists(m1, m2)) {
                JOptionPane.showMessageDialog(null, "Связь уже существует", "Warn", JOptionPane.WARNING_MESSAGE);
                return;
            }
            this.createRelation(mapViewer, m1, m2);
        });

        JPanel clientsInfoPanel = new JPanel();
        clientsInfoPanel.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        clientsInfoPanel.setLayout(new BoxLayout(clientsInfoPanel, BoxLayout.Y_AXIS));

        JTextField numberField3 = new JTextField(3);
        numberField3.setPreferredSize(new Dimension(50, 30));
        numberField3.addKeyListener(keyAdapter);

        JPanel clientsPanel = new JPanel();
        clientsPanel.add(new JLabel("Все узлы:"));

        clientsPanel.setLayout(new BoxLayout(clientsPanel, BoxLayout.Y_AXIS));
        Set<Marker> markers2 = this.getMarkersContainer().getMarkers().keySet();

        final HashMap<Integer, Marker> idMap2 = new HashMap<>();
        int i2 = 0;
        for (Marker marker : markers2) {
            i2 += 1;
            JLabel markerLabel = new JLabel("id: " + i2 + ", " + marker.getMarkerType().getName() + ": " + marker.getAddress());
            clientsPanel.add(markerLabel);
            idMap2.put(i2, marker);
        }

        JScrollPane markersScrollPane2 = new JScrollPane(clientsPanel);
        markersScrollPane2.setPreferredSize(new Dimension(300, 200));
        clientsInfoPanel.add(markersScrollPane2);

        clientsInfoPanel.add(new JLabel("ID 1:"));
        clientsInfoPanel.add(numberField3);

        JButton deleteEdge = new JButton("Удалить узел");
        JButton info = new JButton("Информация");

        deleteEdge.addActionListener(e -> {
            int id1 = Integer.parseInt(numberField3.getText());

            if (!idMap2.containsKey(id1)) {
                JOptionPane.showMessageDialog(null, "Такого айди не существует", "Warn", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Marker m1 = idMap2.get(id1);
            if (m1.getMarkerType() == MarkerType.HEAD) {
                this.getAddressHandler().setHeadCenter(null);
            }
            for (Marker marker : new HashSet<>(this.getMarkersContainer().getMarkers().get(m1))) {
                if (marker.getMarkerType() == MarkerType.CLIENT) {
                    this.getMarkersContainer().removeEdge(marker);
                    DB.deleteEdge(marker);
                }
            }
            this.getMarkersContainer().removeEdge(m1);
            DB.deleteEdge(m1);
            this.repaint(mapViewer);
        });


        info.addActionListener(e -> {
            int id1 = Integer.parseInt(numberField3.getText());

            if (!idMap2.containsKey(id1)) {
                JOptionPane.showMessageDialog(null, "Такого айди не существует", "Warn", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Marker m1 = idMap2.get(id1);
            switch (m1.getMarkerType()) {
                case HEAD: {
                    JOptionPane.showMessageDialog(null, "Главный центр" + "\n" + m1.getAddress(), "Info", JOptionPane.INFORMATION_MESSAGE);
                    break;
                }
                case CLIENT: {
                    Pair<Marker, Double> pair = DB.getClientsSupplyP(m1);
                    JOptionPane.showMessageDialog(null, "Клиент" + "\n" + m1.getAddress() + "\n" + "Пункт выдачи: " + pair.getKey().getAddress() + "\n" + "Расстояние: " + pair.getValue(), "Info", JOptionPane.INFORMATION_MESSAGE);
                    break;
                }
                case SUPPLY: {
                    int busy = DB.getClientCountAttachedToMarker(m1);
                    JOptionPane.showMessageDialog(null, "Пункт выдачи" + "\n" + m1.getAddress() + "\n" + "Загруженность: " + busy, "Info", JOptionPane.INFORMATION_MESSAGE);
                    break;
                }
            }
        });

        clientsInfoPanel.add(deleteEdge);
        clientsInfoPanel.add(info);

        inputPanel.setLayout(new BorderLayout());
        inputPanel.add(mainContent, BorderLayout.NORTH);
        inputPanel.add(addressInfoPanel, BorderLayout.CENTER);
        inputPanel.add(clientsInfoPanel, BorderLayout.SOUTH);
        clientsInfoPanel.add(supplierInfoPanel, BorderLayout.SOUTH);
    }

    private void OSMMapRender(JXMapViewer mapViewer, JPanel mapPanel) {
        TileFactoryInfo tileFactoryInfo = new OSMTileFactoryInfo();
        TileFactory tileFactory = new ClassicTF(tileFactoryInfo);

        mapViewer.setTileFactory(tileFactory);
        this.mouseEvents(mapViewer);

        GeoPosition initialPosition = new GeoPosition(55.7558, 37.6176);
        AddressHandler.PointData head = this.getAddressHandler().getHeadCenter();
        if (head != null) {
            initialPosition = head.getGeoPosition();
        }
        mapViewer.setAddressLocation(initialPosition);
        mapViewer.setZoom(10);

        mapPanel.add(mapViewer, BorderLayout.CENTER);
    }

    private void mouseEvents(JXMapViewer mapViewer) {
        mapViewer.addMouseListener(new MouseAdapter() {
            private Point lastPoint;

            @Override
            public void mousePressed(MouseEvent e) {
                this.lastPoint = e.getPoint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                this.lastPoint = null;
            }
        });

        mapViewer.addMouseMotionListener(new MouseMotionAdapter() {
            private Point lastPoint;

            @Override
            public void mouseDragged(MouseEvent e) {
                if (lastPoint != null) {
                    Point currentPoint = e.getPoint();

                    int deltaX = currentPoint.x - lastPoint.x;
                    int deltaY = currentPoint.y - lastPoint.y;

                    GeoPosition currentGeoPosition = mapViewer.getAddressLocation();
                    double latitude = currentGeoPosition.getLatitude();
                    double longitude = currentGeoPosition.getLongitude();

                    double zoomFactor = Math.pow(2, mapViewer.getZoom());

                    double f = 0.0000015d;

                    double latitudeOffset = deltaY * -(f * zoomFactor);
                    double longitudeOffset = deltaX * -(f * zoomFactor);

                    mapViewer.setAddressLocation(new GeoPosition(latitude - latitudeOffset, longitude + longitudeOffset));
                }

                lastPoint = e.getPoint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                lastPoint = null;
            }
        });

        mapViewer.addMouseWheelListener(e -> {
            int notches = e.getWheelRotation();
            int zoom = mapViewer.getZoom();
            if (notches < 0) {
                mapViewer.setZoom(Math.max(zoom - 1, 0));
            } else {
                mapViewer.setZoom(Math.min(zoom + 1, 15));
            }
        });
    }

    public MarkersContainer getMarkersContainer() {
        return this.markersContainer;
    }

    public AddressHandler getAddressHandler() {
        return this.addressHandler;
    }

    private static class ClassicTF extends AbstractTileFactory {
        public ClassicTF(TileFactoryInfo info) {
            super(info);
        }
    }
}