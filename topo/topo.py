#!/usr/bin/env python3
# encoding: utf-8

import argparse
import json
import requests
import os
import sys
import errno
import pprint
from mergedeep import merge, Strategy

from operator import itemgetter
from itertools import groupby
from today.funcs import get_isis_neighbors, make_isis_graph, get_devices, get_ports_by_rtr, get_ip_addrs, get_vlans

# This script produces an OSCARS-style topology dataset:
#   devices.json        : the devices inventory with all the ports per device,
#   adjacencies.json    : the adjacencies between backbone links

# order of operations:
# 1. Get latest network data from ESDB and Netbeam.
#      In development, to improve turnaround:
#      Set --save to save data as files in ./cache (first run)
#      Set --fast to retrieve from cache (subsequent runs)
# 2. Create topology from network data
#      The OSCARS-style topology structure is created and goes through
#      several processing steps. We need some config-provided topology hints from
#      config files: lags.json, switches.json, vars.json
# 3. Override properties with matches from overrides.json (TODO)
#       The file contains override definitions for ports, devices and adjacencies.
#       An output object that matches will have its properties overridden
#       with those defined in the file. Definitions may also add objects that are
#       altogether missing from the network data.
# 4. Filter topology with matches from filters.json (TODO)
#       The file contains filter definitions. These are applied in order on the
#       topology structure. The intent is to be able to easily filter out
#       devices / ports. The default is that everything passes the fiter unless
#       explicitely filtered out.
#       If a device is filtered out, all its ports are filtered out as well.
#       If a port is filtered out (directly or indirectly because a device
#       was filtered out), the related adjacencies will be filtered out as well.

pp = pprint.PrettyPrinter(indent=2)


def main():
    settings = {
        'VERBOSE': False,

        'URLS': {
            'NETBEAM': "https://esnet-netbeam.appspot.com/api/network/esnet/prod",
            'ESDB': "https://esdb.es.net/esdb_api/v1",
        },
        'DATASETS': {
            'INTERFACES': {
                'SOURCE': 'NETBEAM',
                'URL': '/interfaces',
                'CACHE_FILENAME': 'interfaces.json'
            },
            'SAPS': {
                'SOURCE': 'NETBEAM',
                'URL': '/saps',
                'CACHE_FILENAME': 'saps.json'
            },
            'TODAY': {
                'SOURCE': 'ESDB',
                'URL': '/topology/today/1',
                'CACHE_FILENAME': 'today.json'
            },
            'EQUIPMENT': {
                'SOURCE': 'ESDB',
                'URL': '/equipment/?limit=0&detail=list',
                'CACHE_FILENAME': 'equipment.json'

            },
            'LOCATIONS': {
                'SOURCE': 'ESDB',
                'URL': '/location/?limit=0',
                'CACHE_FILENAME': 'locations.json'
            }

        },

        'CONFIG': {
            'DIR': "./config/",
            'ESDB_TOKEN': 'esdb_token.txt',
            'FILES': {
                'VARS': "vars.json",
                'LAGS': "lags.json",
                'OVERRIDES': "overrides.json",
                'SWITCHES': "switches.json",
            }
        },

        'OUTPUT': {
            'DIR': "./output/",
            'DEVICES': 'devices.json',
            'ADJCIES': 'adjcies.json'
        },

        'CACHE': {
            'DIR': './cache',
            'SAVE': False,
            'USE': False,
        }


    }

    parser = argparse.ArgumentParser(description='OSCARS topology generator')
    parser.add_argument('-v', '--verbose', action='count', default=0,
                        help="set verbosity level")
    parser.add_argument('--cache-dir', default=settings['CACHE']['DIR'], help="Set cache directory")

    cache_action = parser.add_mutually_exclusive_group(required=False)
    cache_action.add_argument('-s', '--save-cache', action='count', default=0,
                              help="Save server data to cache dir")
    cache_action.add_argument('-f', '--use-cache', action='count', default=0,
                              help="Fast mode (use cached data)")

    parser.add_argument('--config-dir', default=settings['CONFIG']['DIR'], help="Set config directory")
    parser.add_argument('--output-dir', default=settings['OUTPUT']['DIR'], help="Set output directory")

    opts = parser.parse_args()

    process_options(opts, settings)

    config = load_config(settings)

    adjacencies, devices = create_topology(settings, config)

    write_output(adjacencies, devices, settings)


