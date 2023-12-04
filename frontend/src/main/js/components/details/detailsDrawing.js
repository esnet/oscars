import React, { Component } from "react";
import { inject, observer } from "mobx-react";
import { autorun, toJS } from "mobx";
import { Card, CardBody, CardHeader } from "reactstrap";
import { DataSet, Network } from "visjs-network/dist/vis-network.min.js";
import VisUtils from "../../lib/vis";
import HelpPopover from "../helpPopover";
import { size } from "lodash-es";

@inject("connsStore", "modalStore", "mapStore", "topologyStore")
@observer
class DetailsDrawing extends Component {
    constructor(props) {
        super(props);

        let nodeDataset = new DataSet();
        let edgeDataset = new DataSet();
        this.datasource = {
            nodes: nodeDataset,
            edges: edgeDataset
        };
    }

    onFixtureClicked = fixture => {
        this.props.connsStore.setSelected({
            type: "fixture",
            data: fixture
        });
    };

    onJunctionClicked = junction => {
        this.props.connsStore.setSelected({
            type: "junction",
            data: junction
        });
    };

    onPipeClicked = pipe => {
        this.props.connsStore.setSelected({
            type: "pipe",
            data: pipe
        });
    };

    network = {
        fit: () => {
            // console.log('default fit')
        },
        stabilize: () => {
            // console.log('default stab')
        }
    };

    componentDidMount() {
        if (size(this.props.mapStore.positions) === 0) {
            this.props.mapStore.loadPositions();
        }
        if (size(this.props.topologyStore.positions) === 0) {
            this.props.topologyStore.loadAdjacencies();
        }
        let options = {
            height: "300px",
            interaction: {
                hover: false,
                navigationButtons: false,
                zoomView: true,
                dragView: true
            },
            physics: {
                solver: "barnesHut",
                stabilization: {
                    fit: true
                },
                barnesHut: {
                    centralGravity: 0.5
                }
            },
            nodes: {
                shape: "dot",
                color: { background: "white" }
            }
        };

        const schematicId = document.getElementById("schematicDrawing");

        this.network = new Network(schematicId, this.datasource, options);

        this.network.on("dragEnd", params => {
            if (params.nodes.length > 0) {
                let nodeId = params.nodes[0];
                this.datasource.nodes.update({ id: nodeId, fixed: { x: true, y: true } });
            }
        });

        this.network.on("dragStart", params => {
            if (params.nodes.length > 0) {
                let nodeId = params.nodes[0];
                this.datasource.nodes.update({ id: nodeId, fixed: { x: false, y: false } });
            }
        });

        this.network.on("click", params => {
            if (params.nodes.length > 0) {
                let nodeId = params.nodes[0];
                let nodeEntry = this.datasource.nodes.get(nodeId);
                if (nodeEntry.onClick !== null) {
                    nodeEntry.onClick(nodeEntry.data);
                }
            }
            if (params.edges.length > 0) {
                let edgeId = params.edges[0];
                let edgeEntry = this.datasource.edges.get(edgeId);

                if (edgeEntry.onClick !== null) {
                    edgeEntry.onClick(edgeEntry.data);
                }
            }
        });
    }

    componentWillUnmount() {
        this.disposeOfMapUpdate();
        this.disposeOfRedraw();
    }

    disposeOfRedraw = autorun(() => {
        if (this.props.connsStore.drawing.redraw === true) {
            // console.log('redrawing');
            this.props.connsStore.setRedraw(false);
            this.network.stabilize(50);
            this.network.fit({ animation: false });
            // console.log('done redrawing');
        }
    });

