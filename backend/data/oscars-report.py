#!/usr/bin/env python3
import psycopg2

# Connect to the database
conn = psycopg2.connect(
    database="oscars",
    user='oscars',
    password='<password>',
    host='<ip-address>',
    port='5432'
)

# Create a cursor
cur = conn.cursor()

conns_query = """SELECT connection_id, description, username FROM connection WHERE

(
    (connection.connection_id IN (SELECT DISTINCT connection_id FROM router_command_history WHERE date > '2024-10-01'))
    OR
    connection.phase = 1
)
AND username = 'nsi';
"""

cur.execute(conns_query)
rows = cur.fetchall()
print("connection_id,description,username,port_a,vlan_a,port_z,vlan_z")
for row in rows:
    connection_id, description, username = row
    vf_query = f"""
SELECT
   vlan_fixture.port_urn AS port_urn, vlan.vlan_id AS vlan
FROM vlan_fixture LEFT JOIN vlan ON vlan_fixture.vlan_id = vlan.id
WHERE vlan_fixture.connection_id = '{connection_id}';
"""
    cur.execute(vf_query)
    vf_rows = cur.fetchall()
    port_vlan_set = set()
    for vf_row in vf_rows:
        port_urn, vlan = vf_row
        port_vlan_set.add(f"{port_urn},{vlan}")
    port_vlans = ",".join(port_vlan_set)
    print(f"{connection_id},{description},{username},{port_vlans}")
# Close the cursor and the connection
cur.close()
conn.close()