def create_topology(settings, config):

    datasets = load_datasets(settings)

    today = datasets['TODAY']['topology_snapshots'][0]['data']['today']
    latency = datasets['TODAY']['topology_snapshots'][0]['data']['latency']
    netbeam_interfaces = datasets['INTERFACES']
    netbeam_saps = datasets['SAPS']

    today_isis = get_isis_neighbors(today['ipv4net'], latency)
    isis_adjcies = make_isis_graph(today_isis)
    addrs = get_ip_addrs(today['ipv4net'])

    repair_blank_isis_ports(isis_adjcies, netbeam_interfaces, settings)

    (oscars_adjcies, igp_portmap) = to_oscars_adjcies(isis_adjcies=isis_adjcies)
    validate_oscars_adjacencies(oscars_adjcies)
    # done with OSCARS MPLS adjacencies

    today_devices = get_devices(today['router_system'])

    oscars_devices = to_oscars_devices(in_devices=today_devices,
                                       datasets=datasets,
                                       igp_portmap=igp_portmap,
                                       addrs=addrs,
                                       settings=settings,
                                       config=config)
    merge_switch_devices(oscars_devices, config=config)
    # done with device base attributes

    # now for the device ports
    #
    # first we will consider the ports with isis L3 interfaces
    insert_isis_ports(oscars_devices, igp_portmap)

    #    pp.pprint(oscars_devices)

    esdb_ports_by_rtr = get_ports_by_rtr(today['ipv4net'])
    esdb_vlans = get_vlans(today['VLAN'])

    # find all used VLANs on all ports
    used_vlans, oscars_vlans = gather_used_vlans(netbeam_saps, netbeam_interfaces, esdb_ports_by_rtr, esdb_vlans)

    # decide which ports are customer-facing
    edge_ports = gather_edge_ports(netbeam_saps, netbeam_interfaces, esdb_ports_by_rtr, oscars_devices)

    merge_lags(edge_ports, config)

    # merge all this info into the oscars_devices
    merge_edge_ports_vlans(oscars_devices, edge_ports, used_vlans, oscars_vlans, config)

    # finally merge any overrides
    merge_overrides(oscars_devices, config)

    return oscars_adjcies, oscars_devices


def merge_lags(edge_ports, config):
    lags = config['LAGS']

    for device in lags:
        for lag in lags[device]:
            lag_found = False
            mbps = lags[device][lag]["mbps"]
            for entry in edge_ports:
                if entry['device'] == device and entry['port'] == lag:
                    entry['lag'] = 'parent'
                    lag_found = True
                    if entry['speed'] != mbps:
                        entry['speed'] = mbps

            lag_sites = []
            for lag_port in lags[device][lag]["ports"]:
                port_found = False
                for entry in edge_ports:
                    if entry['device'] == device and entry['port'] == lag_port:
                        entry['lag'] = 'member'
                        port_found = True
#                        pp.pprint(entry)
#                        print("lag port found: {}:{} ".format(entry['device'], entry['port']))
                        for site in entry['sites']:
                            if site not in lag_sites:
                                lag_sites.append(site)

                if not port_found and lags[device][lag]["enable"]:
                    eprint("lag port not found: {}:{}".format(device, lag_port))

            if not lag_found and lags[device][lag]["enable"]:
                new_entry = {
                    "port": lag,
                    "oscars": False,
                    "sap": False,
                    "interface": False,
                    "device": device,
                    "speed": mbps,
                    'tags': [],
                    "lag": "parent",
                    "source": "lags",
                    "sites": lag_sites
                }
                edge_ports.append(new_entry)


def merge_switch_devices(oscars_devices=None, config=None):
    for sw_name, sw_entry in config['SWITCHES'].items():
        print(sw_name)
        for d_entry in oscars_devices:
            if sw_name == d_entry['urn']:
                print("bad urn")


def merge_edge_ports_vlans(oscars_devices=None, edge_ports=None, used_vlans=None, oscars_vlans=None, config=None):
    for entry in edge_ports:
        port_urn = entry['device'] + ':' + entry['port']
        device_urn = entry['device']
        if 'speed' not in entry or entry['speed'] is None:
            eprint("could not determine speed for {}, skipping".format(port_urn))
            continue
        if "lag" in entry and entry['lag'] == 'member':
            continue

        device = None
        for d in oscars_devices:
            if d['urn'] == device_urn:
                device = d
        if not device:
            eprint("could not find device {}".format(device_urn))
            continue

        found = False
        port_used_vlans = []
        if port_urn in used_vlans:
            port_used_vlans = used_vlans[port_urn]

        port_oscars_vlans = []
        if port_urn in oscars_vlans:
            port_oscars_vlans = oscars_vlans[port_urn]

        for p in device['ports']:
            if port_urn == p['urn']:
                if "ETHERNET" not in p["capabilities"]:
                    p["capabilities"].append("ETHERNET")
                p["tags"] = entry['tags']
                p["reservableVlans"] = make_reservable_vlans(port_used_vlans, port_oscars_vlans, config)
                found = True
        if not found:
            port_entry = {
                "urn": port_urn,
                "ifces": [],
                "tags": entry['tags'],
                "capabilities": ["ETHERNET"],
                "reservableIngressBw": entry['speed'],
                "reservableEgressBw": entry['speed'],
                "reservableVlans": make_reservable_vlans(port_used_vlans, port_oscars_vlans, config)
            }
            device["ports"].append(port_entry)


