package net.es.oscars.sb.nso.rest;

/**
 * The LiveStatusMockData class contains mock data for the NSO live status queries when the
 * nso.mock-live-show-commands=true flag is set in the application.properties file.
 */
public class LiveStatusMockData {

    public final static String SDP_MOCK_DATA =
            "\n\r\n" +
            "===============================================================================\r\n" +
            "Services: Service Destination Points\r\n" +
            "===============================================================================\r\n" +
            "SdpId            Type     Far End addr    Adm     Opr       I.Lbl     E.Lbl\r\n" +
            "-------------------------------------------------------------------------------\r\n" +
            "7002:7005        Spok     134.55.200.174  Up      Up        524108    524262\r\n" +
            "7003:7008        Spok     134.55.200.174  Up      Down      524101    None\r\n" +
            "-------------------------------------------------------------------------------\r\n" +
            "Number of SDPs : 2\r\n" +
            "-------------------------------------------------------------------------------\r\n" +
            "===============================================================================\r\n" +
            "A:star-cr6# ";


    // request devices device star-cr6 live-status exec show args service id 7005 sap
    public final static String SAP_MOCK_DATA =
            "\n\r\n" +
            "===============================================================================\r\n" +
            "SAP(Summary), Service 7005\r\n" +
            "===============================================================================\r\n" +
            "PortId                          SvcId      Ing.  Ing.    Egr.  Egr.   Adm  Opr\r\n" +
            "                                           QoS   Fltr    QoS   Fltr        \r\n" +
            "-------------------------------------------------------------------------------\r\n" +
            "2/1/c5/1:1814                   7005       7001  none    7001  none   Up   Up\r\n" +
            "-------------------------------------------------------------------------------\r\n" +
            "Number of SAPs : 1\r\n" +
            "-------------------------------------------------------------------------------\r\n" +
            "===============================================================================\r\n" +
            "A:star-cr6# ";


    public final static String LSP_MOCK_DATA =
            "\n\r\n" +
            "===============================================================================\r\n" +
            "MPLS LSPs (Originating)\r\n" +
            "===============================================================================\r\n" +
            "LSP Name                                            Tun     Fastfail  Adm  Opr\r\n" +
            "  To                                                Id      Config         \r\n" +
            "-------------------------------------------------------------------------------\r\n" +
            "doe-in-vpls_albq-cr6                                1       No        Up   Up\r\n" +
            "  134.55.200.169                                                           \r\n" +
            "6999---srs70344a-cr6                                2       No        Up   Up\r\n" +
            "  134.55.200.230                                                           \r\n" +
            "6999---pantex-cr6                                   50      No        Up   Up\r\n" +
            "  134.55.200.216                                                           \r\n" +
            "JMTF-WRK-anl541b-cr6                                53      No        Up   Up\r\n" +
            "  134.55.200.174                                                           \r\n" +
            "JMTF-PRT-anl541b-cr6                                54      No        Up   Up\r\n" +
            "  134.55.200.174                                                           \r\n" +
            "-------------------------------------------------------------------------------\r\n" +
            "LSPs : 5\r\n" +
            "===============================================================================\r\n" +
            "A:star-cr6# ";

}
