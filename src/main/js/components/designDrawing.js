import React, {Component} from 'react';
import {inject, observer} from 'mobx-react';
import {autorunAsync, toJS, action} from 'mobx';
import {Panel, Glyphicon, OverlayTrigger, Popover} from 'react-bootstrap';
import transformer from '../lib/transform';
import vis from 'vis';
import validator from '../lib/validation'
import VisUtils from '../lib/vis'
import myClient from '../agents/client';
require('vis/dist/vis-network.min.css');
require('vis/dist/vis.css');


@inject('controlsStore', 'designStore', 'modalStore')
@observer
export default class DesignDrawing extends Component {
    constructor(props) {
        super(props);

        let nodeDataset = new vis.DataSet();
        let edgeDataset = new vis.DataSet();
        this.datasource = {
            nodes: nodeDataset,
            edges: edgeDataset
        };
    }


    state = {
        showMap: true
    };

    onFixtureClicked = (fixture) => {
        const params = transformer.existingFixtureToEditParams(fixture);
        this.props.controlsStore.setParamsForEditFixture(params);
        this.props.modalStore.openModal('editFixture');
    };

    onJunctionClicked = (junction) => {
        this.props.controlsStore.setParamsForEditJunction({junction: junction.id});
        this.props.modalStore.openModal('editJunction');
    };

    onPipeClicked = (pipe) => {
        const params = transformer.existingPipeToEditParams(pipe);
        this.props.controlsStore.setParamsForEditPipe(params);
        this.props.modalStore.openModal('editPipe');
    };