def insert_isis_ports(oscars_devices=None, igp_portmap=None):
    for dev_urn in igp_portmap:
        for oscars_device in oscars_devices:
            if oscars_device["urn"] == dev_urn:
                for port in igp_portmap[dev_urn]:
                    port_urn = dev_urn + ":" + port

                    mbps = igp_portmap[dev_urn][port]["mbps"]
                    ifce_names = igp_portmap[dev_urn][port]["ifces"]
                    ifces = []
                    tags = []
                    for ifce_name in ifce_names:
                        ifce_urn = dev_urn + ":" + ifce_name
                        tags.append(ifce_name)
                        ifces.append({
                            "urn": ifce_urn,
                            "port": port_urn,
                            "ipv4Address": igp_portmap[dev_urn][port]["ifces"][ifce_name]
                        })

                    port_entry = {
                        "urn": port_urn,
                        "ifces": ifces,
                        "tags": tags,
                        "capabilities": ["MPLS"],
                        "reservableIngressBw": mbps,
                        "reservableEgressBw": mbps
                    }
                    oscars_device["ports"].append(port_entry)

    return


def gather_edge_ports(netbeam_saps=None, netbeam_interfaces=None,
                      esdb_ports_by_rtr=None, oscars_devices=None):
    """
    this function is the worst
    """
    saps_by_device = get_saps_by_device(netbeam_saps)
    oscars_device_urns = []
    for device in oscars_devices:
        oscars_device_urns.append(device['urn'])

    from_saps = 0
    from_netbeam = 0
    new_from_netbeam = 0
    from_esdb = 0
    new_from_esdb = 0
    candidate_ports = {}
    intracloud_ifces = []

    # SAPs are always customer facing
    for router in saps_by_device:
        saps = saps_by_device[router]
        for sap in saps:
            port_urn = router + ":" + sap['port']
            if port_urn not in candidate_ports:
                tags = []
                if 'description' in sap and sap['description'] is not None and sap['description'] != "":
                    tags.append(sap['description'])
                from_saps += 1
                candidate_ports[port_urn] = {
                    "port": sap['port'],
                    "oscars": False,
                    "sap": True,
                    "interface": False,
                    "device": router,
                    "tags": tags,
                    "speed": None,
                    "source": "saps",
                    "sites": []
                }
            entry = candidate_ports[port_urn]
            if sap['oscars']:
                entry["oscars"] = True
            if 'site' in sap and sap['site'] != "":
                if sap['site'] not in entry["sites"]:
                    entry["sites"].append(sap['site'])
            candidate_ports[port_urn] = entry

    # Netbeam interfaces include
    # - Nokia ports
    #    - May have any SAPs, or not. We already know the ones that do have SAPs.
    #    - We will keep any others that do not have the default interface description.
    # - Juniper interfaces
    #    - which may be only backbone, only edge, or mixed. We want to skip the only-backbone ones.
    #    - they show up multiple times, once for the base port and each time for a sub-ifce
    # first pass we decide whether to keep interfaces around for a second pass
    sub_interfaces = {}
    keep_decisions = {}
    oscars_desc_ifces = []
    for ifce in netbeam_interfaces:
        ifce_name = ifce["name"]
        ifce_urn = ifce['device'] + ":" + ifce["name"]

        keep = "No"
        ifce_type = "juniper"
        if ifce_name[0].isdigit():
            ifce_type = "nokia"
            if "Gig Ethernet" in ifce["description"]:
                keep = "No"
            elif "sts192" in ifce_name:
                keep = "No"
            else:
                if ifce_urn not in keep_decisions:
                    keep = "Yes"
                else:
                    keep = keep_decisions[ifce_urn]

        if ifce_name.startswith('lag'):
            ifce_type = "nokia"
            keep = "Yes"

        if ifce["nokiaType"] == "network":
            ifce_type = "nokia"
            keep = "No"
            parent_port_urn = ifce['device'] + ":" + ifce["port"]
            keep_decisions[parent_port_urn] = "No"

        if ifce_name.startswith('ge'):
            keep = "Yes"
            ifce_type = "juniper"
        if ifce_name.startswith('xe'):
            keep = "Yes"
            ifce_type = "juniper"
        if ifce_name.startswith('ae'):
            keep = "Yes"
            ifce_type = "juniper"
        if ifce_name.startswith('et'):
            keep = "Yes"
            ifce_type = "juniper"
        if '32767' in ifce_name:
            keep = "No"

        if is_nokia_breakout(ifce_name):
            keep = "No"

        if ifce["device"] not in oscars_device_urns:
            keep = "No"

        if "OSCARS" in ifce["description"]:
            if ifce_urn not in oscars_desc_ifces:
                oscars_desc_ifces.append(ifce_urn)

        ifce_urn = ifce["device"] + ":" + ifce["name"]
        if ifce["intracloud"]:
            if ifce_urn not in intracloud_ifces:
                intracloud_ifces.append(ifce_urn)

        if ifce_type == "juniper":
            if keep == "Yes":
                if "." in ifce_name:
                    # we don't want to keep the sub-interface
                    keep_decisions[ifce_urn] = "No"
                    parts = ifce_name.split('.')
                    port = parts[0]
                    port_urn = ifce['device'] + ":" + port
                    if port_urn not in sub_interfaces:
                        sub_interfaces[port_urn] = []
                    sub_interfaces[port_urn].append(ifce)
                else:
                    # we might want to keep the base interface, if it's not exclusively used for intracloud
                    keep_decisions[ifce_urn] = "Maybe"
                    port = ifce_name
                    port_urn = ifce['device'] + ":" + port
                    if port_urn not in sub_interfaces:
                        sub_interfaces[port_urn] = []
            else:
                keep_decisions[ifce_urn] = "No"
        else:
            keep_decisions[ifce_urn] = keep

    # second pass through
    for ifce in netbeam_interfaces:
        ifce_name = ifce["name"]
        ifce_urn = ifce["device"] + ":" + ifce["name"]
        keep = keep_decisions[ifce_urn]

        if keep == "Maybe":
            keep = "No"
            port = ifce_name
            port_urn = ifce['device'] + ":" + port
            all_intracloud = True

            for sub_ifce in sub_interfaces[port_urn]:
                if sub_ifce['intracloud']:
                    sub_ifce_urn = sub_ifce["device"] + ":" + sub_ifce["name"]
                    if sub_ifce_urn not in intracloud_ifces:
                        intracloud_ifces.append(sub_ifce_urn)
                else:
                    all_intracloud = False

                if sub_ifce['intercloud']:
                    all_intracloud = False

            if not all_intracloud:
                keep = "Yes"

        if keep == "Yes":
            from_netbeam += 1

            port_urn = ifce["device"] + ":" + ifce_name
            if port_urn not in candidate_ports:
                new_from_netbeam += 1
                candidate_ports[port_urn] = {
                    "port": ifce_name,
                    "oscars": False,
                    "sap": False,
                    "interface": True,
                    "source": "netbeam",
                    "speed": ifce["speed"],
                    "device": ifce["device"],
                    "tags": [],
                    "sites": []
                }
            entry = candidate_ports[port_urn]
            entry["interface"] = True
            if ifce['oscars']:
                entry["oscars"] = True

            if ifce_urn in oscars_desc_ifces:
                entry["oscars"] = True

            if 'speed' in ifce and ifce['speed'] is not None and ifce['speed'] != "":
                entry["speed"] = ifce['speed']

            oscars_tags = [ifce['description']]
            for oscars_tag in oscars_tags:
                if oscars_tag != "" and oscars_tag not in entry['tags']:
                    entry["tags"].append(oscars_tag)

            if 'site' in ifce and ifce['site'] != "":
                if ifce['site'] not in entry["sites"]:
                    entry["sites"].append(ifce['site'])
            candidate_ports[port_urn] = entry

    for router in esdb_ports_by_rtr:
        if router in oscars_device_urns:
            for port in esdb_ports_by_rtr[router]:
                if port == "BLANK":
                    continue
                port_ifces = esdb_ports_by_rtr[router][port]

                all_intracloud = True
                speed = None
                oscars_tags = []
                for ifce_data in port_ifces:
                    if ifce_data["alias"] not in oscars_tags:
                        oscars_tags.append(ifce_data["alias"])
                    speed = ifce_data["mbps"]
                    ifce_urn = router + ":" + ifce_data["int_name"]
                    if ifce_urn not in intracloud_ifces:
                        all_intracloud = False

                port_urn = router + ":" + port
                if port_urn in candidate_ports:
                    entry = candidate_ports[port_urn]
                    for oscars_tag in oscars_tags:
                        if oscars_tag not in entry['tags']:
                            entry["tags"].append(oscars_tag)
                    if entry['speed'] is None:
                        entry['speed'] = speed
                        candidate_ports[port_urn] = entry

                if not all_intracloud:
                    from_esdb += 1
                    if port_urn not in candidate_ports:
                        new_from_esdb += 1
                        candidate_ports[port_urn] = {
                            "port": port,
                            "oscars": False,
                            "sap": False,
                            "source": "esdb",
                            "interface": True,
                            "device": router,
                            "speed": speed,
                            "tags": oscars_tags,
                            "sites": []
                        }
                    entry = candidate_ports[port_urn]
                    entry["interface"] = True

                    candidate_ports[port_urn] = entry
    #    print "saps: {} netbeam: {} ({} new) esdb: {} ({} new)"\
    #        .format(from_saps, from_netbeam, new_from_netbeam, from_esdb, new_from_esdb)

    return list(candidate_ports.values())


