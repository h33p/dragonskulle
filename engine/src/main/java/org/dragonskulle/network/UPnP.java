/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.extern.java.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class can be used for automatic port-forwarding and also to acquire the external IP address
 * of the network.
 *
 * @author Harry Stoltz
 *     <p>After initialisation the possible commands from the WANIPConnection service that can be
 *     used are: AddPortMapping, DeletePortMapping and GetExternalIPAddress.
 */
@Log
public class UPnP {

    private static final byte[] QUERY =
            ("M-SEARCH * HTTP/1.1\r\n"
                            + "HOST: 239.255.255.250:1900\r\n"
                            + "MAN: \"ssdp:discover\"\r\n"
                            + "MX: 1\r\n"
                            + "ST: urn:schemas-upnp-org:service:WANIPConnection:1\r\n\r\n")
                    .getBytes();

    private static final String SERVICE_NAME = "urn:schemas-upnp-org:service:WANIPConnection:1";

    private static final Map<Integer, Collection<String>> sMappings = new HashMap<>();
    private static Inet4Address sLocal;
    private static URL sControlURL;
    private static boolean sInitialised;

    /**
     * Prepare the UPnP class for use by getting the local address from the currently active network
     * interface and locating the WANIPConnection service on that interface.
     *
     * <p>If initialisation is successful the other methods in this class will be usable, otherwise
     * they will always fail.
     */
    public static void initialise() {
        ArrayList<Inet4Address> localAddresses = getLocalAddresses();
        Thread[] threads = new Thread[localAddresses.size()];
        byte[][] received = new byte[localAddresses.size()][];

        for (int i = 0; i < localAddresses.size(); i++) {
            final int idx = i;
            final Inet4Address address = localAddresses.get(i);

            Runnable searcher =
                    () -> {
                        try {
                            DatagramSocket socket = new DatagramSocket(0, address);
                            socket.send(
                                    new DatagramPacket(
                                            QUERY,
                                            QUERY.length,
                                            new InetSocketAddress("239.255.255.250", 1900)));
                            try {
                                DatagramPacket packet = new DatagramPacket(new byte[2048], 2048);
                                socket.setSoTimeout(2000);
                                socket.receive(packet);
                                received[idx] = packet.getData();
                                sLocal = address;
                                log.fine(
                                        "Received response to query on address : "
                                                + address.getHostAddress());
                            } catch (SocketTimeoutException e) {
                                log.fine(
                                        "Didn't receive response to query on address : "
                                                + address.getHostAddress());
                            }
                        } catch (SocketException e) {
                            log.warning(
                                    "Failed to create UDP socket on address : "
                                            + address.getHostAddress());
                        } catch (IOException e) {
                            log.warning(
                                    "Failed to broadcast query on address : "
                                            + address.getHostAddress());
                        }
                    };

            threads[i] = new Thread(searcher);
            threads[i].start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for (byte[] data : received) {
            if (data != null && processResponse(data)) {
                log.fine("UPnP initialised");
                sInitialised = true;
                return;
            }
        }
        log.severe(
                "Failed to initialise UPnP. Hosting a lobby will not be possible without manual port forwarding");
    }

    /**
     * Get the external IP address for the current network.
     *
     * @return A string containing the IPv4 address of the network, or null if unable to
     */
    public static String getExternalIPAddress() {
        if (!sInitialised) {
            return null;
        }
        Map<String, String> out = executeCommand("GetExternalIPAddress");
        return out == null ? null : out.get("NewExternalIPAddress");
    }

    /**
     * Add a new port mapping via UPnP.
     *
     * @param port Port to be forwarded to local address. Must be in the range 0 - 65535
     * @param protocol Protocol of the new mapping. Can be TCP, UDP or BOTH
     * @return true if the port mapping was added successfully, false otherwise
     */
    public static boolean addPortMapping(int port, String protocol) {
        if (!sInitialised) {
            return false;
        }
        if (protocol.equals("BOTH")) {
            return addPortMapping(port, "TCP") & addPortMapping(port, "UDP");
        }

        Map<String, String> out =
                executeCommand(
                        "AddPortMapping",
                        "NewRemoteHost",
                        "",
                        "NewExternalPort",
                        Integer.toString(port),
                        "NewProtocol",
                        protocol,
                        "NewInternalPort",
                        Integer.toString(port),
                        "NewInternalClient",
                        sLocal.getHostAddress(),
                        "NewEnabled",
                        "",
                        "NewPortMappingDescription",
                        "",
                        "NewLeaseDuration",
                        "0");

        if (out != null) {
            storePortMapping(port, protocol);
            log.fine("Added port mapping " + protocol + " : " + port);
            return true;
        } else {
            log.warning("Failed to add port mapping " + protocol + " : " + port);
            return false;
        }
    }

    /**
     * Delete an existing port mapping.
     *
     * @param port Port of the mapping to be deleted
     * @param protocol Protocol of the mapping to be deleted
     * @return true if the mapping was deleted successfully, false otherwise
     */
    public static boolean deletePortMapping(int port, String protocol) {
        if (!sInitialised) {
            return false;
        }
        if (protocol.equals("BOTH")) {
            return deletePortMapping(port, "TCP") & deletePortMapping(port, "UDP");
        }

        Map<String, String> out =
                executeCommand(
                        "DeletePortMapping",
                        "NewRemoteHost",
                        "",
                        "NewExternalPort",
                        Integer.toString(port),
                        "NewProtocol",
                        protocol);

        if (out != null) {
            removePortMapping(port, protocol);
            log.fine("Deleted port mapping " + protocol + " : " + port);
            return true;
        } else {
            log.fine("Failed to delete port mapping " + protocol + " : " + port);
            return false;
        }
    }

    /**
     * Check if a given port and protocol is already mapped to an address.
     *
     * @param port The port to check
     * @param protocol The protocol to check. Either TCP, UDP or BOTH
     * @return true if the port is not mapped, false is the port is already mapped to an address. If
     *     the protocol is BOTH, true is only returned if both TCP AND UDP are available. If the
     *     port is mapped to the current address already, false is still returned.
     */
    public static boolean isPortAvailable(int port, String protocol) {
        if (!sInitialised) {
            return false;
        }
        if (protocol.equals("BOTH")) {
            return isPortAvailable(port, "TCP") & isPortAvailable(port, "UDP");
        }

        Map<String, String> out =
                executeCommand(
                        "GetSpecificPortMappingEntry",
                        "NewRemoteHost",
                        "",
                        "NewExternalPort",
                        Integer.toString(port),
                        "NewProtocol",
                        protocol);

        return out == null;
    }

    /** Delete all port mappings that were added at runtime. */
    public static void deleteAllMappings() {
        if (!sInitialised) {
            return;
        }
        Set<Map.Entry<Integer, Collection<String>>> entries = sMappings.entrySet();
        for (Map.Entry<Integer, Collection<String>> entry : entries) {
            for (String protocol : entry.getValue()) {
                deletePortMapping(entry.getKey(), protocol);
            }
        }
    }

    /**
     * Check if we have mapped a port with a specific protocol during runtime.
     *
     * @param port Port to check
     * @param protocol Protocol to check
     * @return true if the port is mapped with that protocol, false otherwise
     */
    public static boolean checkMappingExists(Integer port, String protocol) {
        if (sMappings.containsKey(port)) {
            return sMappings.get(port).stream()
                            .filter(p -> p.equals(protocol))
                            .findFirst()
                            .orElse(null)
                    != null;
        } else {
            return false;
        }
    }

    /**
     * Store a new port mapping in the map of mappings.
     *
     * @param port Port of the new mapping
     * @param protocol Protocol of the new mapping
     */
    private static void storePortMapping(Integer port, String protocol) {
        if (sMappings.containsKey(port)) {
            sMappings.get(port).add(protocol);
        } else {
            ArrayList<String> values = new ArrayList<>();
            values.add(protocol);
            sMappings.put(port, values);
        }
    }

    /**
     * Remove an existing port mapping from the map of mappings.
     *
     * @param port Port of the mapping to remove
     * @param protocol Protocol of the mapping to remove
     */
    private static void removePortMapping(Integer port, String protocol) {
        if (sMappings.containsKey(port)) {
            Collection<String> values = sMappings.get(port);
            if (values.size() == 1) {
                sMappings.remove(port);
            } else {
                values.remove(protocol);
            }
        }
    }

    /**
     * Execute a command via the control URL. The arguments should be passed with the name of the
     * argument followed by the value. e.g. executeCommand("Command", "Arg1", "7") would execute the
     * command "Command" with "Arg1" = "7".
     *
     * @param command Name of the command to execute
     * @param args List of arguments for the command
     * @return A Map containing the returned values and their names
     */
    private static Map<String, String> executeCommand(String command, String... args) {
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\"?>\r\n");
        builder.append("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"");
        builder.append(" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">");
        builder.append("<s:Body><u:");
        builder.append(command);
        builder.append(" xmlns:u=\"");
        builder.append(SERVICE_NAME);
        builder.append("\">");

        if (args != null) {
            for (int i = 0; i < args.length; i += 2) {
                builder.append("<");
                builder.append(args[i]);
                builder.append(">");
                builder.append(args[i + 1]);
                builder.append("</");
                builder.append(args[i]);
                builder.append(">");
            }
        }

        builder.append("</u:");
        builder.append(command);
        builder.append("></s:Body></s:Envelope>");

        HashMap<String, String> out = new HashMap<>();
        try {
            byte[] data = builder.toString().getBytes();

            HttpURLConnection connection = (HttpURLConnection) sControlURL.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Length", Integer.toString(data.length));
            connection.setRequestProperty("Content-Type", "text/xml");
            connection.setRequestProperty(
                    "SOAPACTION",
                    "\"urn:schemas-upnp-org:service:WANIPConnection:1#" + command + "\"");

            connection.getOutputStream().write(data);

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                Document doc =
                        DocumentBuilderFactory.newInstance()
                                .newDocumentBuilder()
                                .parse(connection.getInputStream());

                Node response = doc.getElementsByTagName("u:" + command + "Response").item(0);

                for (Node n = response.getFirstChild(); n != null; n = n.getNextSibling()) {
                    out.put(n.getNodeName(), n.getTextContent());
                }
                return out;
            }
        } catch (Exception e) {
            log.warning("Error making request to UPnP Service control URL");
        }
        return null;
    }

    /**
     * Process a response from the search query and attempt to extract the control url for the
     * WANIPConnection service.
     *
     * @param received The received bytes
     * @return true if the control url was found
     */
    private static boolean processResponse(byte[] received) {
        String response = new String(received);
        StringTokenizer tokenizer = new StringTokenizer(response, "\r\n");

        String url = null;
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.startsWith("LOCATION:")) {
                url = token.substring(10);
            }
        }
        if (url == null) {
            return false;
        }
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url);
            NodeList nodes = doc.getElementsByTagName("service");

            for (int i = 0; i < nodes.getLength(); i++) {
                NodeList serviceNodes = nodes.item(i).getChildNodes();
                String serviceType = serviceNodes.item(0).getTextContent();
                if (!serviceType.equals(SERVICE_NAME)) {
                    continue;
                }
                String control = serviceNodes.item(3).getTextContent();
                sControlURL = new URL(url.substring(0, url.lastIndexOf('/')) + control);
                return true;
            }
        } catch (Exception e) {
            log.warning("Error parsing UPnP service description xml");
            return false;
        }
        return false;
    }

    /**
     * Iterate through all active, physical network interfaces and collect all IPv4 addresses
     * associated with them.
     *
     * @return A new ArrayList containing the addresses that were found
     */
    private static ArrayList<Inet4Address> getLocalAddresses() {
        ArrayList<Inet4Address> out = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface i = interfaces.nextElement();
                if (!i.isUp() || i.isLoopback() || i.isPointToPoint() || i.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = i.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address) {
                        out.add((Inet4Address) address);
                    }
                }
            }
        } catch (SocketException e) {
            log.warning("Unable to get network interfaces. UPnP unusable");
            return out;
        }
        return out;
    }
}