    componentDidMount() {
        let options = {
            height: '300px',
            interaction: {
                hover: false,
                navigationButtons: false,
                zoomView: true,
                dragView: true
            },
            physics: {
                solver: 'barnesHut',
                stabilization: {
                    fit: true
                },
                barnesHut: {
                    centralGravity: 0.5
                }
            },
            nodes: {
                shape: 'dot',
                color: {background: 'white'}
            }
        };

        this.network = new vis.Network(this.mapRef, this.datasource, options);

        this.network.on('dragEnd', (params) => {
            if (params.nodes.length > 0) {
                let nodeId = params.nodes[0];
                this.datasource.nodes.update({id: nodeId, fixed: {x: true, y: true}});
            }
        });

        this.network.on('dragStart', (params) => {
            if (params.nodes.length > 0) {
                let nodeId = params.nodes[0];
                this.datasource.nodes.update({id: nodeId, fixed: {x: false, y: false}});
            }
        });
        this.network.on('click', (params) => {
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
    }


    // this automagically updates the map;
    // TODO: use a reaction and don't clear the whole graph, instead add/remove/update
    disposeOfMapUpdate = autorunAsync(() => {

        let {design} = this.props.designStore;
        let junctions = toJS(design.junctions);
        let fixtures = toJS(design.fixtures);
        let pipes = toJS(design.pipes);

        let nodes = [];
        let edges = [];


        myClient.loadJSON({method: 'GET', url: '/api/map'})
            .then(action((response) => {
                let positions = {};
                let topology = JSON.parse(response);
                topology.nodes.map((n) => {
                    // scale everything down
                    positions[n.id] = {
                        x: 0.3 * n.x,
                        y: 0.3 * n.y
                    }

                });


                junctions.map((j) => {
                    let junctionNode = {
                        id: j.id,
                        label: j.id,
                        size: 20,
                        data: j,
                        x: positions[j.id].x,
                        y: positions[j.id].y,
                        physics: false,
                        color: {
                            inherit: false
                        },
                        onClick: this.onJunctionClicked
                    };
                    nodes.push(junctionNode);
                });


                fixtures.map((f) => {
                    console.log(toJS(f));
                    let fixtureNode = {
                        id: f.id,
                        label: f.label,
                        x: positions[f.device].x + 10,
                        size: 8,
                        color: {
                            background: validator.fixtureMapColor(f),
                            inherit: false
                        },
                        data: f,
                        onClick: this.onFixtureClicked
                    };
                    nodes.push(fixtureNode);
                    let edge = {
                        id: f.device + ' --- ' + f.id,
                        from: f.device,
                        to: f.id,
                        width: 1.5,
                        length: 2
                    };
                    edges.push(edge);
                });

                const colors = [
                    '#CC0000',
                    '#3333FF',
                    '#00CC00', 'orange', 'cyan', 'brown', 'pink'];

                pipes.map((p, pipe_idx) => {
                    if (p.locked) {
                        let i = 0;
                        while (i < p.ero.length - 1) {
                            let a = p.ero[i];
                            let b = p.ero[i + 1];
                            let y = p.ero[i + 2];
                            let z = p.ero[i + 3];

                            let foundZ = false;
                            nodes.map((node) => {
                                if (node.id === z) {
                                    foundZ = true;
                                }
                            });
                            if (!foundZ) {
                                let zNode = {
                                    id: z,
                                    label: z,
                                    onClick: null

                                };
                                nodes.push(zNode);
                            }
                            let edge = {
                                id: pipe_idx + ' : ' + b + ' --- ' + y,
                                from: a,
                                color: {
                                    color: colors[pipe_idx]
                                },
                                to: z,
                                length: 3,
                                width: 1.5
                            };
                            edges.push(edge);


                            i = i + 3;
                        }


                    } else {
                        let edge = {
                            id: p.id,
                            from: p.a,
                            to: p.z,
                            dashes: true,
                            length: 10,
                            color: {
                                color: colors[pipe_idx]
                            },
                            width: 5,
                            data: p,
                            onClick: this.onPipeClicked

                        };
                        edges.push(edge);
                    }
                });
                console.log(toJS(edges));

                VisUtils.mergeItems(nodes, this.datasource.nodes);

                this.datasource.edges.clear();
                this.datasource.edges.add(edges);
                this.network.stabilize(1000);
                this.network.fit({animation: false})


            }));


    }, 500);

    flipMapState = () => {
        this.setState({showMap: !this.state.showMap});
    };

    render() {
        let toggleIcon = this.state.showMap ? 'chevron-down' : 'chevron-right';


        let myHelp = <Popover id='help-designMap' title='Design drawing'>
            <p>This drawing displays the fixtures, junctions and pipes of your design. It starts empty and
                will auto-update as they are added, deleted, or updated.</p>
            <p>Fixtures are drawn as small circles. Junctions are represented by larger circles, and pipes
                are drawn as lines between junctions.</p>
            <p>Unlocked components are drawn in orange color .</p>
            <p>Zoom in and out by mouse-wheel, click and drag the background to pan, or click-and-drag a circle
                to temporarily reposition it.</p>
            <p>Click on any component to bring up its edit form. You may also click on the
                magnifying glass icon to the right to auto-adjust the zoom level to fit in the displayed window,
                or the chevron icon to hide / show the map.</p>
            <p>Left click and hold to pan, use mouse wheel to zoom in / out. </p>
        </Popover>;


        return (
            <Panel expanded={this.state.showMap} onToggle={this.flipMapState}>
                <Panel.Heading>
                    <div><span onClick={this.flipMapState}>Design drawing</span>
                        <div className='pull-right'>
                            <OverlayTrigger trigger='click' rootClose placement='left' overlay={myHelp}>
                                <Glyphicon glyph='question-sign'/>
                            </OverlayTrigger>
                            {' '}
                            <Glyphicon onClick={() => {
                                this.network.fit({animation: true})
                            }} glyph='zoom-out'/>
                            {' '}
                            <Glyphicon onClick={() => this.setState({showMap: !this.state.showMap})}
                                       glyph={toggleIcon}/>
                        </div>
                    </div>
                </Panel.Heading>
                <Panel.Collapse>
                    <div ref={(ref) => {
                        this.mapRef = ref;
                    }}><p>design map</p></div>
                </Panel.Collapse>
            </Panel>

        );
    }
}