def gather_used_vlans(netbeam_saps=None, netbeam_interfaces=None, esdb_ports_by_rtr=None, esdb_vlans=None):
    saps_by_device = get_saps_by_device(netbeam_saps)
    used_vlans = {}
    oscars_vlans = {}

    from_saps = 0
    from_netbeam = 0
    new_from_netbeam = 0
    from_esdb = 0
    new_from_esdb = 0

    for router in saps_by_device:
        saps = saps_by_device[router]
        for sap in saps:
            port_urn = router + ":" + sap['port']
            vlan = int(sap['vlan'])
            from_saps += 1
            if port_urn not in used_vlans:
                used_vlans[port_urn] = []
            if vlan not in used_vlans[port_urn]:
                used_vlans[port_urn].append(vlan)
            if sap['oscars']:
                if port_urn not in oscars_vlans:
                    oscars_vlans[port_urn] = []
                oscars_vlans[port_urn].append({
                    "vlan": vlan,
                    "service": sap["service"],
                    "connection": sap["connection"]
                })

    for ifce in netbeam_interfaces:
        if ifce["port"] is not None and not ifce["port"].startswith("virtual"):
            port_urn = ifce["device"] + ":" + ifce["port"]
            vlan = int(ifce["vlan"])
            if vlan is not None:
                from_netbeam += 1
                if port_urn not in used_vlans:
                    new_from_netbeam += 1
                    used_vlans[port_urn] = []
                if vlan not in used_vlans[port_urn]:
                    used_vlans[port_urn].append(vlan)

        if ifce["port"] is None:
            ifce_name = ifce["name"]
            if ifce_name.startswith('ge') or ifce_name.startswith('xe') or \
                    ifce_name.startswith('ae') or ifce_name.startswith('et'):
                if "." in ifce_name and '32767' not in ifce_name:
                    parts = ifce_name.split('.')
                    port = parts[0]
                    vlan = int(parts[1])
                    port_urn = ifce["device"] + ":" + port
                    from_netbeam += 1
                    if port_urn not in used_vlans:
                        new_from_netbeam += 1
                        used_vlans[port_urn] = []

                    if vlan not in used_vlans[port_urn]:
                        used_vlans[port_urn].append(vlan)

    for router in esdb_ports_by_rtr:
        for port in esdb_ports_by_rtr[router]:
            port_urn = router + ":" + port
            port_ifces = esdb_ports_by_rtr[router][port]
            for ifce_entry in port_ifces:
                vlan = find_vlan(router, ifce_entry["int_name"], netbeam_interfaces, esdb_vlans)
                if vlan is not None:
                    from_esdb += 1
                    if port_urn not in used_vlans:
                        used_vlans[port_urn] = []
                        new_from_esdb += 1
                    if vlan not in used_vlans[port_urn]:
                        used_vlans[port_urn].append(vlan)

    #    print "saps: {} netbeam: {} ({} new) esdb: {} ({} new)"\
    #        .format(from_saps, from_netbeam, new_from_netbeam, from_esdb, new_from_esdb)
    return used_vlans, oscars_vlans


