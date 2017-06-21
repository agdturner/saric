/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.leeds.ccg.andyt.projects.saric.data.osm;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

//import org.osm.lights.diff.OSMNode;
//import uk.ac.leeds.ccg.andyt.projects.saric.data.osm.Authenticator;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * http://wiki.openstreetmap.org/wiki/Java_Access_Example
 *
 * @author geoagdt
 */
public class OSM {

    private static final String OPENSTREETMAP_API_06 = "http://www.openstreetmap.org/api/0.6/";

    public OSM() {
    }

    /**
     * main method that simply reads some nodes
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            new OSM().run();
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Error e) {
            e.printStackTrace();
        }
    }

    public void run() throws IOException, SAXException, ParserConfigurationException {
        //Authenticator.setDefault(new Authenticator("username", "password"));
        List<OSMNode> osmNodesInVicinity = getOSMNodesInVicinity(49, 8.3, 0.005); //http://www.openstreetmap.org/api/0.6/map?bbox=48.095,8.295,49.005,8.305
//        List<OSMNode> osmNodesInVicinity = getOSMNodesInVicinity(52.7766, 0.6317, 0.3);
//        List<OSMNode> osmNodesInVicinity = getOSMNodesInVicinity(52.7766, 0.6317, 0.25); //http://www.openstreetmap.org/api/0.6/map?bbox=52.5266,0.3817,53.0266,0.8817
//        List<OSMNode> osmNodesInVicinity = getOSMNodesInVicinity(52.565546, 0.526386, 0.25);
        for (OSMNode osmNode : osmNodesInVicinity) {
            System.out.println(osmNode.getId() + ":" + osmNode.getLat() + ":" + osmNode.getLon());
        }
    }

    public static OSMNode getNode(String nodeId) throws IOException, ParserConfigurationException, SAXException {
        String string = OPENSTREETMAP_API_06 + "node/" + nodeId;
        URL osm = new URL(string);
        HttpURLConnection connection;
        connection = (HttpURLConnection) osm.openConnection();
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document document = docBuilder.parse(connection.getInputStream());
        List<OSMNode> nodes = getNodes(document);
        if (!nodes.isEmpty()) {
            return nodes.iterator().next();
        }
        return null;
    }

    /**
     *
     * @param lon the longitude
     * @param lat the latitude
     * @param vicinityRange bounding box in this range
     * @return the xml document containing the queries nodes
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    @SuppressWarnings("nls")
    private static Document getXML(double lon, double lat, double vicinityRange) throws IOException, SAXException,
            ParserConfigurationException {

        DecimalFormat format = new DecimalFormat("##0.0000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH)); //$NON-NLS-1$
        String left = format.format(lat - vicinityRange);
        String bottom = format.format(lon - vicinityRange);
        String right = format.format(lat + vicinityRange);
        String top = format.format(lon + vicinityRange);

        String string = OPENSTREETMAP_API_06 + "map?bbox=" + left + "," + bottom + "," + right + ","
                + top;
        URL osm = new URL(string);
        HttpURLConnection connection = (HttpURLConnection) osm.openConnection();

        DocumentBuilderFactory dbf;
        dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document d;
        d = db.parse(connection.getInputStream());
        return d;
    }

    /**
     *
     * @param d
     * @return a list of openseamap nodes extracted from xml
     */
    @SuppressWarnings("nls")
    public static List<OSMNode> getNodes(Document d) {
        List<OSMNode> osmNodes = new ArrayList<OSMNode>();

        // Document xml = getXML(8.32, 49.001);
        Node osmRoot = d.getFirstChild();
        NodeList osmXMLNodes = osmRoot.getChildNodes();
        for (int i = 1; i < osmXMLNodes.getLength(); i++) {
            Node item = osmXMLNodes.item(i);
            if (item.getNodeName().equals("node")) {
                NamedNodeMap attributes = item.getAttributes();
                NodeList tagXMLNodes = item.getChildNodes();
                Map<String, String> tags = new HashMap<String, String>();
                for (int j = 1; j < tagXMLNodes.getLength(); j++) {
                    Node tagItem = tagXMLNodes.item(j);
                    NamedNodeMap tagAttributes = tagItem.getAttributes();
                    if (tagAttributes != null) {
                        tags.put(tagAttributes.getNamedItem("k").getNodeValue(), tagAttributes.getNamedItem("v")
                                .getNodeValue());
                    }
                }
                Node namedItemID = attributes.getNamedItem("id");
                Node namedItemLat = attributes.getNamedItem("lat");
                Node namedItemLon = attributes.getNamedItem("lon");
                Node namedItemVersion = attributes.getNamedItem("version");

                String id = namedItemID.getNodeValue();
                String latitude = namedItemLat.getNodeValue();
                String longitude = namedItemLon.getNodeValue();
                String version = "0";
                if (namedItemVersion != null) {
                    version = namedItemVersion.getNodeValue();
                }

                osmNodes.add(new OSMNode(id, latitude, longitude, tags, version));
            }

        }
        return osmNodes;
    }

    public static List<OSMNode> getOSMNodesInVicinity(
            double lat,
            double lon,
            double vicinityRange) throws IOException,
            SAXException, ParserConfigurationException {
        return OSM.getNodes(getXML(lon, lat, vicinityRange));
    }

}