    // this automagically updates the map;
    disposeOfMapUpdate = autorun(
        () => {
            if (typeof(this.props.connsStore.store.current) === "undefined") {
                console.log('undefined store.current');
                return;
            } else if (typeof(this.props.connsStore.store.current.archived) === "undefined") {
                console.log('undefined store.current.archived');
                return;
            } else if (typeof(this.props.connsStore.store.current.archived.cmp) === "undefined") {
                console.log('undefined store.current.archived.cmp');
                return;
            }

            let design = this.props.connsStore.store.current.archived.cmp;
            let junctions = toJS(design.junctions);
            let fixtures = toJS(design.fixtures);
            let pipes = toJS(design.pipes);
            let positions = this.props.mapStore.positions;

            let nodes = [];
            let edges = [];
            let nodeIds = [];
            let detailsDevices = [];
            if (size(junctions) === 0) {
                return;
            }

            junctions.map(j => {
                let junctionNode = {
                    id: j.deviceUrn,
                    label: j.deviceUrn,
                    size: 20,
                    data: j,
                    onClick: this.onJunctionClicked
                };
                if (j.deviceUrn in positions) {
                    junctionNode.x = positions[j.deviceUrn].x * 0.4;
                    junctionNode.y = positions[j.deviceUrn].y * 0.4;
                    junctionNode.fixed = { x: true, y: true };
                    junctionNode.physics = false;
                }

                detailsDevices.push(j.deviceUrn);
                nodes.push(junctionNode);
                nodeIds.push(junctionNode.id);
            });
            fixtures.map(f => {
                // console.log(toJS(f));
                const label = f.portUrn.replace(f.junction + ":", "") + ":" + f.vlan.vlanId;
                let fixtureNode = {
                    id: f.portUrn + ":" + f.vlan.vlanId,
                    label: label,
                    shape: "hexagon",
                    color: {
                        background: "lightblue",
                        inherit: false
                    },

                    size: 12,
                    data: f,

                    onClick: this.onFixtureClicked
                };
                nodeIds.push(fixtureNode.id);

                nodes.push(fixtureNode);
                let edge = {
                    id: f.portUrn + ":" + f.vlan.vlanId,
                    from: f.junction,
                    to: f.portUrn + ":" + f.vlan.vlanId,
                    length: 0.4,
                    width: 2,
                    onClick: null
                };
                edges.push(edge);
            });
            if (typeof pipes !== "undefined") {
                const colors = ["red", "orange", "green", "orange", "cyan", "brown", "pink"];

                pipes.map((p, pipe_idx) => {
                    let i = 0;
                    while (i < p.azERO.length - 1) {
                        let a = p.azERO[i]["urn"];
                        let b = p.azERO[i + 1]["urn"];
                        let y = p.azERO[i + 2]["urn"];
                        let z = p.azERO[i + 3]["urn"];

                        let foundZ = false;
                        nodes.map(node => {
                            if (node.id === z) {
                                foundZ = true;
                            }
                        });
                        if (!foundZ) {
                            let zNode = {
                                id: z,
                                label: z,
                                shape: "diamond",
                                size: 12,
                                color: colors[pipe_idx],
                                onClick: null
                            };
                            if (z in positions) {
                                zNode.x = positions[z].x * 0.4;
                                zNode.y = positions[z].y * 0.4;
                                zNode.fixed = { x: true, y: true };
                            }

                            nodes.push(zNode);
                        }
                        let edge = {
                            id: pipe_idx + " : " + b + " --- " + y,
                            from: a,
                            color: {
                                inherit: false,
                                color: colors[pipe_idx]
                            },
                            to: z,
                            width: 3,
                            length: 12,
                            onClick: null
                        };
                        edges.push(edge);
                        detailsDevices.push(a);
                        detailsDevices.push(z);

                        i = i + 3;
                    }
                });
            }
            //        console.log(edges);
            /*
        let addedEdges = [];
        let addedNodes = [];
        for (let adjcy of this.props.topologyStore.adjacencies) {
            const addAz = (detailsDevices.includes(adjcy.a) && !detailsDevices.includes(adjcy.z));
            const addZa = (detailsDevices.includes(adjcy.z) && !detailsDevices.includes(adjcy.a));
            let a = null;
            let z = null;

            if (addAz) {
                a = adjcy.a;
                z = adjcy.z;

            }
            if (addZa) {
                a = adjcy.z;
                z = adjcy.a;
            }
            if (addAz || addZa) {
                let edgeId = a+ ' --- ' + z;
                if (addedEdges.includes(edgeId)) {
                    continue;
                }
                let zNode = {
                    id: z,
                    label: z,
                    font: {
                        size: 9
                    },
                    size: 5,
                    shape: 'dot',
                    onClick: null,
                };
                if (z in positions) {
                    zNode.x = positions[z].x * 0.4;
                    zNode.y = positions[z].y * 0.4;
                    zNode.fixed = {x: true, y: true}
                }
                if (!addedNodes.includes(z)) {
                    nodes.push(zNode);
                    addedNodes.push(z);
                }

                let edge = {
                    id: edgeId,
                    from: a,
                    to: z,
                    width: 0.5,
                    length: 10,
                    color: {
                        inherit: false,
                        color: 'blue',
                        opacity: 0.5
                    },

                    dashes: true,

                    onClick: null
                };
                edges.push(edge);
                addedEdges.push(edgeId);
            }
        }
        */

            VisUtils.mergeItems(nodes, this.datasource.nodes);
            this.datasource.edges.clear();
            this.datasource.edges.add(edges);
        },
        { delay: 500 }
    );

    render() {
        const helpHeader = <span>Schematic help</span>;
        const helpBody = (
            <span>
                <p>This schematic displays the fixtures, junctions and pipes of your connection.</p>
                <p>
                    Fixtures are drawn as small circles. Junctions are represented by larger
                    circles, and pipes are drawn as lines between junctions.
                </p>
                <p>
                    Zoom in and out by mouse-wheel, click and drag the background to pan, or
                    click-and-drag a circle to temporarily reposition it.
                </p>
                <p>
                    Click on any component to bring up information about it. You may also click on
                    the magnifying glass icon to the right to ¬auto-zoom the map to fit in the
                    displayed window, or the chevron icon to hide / show the map.
                </p>
                <p>Left click and hold to pan, use mouse wheel to zoom in / out. </p>
            </span>
        );

        const help = (
            <span className="float-right">
                <HelpPopover
                    header={helpHeader}
                    body={helpBody}
                    placement="right"
                    popoverId="ddHelp"
                />
            </span>
        );

        return (
            <Card>
                <CardHeader className="p-1">Schematic {help}</CardHeader>
                <CardBody>
                    <div id="schematicDrawing">
                        <p>connection map</p>
                    </div>
                </CardBody>
            </Card>
        );
    }
}

export default DetailsDrawing;