def find_vlan(router, ifce_name, netbeam_interfaces, esdb_vlans):
    netbeam_vlan = None
    for entry in netbeam_interfaces:
        if entry["device"] == router and entry["name"] == ifce_name:
            if netbeam_vlan is None:
                netbeam_vlan = entry["vlan"]
            else:
                eprint("multiple netbeam vlans found for {} -- {}".format(router, ifce_name))
    esdb_vlan = None
    for entry in esdb_vlans:
        if entry["router"] == router and entry["int_name"] == ifce_name:
            if esdb_vlan is None:
                esdb_vlan = entry["vlan_id"]
            else:
                eprint("multiple esdb vlans found for {} -- {}".format(router, ifce_name))
    ifce_name_vlan = None
    parts = ifce_name.split(".")
    if len(parts) == 2:
        ifce_name_vlan = int(parts[1])

    if netbeam_vlan is not None and esdb_vlan is not None:
        if netbeam_vlan != esdb_vlan:
            eprint("Netbeam vlan ({}) differs from ESDB ({}) for {} -- {}"
                   .format(netbeam_vlan, esdb_vlan, router, ifce_name))

    if netbeam_vlan is not None and ifce_name_vlan is not None:
        if netbeam_vlan != ifce_name_vlan:
            eprint("Netbeam vlan ({}) differs from ifce name VLAN ({}) for {} -- {}"
                   .format(netbeam_vlan, ifce_name_vlan, router, ifce_name))

    if esdb_vlan is not None and ifce_name_vlan is not None:
        if esdb_vlan != ifce_name_vlan:
            eprint("ESDB vlan ({}) differs from ifce name VLAN ({}) for {} -- {}"
                   .format(esdb_vlan, ifce_name_vlan, router, ifce_name))
    if netbeam_vlan is not None:
        return int(netbeam_vlan)
    elif esdb_vlan is not None:
        return int(esdb_vlan)
    else:
        return ifce_name_vlan


