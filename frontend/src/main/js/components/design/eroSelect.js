import React, { Component } from "react";
import { inject, observer } from "mobx-react";
import ToggleDisplay from "react-toggle-display";
import {
    CardHeader,
    ListGroup,
    ListGroupItem,
    Button,
    Card,
    CardBody,
    InputGroup
} from "reactstrap";
import Select from "react-select-plus";
import Octicon from "react-octicon";

import { size } from "lodash-es";
import "react-select-plus/dist/react-select-plus.css";
import PropTypes from "prop-types";

import Topology from "../../lib/topology";

@inject("controlsStore", "topologyStore")
@observer
class EroSelect extends Component {
    constructor(props) {
        super(props);
    }

    componentWillMount() {
        this.props.topologyStore.loadAdjacencies();
    }

    resetEro = () => {
        const ep = this.props.controlsStore.editPipe;

        this.props.controlsStore.setParamsForEditPipe({
            ero: {
                include: [ep.a, ep.z],
                acceptable: false,
                exclude: []
            }
        });
    };

    nextHopOptions(urn, adjacencies, ero) {
        let options = [
            {
                label: "Immediate",
                options: []
            },
            {
                label: "Other",
                options: []
            }
        ];

        // TODO: this can be improved
        let added = ero.slice();
        for (let adjcy of adjacencies) {
            let urns = [];
            let label = "";
            if (urn === adjcy.a) {
                label = adjcy.b + " -- " + adjcy.y;
                for (let urn of [adjcy.b, adjcy.y]) {
                    if (!added.includes(urn)) {
                        urns.push(urn);
                        added.push(urn);
                    }
                }
            } else if (urn === adjcy.b) {
                label = adjcy.y;
                if (!added.includes(adjcy.y)) {
                    urns.push(adjcy.y);
                    added.push(adjcy.y);
                }
            } else if (urn === adjcy.y) {
                label = adjcy.z;
                for (let urn of [adjcy.z]) {
                    if (!added.includes(urn)) {
                        urns.push(urn);
                        added.push(urn);
                    }
                }
            }
            if (urns.length > 0) {
                let value = JSON.stringify(urns);
                options[0].options.push({ label: label, value: value });
            }
        }

        for (let adjcy of adjacencies) {
            if (!added.includes(adjcy.a)) {
                let value = JSON.stringify([adjcy.a]);
                options[1].options.push({ label: adjcy.a, value: value });
                added.push(adjcy.a);
            }
            if (!added.includes(adjcy.b)) {
                let value = JSON.stringify([adjcy.b]);
                options[1].options.push({ label: adjcy.b, value: value });
                added.push(adjcy.b);
            }
            if (!added.includes(adjcy.y)) {
                let value = JSON.stringify([adjcy.y]);
                options[1].options.push({ label: adjcy.y, value: value });
                added.push(adjcy.y);
            }
            if (!added.includes(adjcy.z)) {
                let value = JSON.stringify([adjcy.z]);
                options[1].options.push({ label: adjcy.z, value: value });
                added.push(adjcy.z);
            }
        }

        return options;
    }

    removeUrn = i => {
        const ep = this.props.controlsStore.editPipe;
        let new_include = ep.ero.include.slice();
        new_include.splice(i, 1);
        let params = {
            ero: {
                include: new_include,
                acceptable: false,
                message: "Recalculating..."
            }
        };

        this.props.controlsStore.setParamsForEditPipe(params);
    };

    render() {
        const ep = this.props.controlsStore.editPipe;
        const adjcies = this.props.topologyStore.adjacencies;

        if (size(ep.ero.include) === 0) {
            return <p>Loading..</p>;
        }

        const last = ep.z;

        let items = [];

        ep.ero.include.map((urn, i) => {
            if (i === 0 || urn === last) {
                items.push(
                    <ListGroupItem className="p-1" key={urn}>
                        <small>{urn}</small>
                    </ListGroupItem>
                );
            } else {
                items.push(
                    <ListGroupItem className="p-1" key={urn}>
                        <small>
                            {urn}
                            <ToggleDisplay show={!ep.locked}>
                                <span className="float-right">
                                    <Octicon
                                        name="trashcan"
                                        style={{ height: "16px", width: "16px" }}
                                        onClick={() => this.removeUrn(i)}
                                    />
                                </span>
                            </ToggleDisplay>
                        </small>
                    </ListGroupItem>
                );
            }

            if (urn !== last && !ep.locked) {
                let next_urn = ep.ero.include[i + 1];

                if (Topology.adjacent(urn, next_urn, adjcies) === "NONE") {
                    let options = this.nextHopOptions(urn, adjcies, ep.ero.include);
                    items.push(
                        <ListGroupItem className="p-1" key={urn + "-next"}>
                            <NextHopSelect urn={urn} options={options} index={i} />
                        </ListGroupItem>
                    );
                }
            }
        });

        return (
            <Card style={{ width: "100%", height: "100%" }}>
                <CardHeader className="p-1">ERO Constraints</CardHeader>
                <CardBody>
                    <ListGroup>{items}</ListGroup>
                    <br />
                    <Button onClick={this.resetEro}>Clear</Button>
                </CardBody>
            </Card>
        );
    }
}

@inject("controlsStore")
class NextHopSelect extends Component {
    state = {
        selectedOption: {
            label: "",
            value: ""
        }
    };

    insertUrn = selectedOption => {
        if (selectedOption != null && selectedOption.value !== "") {
            this.setState({ selectedOption });
            let urns = JSON.parse(selectedOption.value);

            const ep = this.props.controlsStore.editPipe;
            let new_include = ep.ero.include.slice();
            let start = this.props.index + 1;
            urns.map((urn, i) => {
                new_include.splice(start + i, 0, urn);
            });
            let params = {
                ero: {
                    include: new_include,
                    acceptable: false,
                    message: "path recalculation needed"
                }
            };

            this.props.controlsStore.setParamsForEditPipe(params);
        }
    };

    render() {
        const { selectedOption } = this.state;
        let value = "";

        if (selectedOption != null) {
            value = selectedOption.value;
        }

        return (
            <InputGroup className="p-1 m-1">
                <small>
                    <Select
                        name={this.props.urn + "-next-select"}
                        onChange={this.insertUrn}
                        value={value}
                        autosize={false}
                        options={this.props.options}
                    />
                </small>
            </InputGroup>
        );
    }
}

NextHopSelect.propTypes = {
    urn: PropTypes.string.isRequired,
    index: PropTypes.number.isRequired,
    options: PropTypes.array.isRequired
};

export default EroSelect;
