#!/usr/bin/env python
# encoding: utf-8
import json
import pprint

pp = pprint.PrettyPrinter(indent=2)


def get_ip_addrs(ipv4nets=None):
    ip_addrs = []

    for net in ipv4nets.keys():
        ipv4net = ipv4nets[net]
        for router in ipv4net.keys():
            ipv4net_info = ipv4net[router]

            mbps = ipv4net_info["high_speed"]
            int_name = ipv4net_info["int_name"]
            alias = ipv4net_info["alias"]
            admin = ipv4net_info["admin"]

            address, address_info = filter_address_info(ipv4net_info=ipv4net_info)
            if not address:
                continue

            ip_entry = {
                "router": router,
                "mbps": mbps,
                "int_name": int_name,
                "admin": admin,
                "address": address,
                "alias": alias,
            }
            port = guess_port(ipv4net_info)

            if port:
                ip_entry["port"] = port
            if "mac" in ipv4net_info.keys():
                ip_entry["mac"] = ipv4net_info["mac"]
            if "circuit" in ipv4net_info.keys():
                ip_entry["circuit"] = ipv4net_info["circuit"].keys()
            if "oscars" in ipv4net_info.keys():
                ip_entry["oscars"] = ipv4net_info["oscars"].keys()
            if "bgp_peers" in address_info.keys():
                ip_entry["bgp_peers"] = []
                for bgp_peer_addr in address_info["bgp_peers"].keys():
                    bgp_info = address_info["bgp_peers"][bgp_peer_addr]
                    bgp_entry = {
                        "peer_addr": bgp_peer_addr,
                        "remote_as": bgp_info["remote_as"]
                    }
                    ip_entry["bgp_peers"].append(bgp_entry)

            ip_addrs.append(ip_entry)

    return ip_addrs


def get_ports_by_rtr(ipv4nets=None):
    ports_by_rtr = {}

    for net in ipv4nets.keys():
        ipv4net = ipv4nets[net]
        for router in ipv4net.keys():
            ipv4net_info = ipv4net[router]

            mbps = ipv4net_info["high_speed"]
            int_name = ipv4net_info["int_name"]
            alias = ipv4net_info["alias"]

            port = guess_port(ipv4net_info)

            if port:
                if router not in ports_by_rtr.keys():
                    ports_by_rtr[router] = {}
                if port not in ports_by_rtr[router].keys():
                    ports_by_rtr[router][port] = []

                entry = {
                    "router": router,
                    "int_name": int_name,
                    "mbps": mbps,
                    "alias": alias
                }

                ports_by_rtr[router][port].append(entry)

    return ports_by_rtr


def get_devices(routers=None):
    devices = []

    for router_name in routers.keys():
        router_info = routers[router_name]
        entry = {
            "name": router_name,
            "os": router_info["os"],
            "description": router_info["description"]
        }
        devices.append(entry)

    return devices


def latency_of(addr=None, latency_db=None):
    if addr in latency_db:
        return latency_db[addr]["latency"]
    return 10


def make_isis_graph(isis=None):
    routers = []

    edges = []
    for addr in isis.keys():
        entry = isis[addr]
        neighbor = entry["isis_neighbor"]
        if neighbor in isis:
            neighbor_entry = isis[neighbor]
            check_isis_neighborship(entry, neighbor_entry)
            edge = {
                "a": entry["router"],
                "z": neighbor_entry["router"],
                "mbps": entry["mbps"],
                "a_ifce": entry["int_name"],
                "z_ifce": neighbor_entry["int_name"],
                "a_addr": addr,
                "z_addr": neighbor,
                "isis_cost": entry["isis_cost"],
                "admin": entry["admin"],
                "latency": entry["latency"]
            }
            if "port" in entry.keys():
                edge["a_port"] = entry["port"]

            if "port" in neighbor_entry.keys():
                edge["z_port"] = neighbor_entry["port"]

            if entry["router"] not in routers:
                routers.append(entry["router"])

            edges.append(edge)
    print(json.dumps(isis, indent=4))
    return edges


def check_isis_neighborship(entry_a, entry_b):
    assert entry_a["isis_neighbor"] == entry_b["address"], "%s %s " % (entry_a["isis_neighbor"], entry_b["address"])
    assert entry_b["isis_neighbor"] == entry_a["address"], "%s %s " % (entry_b["isis_neighbor"], entry_a["address"])


def get_isis_neighbors(ipv4nets=None, latency_db=None):
    isis = {}

    for net in ipv4nets.keys():
        ipv4net = ipv4nets[net]
        for router in ipv4net.keys():
            ipv4net_info = ipv4net[router]
            ip_addr = ipv4net_info["ip_addr"]
            admin = ipv4net_info["admin"]

            mbps = ipv4net_info["high_speed"]
            int_name = ipv4net_info["int_name"]

            if len(ip_addr.keys()) == 1:
                address = list(ip_addr)[0]
                address_info = ip_addr[address]
                if "mask" not in address_info.keys():
                    continue
                else:
                    mask = address_info["mask"]

                if "isis_cost" in address_info.keys():

                    isis_cost = address_info["isis_cost"]
                    isis_status = str(address_info["isis_status"])

                    if isis_status == '1':
                        isis_neighbor = address_info["isis_neighbor"]
                        latency = latency_of(addr=address, latency_db=latency_db)
                        isis_entry = {
                            "address": address,
                            "router": router,
                            "latency": latency,
                            "mask": mask,
                            "int_name": int_name,
                            "admin": admin,
                            "mbps": mbps,
                            "isis_neighbor": isis_neighbor,
                            "isis_cost": isis_cost
                        }
                        port = guess_port(ipv4net_info)

                        if port:
                            isis_entry["port"] = port

                        isis[address] = isis_entry

    return isis


def filter_address_info(ipv4net_info=None):
    ip_addr = ipv4net_info["ip_addr"]

    if len(ip_addr.keys()) == 1:
        address = list(ip_addr)[0]
        address_info = ip_addr[address]
        return address, address_info

    else:
        weird = True
        for address in ip_addr.keys():
            if address == "128.0.0.1":
                weird = False
            alias = str(ipv4net_info["alias"])
            if alias.find("stub"):
                weird = False

        if weird:
            pp.pprint(ipv4net_info)
            return None, None
        else:
            return None, None


def guess_port(ipv4net_info=None):
    if "port" in ipv4net_info.keys():
        return ipv4net_info["port"]

    int_name = str(ipv4net_info["int_name"])
    juniper_ifce_prefix = ["ge", "xe", "ae", "fe", "t3"]

    for prefix in juniper_ifce_prefix:
        if int_name.startswith(prefix):
            return int_name.split(".")[0]

    return None


def get_vlans(today_vlans=None):
    vlans = []

    for vlan_id in today_vlans.keys():
        by_vlan_id = today_vlans[vlan_id]
        for router in by_vlan_id.keys():
            for snmp_idx in by_vlan_id[router].keys():
                vlan_info = by_vlan_id[router][snmp_idx]

                mbps = vlan_info["high_speed"]
                int_name = vlan_info["int_name"]
                alias = vlan_info["alias"]
                admin = vlan_info["admin"]
                entry = {
                    "vlan_id": int(vlan_id),
                    "router": router,
                    "int_name": int_name,
                    "alias": alias,
                    "admin": admin,
                    "mbps": mbps
                }
                if "ip_addr" in vlan_info.keys():
                    address = list(vlan_info["ip_addr"])[0]
                    entry["address"] = address

                vlans.append(entry)

    return vlans