def is_nokia_breakout(ifce_name):
    parts = ifce_name.split('/')
    if len(parts) != 3:
        return False
    if 'c' in parts[2]:
        return True

def validate_oscars_adjacencies(oscars_adjcies=None):
    passes_validation = True
    for oscars_adjcy in oscars_adjcies:
        a = oscars_adjcy["a"]
        z = oscars_adjcy["z"]
        found_inverse = False
        for maybe_inverse in oscars_adjcies:
            if maybe_inverse["a"] == z and maybe_inverse["z"] == a:
                found_inverse = True
        if not found_inverse:
            passes_validation = False
            eprint("No inverse adjacency for {} -- {}".format(a, z))

    if not passes_validation:
        sys.exit(1)


def repair_blank_isis_ports(isis_adjcies=None, netbeam_interfaces=None, settings=None):
    for isis_adjcy in isis_adjcies:
        a_needed_repair = False
        a_repaired = False
        z_needed_repair = False
        z_repaired = False

        if "a_port" not in isis_adjcy:
            a_needed_repair = True
            a_ifce = isis_adjcy["a_ifce"]
            if a_ifce.startswith('ge') or a_ifce.startswith('xe') or \
                    a_ifce.startswith('ae') or a_ifce.startswith('et'):
                if "." in a_ifce and '32767' not in a_ifce:
                    parts = a_ifce.split('.')
                    isis_adjcy["a_port"] = parts[0]
                    a_repaired = True
                    if settings['VERBOSE']:
                        print("repaired adjcy for {}:{}".format(isis_adjcy["a"], isis_adjcy["a_port"]))

        if isis_adjcy["a_port"] == "BLANK":
            a_needed_repair = True
            for ifce in netbeam_interfaces:
                if isis_adjcy["a"] == ifce["device"] and ifce["name"] == isis_adjcy["a_ifce"]:
                    isis_adjcy["a_port"] = ifce["port"]
                    a_repaired = True
                    if settings['VERBOSE']:
                        print("repaired adjcy for {}:{}".format(isis_adjcy["a"], isis_adjcy["a_port"]))

        if "z_port" not in isis_adjcy:
            z_needed_repair = True
            z_ifce = isis_adjcy["z_ifce"]
            if z_ifce.startswith('ge') or z_ifce.startswith('xe') or \
                    z_ifce.startswith('ae') or z_ifce.startswith('et'):
                if "." in z_ifce and '32767' not in z_ifce:
                    parts = z_ifce.split('.')
                    isis_adjcy["z_port"] = parts[0]
                    z_repaired = True
                    if settings['VERBOSE']:
                        print("repaired adjcy for {}:{}".format(isis_adjcy["z"], isis_adjcy["z_port"]))

        if isis_adjcy["z_port"] == "BLANK":
            z_needed_repair = True
            for ifce in netbeam_interfaces:
                if isis_adjcy["z"] == ifce["device"] and ifce["name"] == isis_adjcy["z_ifce"]:
                    isis_adjcy["z_port"] = ifce["port"]
                    z_repaired = True
                    if settings['VERBOSE']:
                        print("repaired adjcy for {}:{}".format(isis_adjcy["z"], isis_adjcy["z_port"]))

        if a_needed_repair or z_needed_repair:
            repaired_all = False
            if a_needed_repair and z_needed_repair:
                repaired_all = a_repaired and z_repaired
            elif a_needed_repair:
                repaired_all = a_repaired
            elif z_needed_repair:
                repaired_all = z_repaired
            if not repaired_all:
                print("unrepairable adjacency - see below:")
                pp.pprint(isis_adjcy)


def to_oscars_adjcies(isis_adjcies=None):
    urn_map = isis_urns_by_addr(isis_adjcies=isis_adjcies)

    oscars_adjcies = []
    igp_portmap = {}

    for isis_adjcy in isis_adjcies:
        a_router = isis_adjcy["a"]

        a_port = isis_adjcy["a_port"]
        a_addr = isis_adjcy["a_addr"]
        a_urn = urn_map[a_addr]["ifce_urn"]

        z_router = isis_adjcy["z"]
        z_port = isis_adjcy["z_port"]
        z_addr = isis_adjcy["z_addr"]
        z_urn = urn_map[z_addr]["ifce_urn"]

        if a_urn is not None and z_urn is not None:

            oscars_adjcy = {
                "a": {
                    "device": a_router,
                    "port": "{}:{}".format(a_router, a_port),
                    "addr": a_addr,
                    "ifce": a_urn,
                },
                "z": {
                    "device": z_router,
                    "port": "{}:{}".format(z_router, z_port),
                    "addr": z_addr,
                    "ifce": z_urn,
                },
                "metrics": {
                    "MPLS": isis_adjcy["latency"]
                }
            }
            oscars_adjcies.append(oscars_adjcy)

            if a_router not in igp_portmap:
                igp_portmap[a_router] = {}

            if a_port not in igp_portmap[a_router]:
                igp_portmap[a_router][a_port] = {
                    "mbps": isis_adjcy["mbps"],
                    "ifces": {}
                }
            igp_portmap[a_router][a_port]["ifces"][isis_adjcy["a_ifce"]] = isis_adjcy["a_addr"]

    #            if len(igp_portmap[a_router][a_port]["ifces"]) > 1:
    #                print >> sys.stderr, "Multiple IGP interfaces on: {}:{}".format(a_router, a_port)

    return oscars_adjcies, igp_portmap


