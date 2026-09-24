#!/usr/bin/env python3
"""
search.py - Search OSCARS connections by a string.

Fetches the connection list from https://oscars.es.net/api/conn/simplelist
and prints every connection whose JSON representation contains the given string.

Usage:
    ./search.py [-v] <search_term>
    python3 search.py [-v] <search_term>

Options:
    -v    Verbose: show full connection details instead of a one-line summary.
"""

import json
import sys
import urllib.request
from datetime import datetime, timezone

API_URL = "https://oscars.es.net/api/conn/simplelist"


def fetch_connections(url: str) -> list:
    with urllib.request.urlopen(url) as response:
        return json.loads(response.read().decode())


def ts_to_str(ts) -> str:
    """Convert a Unix timestamp to a human-readable UTC string, or '-' if absent."""
    if ts is None:
        return "-"
    try:
        return datetime.fromtimestamp(int(ts), tz=timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    except (ValueError, TypeError, OSError):
        return str(ts)


def fmt_bw(mbps) -> str:
    """Format a bandwidth value, converting to Gbps if >= 10000 Mbps."""
    try:
        mbps = int(mbps)
    except (ValueError, TypeError):
        return f"{mbps} Mbps"
    if mbps >= 10000:
        gbps = mbps / 1000
        value = int(gbps) if gbps == int(gbps) else gbps
        return f"{value} Gbps"
    return f"{mbps} Mbps"


def connection_matches(conn: dict, needle: str) -> bool:
    """Return True if the needle appears anywhere in the JSON of the connection."""
    return needle.lower() in json.dumps(conn).lower()


def print_connection_short(conn: dict) -> None:
    cid         = conn.get("connectionId", "?")
    description = conn.get("description", "") or "(none)"
    print(f"  {cid} : {description}")


def parse_ero(ero: list) -> list:
    """
    Parse an ERO hop list into a list of (in_port, device, out_port) tuples.
    in_port and out_port are None when absent (first/last device).
    """
    tuples = []
    i = 0
    while i < len(ero):
        hop = ero[i]
        if ":" not in hop:  # it's a device
            in_port  = ero[i - 1] if i > 0 else None
            out_port = ero[i + 1] if i + 1 < len(ero) else None
            tuples.append((in_port, hop, out_port))
        i += 1
    return tuples


def print_connection_verbose(conn: dict) -> None:
    cid         = conn.get("connectionId", "?")
    description = conn.get("description", "")
    username    = conn.get("username", "?")
    phase       = conn.get("phase", "?")
    state       = conn.get("state", "?")
    mode        = conn.get("mode", "?")
    begin       = ts_to_str(conn.get("begin"))
    end         = ts_to_str(conn.get("end"))
    mtu         = conn.get("connection_mtu", "?")

    junctions = [j.get("device", "?") for j in conn.get("junctions", [])]
    fixtures   = conn.get("fixtures", [])
    pipes      = conn.get("pipes", [])
    tags       = [f"{t.get('category','?')}:{t.get('contents','')}" for t in conn.get("tags", [])]

    print(f"{'─' * 60}")
    print(f"  Connection ID : {cid}")
    print(f"  Description   : {description or '(none)'}")
    print(f"  User          : {username}")
    print(f"  Phase/State   : {phase} / {state}")
    print(f"  MTU           : {mtu}")
    print(f"  Begin         : {begin}")
    print(f"  End           : {end}")
    print(f"  Junctions     : {', '.join(junctions) or '(none)'}")

    if fixtures:
        print(f"  Fixtures      :")
        max_port = max(len(f.get("port", "?")) for f in fixtures)
        for f in fixtures:
            port = f.get("port", "?")
            vlan = f.get("vlan", "?")
            inm  = f.get("inMbps", "?")
            outm = f.get("outMbps", "?")
            bw   = f"bw={fmt_bw(inm)}" if inm == outm else f"in={fmt_bw(inm)}\tout={fmt_bw(outm)}"
            print(f"                  {port:<{max_port}}\tvlan={vlan}\t{bw}")

    if pipes:
        print(f"  Pipes         :")
        for p in pipes:
            a       = p.get("a", "?")
            z       = p.get("z", "?")
            az      = p.get("azMbps", "?")
            za      = p.get("zaMbps", "?")
            protect = "protected" if p.get("protect") else "unprotected"
            bw      = f"bw={fmt_bw(az)}" if az == za else f"az={fmt_bw(az)}  za={fmt_bw(za)}"
            print(f"                  {a} <-> {z}  {bw}  [{protect}]")
            ero = p.get("ero", [])
            if ero:
                tuples = parse_ero(ero)
                # "ERO:" aligns with "Pipes         :" — label is 16 chars wide
                ero_label = "  ERO:          "
                cont_pad  = " " * len(ero_label)
                max_in  = max(len(t[0]) if t[0] else 0 for t in tuples)
                max_dev = max(len(t[1]) for t in tuples)
                print(ero_label)
                for in_port, device, out_port in tuples:
                    in_str = f"{in_port:<{max_in}} - " if in_port else " " * (max_in + 3)
                    suffix = f" - {out_port}" if out_port else ""
                    print(f"{cont_pad}-> {in_str}{device:<{max_dev}}{suffix}")

    if tags:
        print(f"  Tags          : {', '.join(tags)}")


def main():
    args = sys.argv[1:]

    if not args or args[0] in ("-h", "--help"):
        print(f"Usage: {sys.argv[0]} [-v] <search_term>")
        sys.exit(0 if args and args[0] in ("-h", "--help") else 1)

    verbose = False
    if args[0] == "-v":
        verbose = True
        args = args[1:]

    if not args:
        print(f"Usage: {sys.argv[0]} [-v] <search_term>")
        sys.exit(1)

    needle = args[0]

    print(f"Fetching connections from {API_URL} ...", file=sys.stderr)
    try:
        connections = fetch_connections(API_URL)
    except Exception as e:
        print(f"Error fetching data: {e}", file=sys.stderr)
        sys.exit(1)

    matches = [c for c in connections if connection_matches(c, needle)]

    if not matches:
        print(f"No connections found matching '{needle}'.")
        sys.exit(0)

    print(f"Found {len(matches)} connection(s) matching '{needle}':\n", file=sys.stderr)

    if verbose:
        for conn in matches:
            print_connection_verbose(conn)
        print(f"{'─' * 60}")
    else:
        for conn in matches:
            print_connection_short(conn)

    print(f"\nTotal: {len(matches)} match(es) for '{needle}'.")


if __name__ == "__main__":
    main()