def get_saps_by_device(saps_in):
    saps_by_device = {}
    for sap_in in saps_in:
        device = sap_in['device']
        if device not in saps_by_device:
            saps_by_device[device] = []

        sap_in['name'] = sap_in['name'].replace('lag-', 'lag#')

        parts = sap_in['name'].split('-')
        port = parts[1].replace('_', '/')
        port = port.replace('#', '-')

        entry = {
            'service': parts[0],
            'port': port,
            'vlan': parts[2],
            'oscars': sap_in['oscars'],
            'site': sap_in['site'],
            'connection': sap_in['connection'],
            'description': sap_in['description']
        }
        saps_by_device[device].append(entry)
    return saps_by_device


def isis_urns_by_addr(isis_adjcies=None):
    result = {}

    for isis_adjcy in isis_adjcies:
        router_a = isis_adjcy["a"]
        port_a = isis_adjcy["a_port"]
        addr_a = isis_adjcy["a_addr"]

        port_urn_a = router_a + ":" + port_a

        ifce_urn_a = None
        if "a_ifce" in isis_adjcy:
            ifce_urn_a = router_a + ":" + isis_adjcy["a_ifce"]

        entry = {
            "port_urn": port_urn_a,
            "ifce_urn": ifce_urn_a
        }
        result[addr_a] = entry

    return result


def model_map(operating_system=None, description=None):
    if description == "Alcatel" or description == "Nokia":
        return "ALCATEL_SR7750"
    elif description == "Juniper":
        model = "JUNIPER_MX"
        # parts = str(operating_system).split(" ")
        # model = "JUNIPER_" + str(parts[0]).upper()
        return model
    else:
        raise ValueError("could not decide router model for [%s] [%s]" % (os, description))


def set_to_ranges(vlan_set=None):
    ranges = []
    for k, g in groupby(enumerate(vlan_set), lambda p: p[0] - p[1]):
        group = list(map(itemgetter(1), g))
        ranges.append((group[0], group[-1]))

    result = []
    for entry in ranges:
        result.append({
            "floor": int(entry[0]),
            "ceiling": int(entry[1])
        })
    return result


def make_reservable_vlans(used_vlans=None, oscars_vlans=None, config=None):
    used_vlan_set = set(used_vlans)
    for entry in oscars_vlans:
        if entry['vlan'] in used_vlan_set:
            used_vlan_set.remove(entry['vlan'])

    default_vlans_cfg = config['VARS']['vlan-ranges']['DEFAULT']
    default_vlans = set()

    for vlan_range in default_vlans_cfg:
        floor = vlan_range['floor']
        ceiling = vlan_range['ceiling']
        for i in range(floor, ceiling + 1):
            default_vlans.add(i)

    avail_vlans = default_vlans - used_vlan_set
    avail_vlans = list(avail_vlans)
    avail_vlans.sort()

    if avail_vlans is not None:
        return set_to_ranges(avail_vlans)

    else:
        print("could not decide available vlans!")


def to_oscars_devices(in_devices=None, datasets=None, igp_portmap=None, addrs=None, settings=None, config=None):
    equipment = datasets['EQUIPMENT']
    locations = datasets['LOCATIONS']

    locs_by_id = {}
    equip_by_name = {}
    for entry in equipment['results']:
        equip_by_name[entry['name']] = entry
    for entry in locations['results']:
        locs_by_id[entry['id']] = entry

    loopback_nets = config['VARS']['loopback-nets']

    out_routers = []
    for rs in in_devices:
        if rs["name"] not in equip_by_name:
            eprint("device not found in ESDB equipment: " + rs["name"])
            exit(1)
        if rs["name"] in igp_portmap:
            loopback = None
            for addr in addrs:
                int_name = addr["int_name"]
                address = addr["address"]
                router = addr["router"]
                if int_name == "lo0.0" or int_name == "system":
                    for net in loopback_nets:
                        if address.startswith(net) and router == rs["name"]:
                            if loopback is None:
                                loopback = address
                            else:
                                eprint("Multiple loopbacks for: " + rs["name"])

            if loopback is None:
                eprint("Could not find loopback for: " + rs["name"])

            equip_entry = equip_by_name[rs["name"]]
            location = locs_by_id[equip_entry['location']['id']]
            model = model_map(operating_system=rs["os"], description=rs["description"])
            out_router = {
                "urn": rs["name"],
                "model": model,
                "type": "ROUTER",
                "capabilities": ["ETHERNET", "MPLS"],
                "ports": [],
                "ipv4Address": loopback,
                "reservableVlans": [],
                "location": location["short_name"],
                "locationId": location["id"],
                "latitude": location["latitude"],
                "longitude": location["longitude"]
            }
            out_routers.append(out_router)
        else:
            if settings['VERBOSE']:
                print("did not add not-igp router " + rs["name"])

    return out_routers


def merge_overrides(in_devices=None, config=None):
    merged = []
    if len(config) == 0 or 'devices' not in config['OVERRIDES']:
        return in_devices
    device_overrides = config['OVERRIDES']['devices']

    for in_device in in_devices:
        for entry in device_overrides:
            ov_dev_urn = entry['urn']
            if in_device['urn'] == ov_dev_urn:
                in_device = merge(in_device, entry, strategy=Strategy.ADDITIVE)
        merged.append(in_device)

    return merged


def load_config(settings):
    config = {}
    for key, filename in settings['CONFIG']['FILES'].items():
        path = os.path.join(settings['CONFIG']['DIR'], filename)
        with open(path, 'r') as infile:
            config[key] = json.load(infile)
    return config


def process_options(opts, settings):
    settings['VERBOSE'] = opts.verbose

    settings['CACHE']['SAVE'] = opts.save_cache
    settings['CACHE']['USE'] = opts.use_cache
    settings['OUTPUT']['DIR'] = opts.output_dir
    ensure_dir(settings['OUTPUT']['DIR'], create=True)

    settings['CACHE']['DIR'] = opts.cache_dir
    if settings['CACHE']['SAVE'] or settings['CACHE']['USE']:
        ensure_dir(settings['CACHE']['DIR'], create=True)

    settings['CONFIG']['DIR'] = opts.config_dir
    ensure_dir(settings['CONFIG']['DIR'], create=False)


def ensure_dir(path, create=False):
    if not os.path.isdir(path):
        if not os.path.exists(path):
            if create:
                print("creating directory: {}".format(path))
                try:
                    os.makedirs(path)
                except OSError as e:
                    if e.errno != errno.EEXIST:
                        raise
            else:
                eprint("required path does not exist {}".format(path))
                exit(1)
        else:
            eprint("path exists but is not directory: {}".format(path))
            exit(1)


def load_datasets(settings):
    token = get_token(settings)
    verbose = settings['VERBOSE']
    results = {}
    for set_name, dataset in settings['DATASETS'].items():
        data = None
        source = dataset['SOURCE']
        cache_filename = dataset['CACHE_FILENAME']
        cache_path = os.path.join(settings['CACHE']['DIR'], cache_filename)

        if not settings['CACHE']['USE']:
            if verbose:
                print("retrieving {} from {}".format(cache_filename, source))
            r = None
            if source == 'ESDB':
                r = requests.get(settings['URLS']['ESDB'] + dataset['URL'],
                                 headers=dict(Authorization='Token {0}'.format(token)))
            elif source == 'NETBEAM':
                r = requests.get(settings['URLS']['NETBEAM'] + dataset['URL'])
            if r is not None:
                if r.status_code != requests.codes.ok:
                    eprint("error:  {}".format(r.text))
                    exit(1)
                data = r.json()
            else:
                eprint("could not determine data source for {}".format(set_name))
                exit(1)

        else:
            if verbose:
                print("loading {}".format(cache_path))
            with open(cache_path, 'r') as infile:
                data = json.load(fp=infile)

        if settings['CACHE']['SAVE']:
            if verbose:
                print("saving cache at {}".format(cache_path))
            with open(cache_path, 'w') as outfile:
                json.dump(obj=data, fp=outfile, indent=2)
        results[set_name] = data

    return results


def write_output(adjacencies, devices, settings):
    verbose = settings['VERBOSE']
    devices_filename = os.path.join(settings['OUTPUT']['DIR'], settings['OUTPUT']['DEVICES'])
    adjcies_filename = os.path.join(settings['OUTPUT']['DIR'], settings['OUTPUT']['ADJCIES'])
    if verbose:
        print("saving devices at {}".format(devices_filename))
    with open(devices_filename, 'w') as outfile:
        json.dump(obj=devices, fp=outfile, indent=2)
    if verbose:
        print("saving adjacencies at {}".format(adjcies_filename))
    with open(adjcies_filename, 'w') as outfile:
        json.dump(obj=adjacencies, fp=outfile, indent=2)


def get_token(settings):
    """Gets the API token contents from the configured file
    """

    token_path = os.path.join(settings['CONFIG']['DIR'], settings['CONFIG']['ESDB_TOKEN'])

    try:
        token = open(token_path, "r").read().strip()
    except IOError as e:
        raise Exception("{}: unable to get token: {}".format(sys.argv[0], e))

    return token


def eprint(*args, **kwargs):
    print(*args, file=sys.stderr, **kwargs)


if __name__ == '__main__':
    main()
