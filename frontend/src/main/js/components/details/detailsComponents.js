import React, { Component } from "react";
import { inject, observer } from "mobx-react";
import {toJS } from "mobx";
import {
    Card,
    CardHeader,
    CardBody,
    CardSubtitle,
    ListGroupItem,
    ListGroup,
    NavLink
} from "reactstrap";
import ToggleDisplay from "react-toggle-display";
import { size } from "lodash-es";
import HelpPopover from "../helpPopover";

@inject("connsStore")
@observer
class DetailsComponents extends Component {
    constructor(props) {
        super(props);
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

    onConnectionClicked = () => {
        this.props.connsStore.setSelected({
            type: "connection",
            data: ""
        });
    };

    render() {

        if (typeof(this.props.connsStore.store.current) === "undefined") {
            console.log('undefined store.current');
            return <p>Loading connection..</p>;
        } else if (typeof(this.props.connsStore.store.current.archived) === "undefined") {
            console.log('undefined store.current.archived');
            return <p>Loading connection..</p>;
        } else if (typeof(this.props.connsStore.store.current.archived.cmp) === "undefined") {
            console.log('undefined store.current.archived.cmp');
            return <p>Loading connection..</p>;
        }

        const cmp = this.props.connsStore.store.current.archived.cmp;
        if (size(cmp.junctions) === 0) {
            return <p>Loading connection..</p>;
        }
        const connId = this.props.connsStore.store.current.connectionId;

        const helpHeader = <span>Component List</span>;
        const helpBody = (
            <span>
                <p>This displays the fixtures, junctions, and pipes for the current connection. </p>
                <p>You may click on any component to bring up information about it.</p>
            </span>
        );

        const help = (
            <span className="float-right">
                <HelpPopover
                    header={helpHeader}
                    body={helpBody}
                    placement="right"
                    popoverId="dcHelp"
                />
            </span>
        );

        return (
            <Card>
                <CardHeader>Components {help}</CardHeader>
                <CardBody>
                    <CardSubtitle>Connection info:</CardSubtitle>
                    <ListGroup className="p-1">
                        <ListGroupItem className="p-1" onClick={this.onConnectionClicked}>
                            <NavLink href="#">ID: {connId}</NavLink>
                        </ListGroupItem>
                    </ListGroup>

                    <ToggleDisplay show={cmp.junctions.length > 0}>
                        <hr />
                        <CardSubtitle>Junctions & fixtures:</CardSubtitle>
                        {cmp.junctions.map(junction => {
                            let fixtureNodes = cmp.fixtures.map(fixture => {
                                if (fixture.junction === junction.refId) {
                                    let label = (
                                        fixture.portUrn +
                                        ":" +
                                        fixture.vlan.vlanId
                                    ).replace(junction.refId + ":", "");

                                    return (
                                        <ListGroupItem
                                            className="p-0"
                                            key={label}
                                            onClick={() => {
                                                this.onFixtureClicked(fixture);
                                            }}
                                        >
                                            <NavLink href="#">{label}</NavLink>
                                        </ListGroupItem>
                                    );
                                }
                            });

                            return (
                                <ListGroup className="p-0" key={junction.refId + "nav"}>
                                    <ListGroupItem
                                        className="p-0"
                                        key={junction.refId}
                                        onClick={() => {
                                            this.onJunctionClicked(junction);
                                        }}
                                    >
                                        <NavLink href="#">
                                            <strong>{junction.refId}</strong>
                                        </NavLink>
                                    </ListGroupItem>
                                    {fixtureNodes}
                                </ListGroup>
                            );
                        })}
                    </ToggleDisplay>

                    <ToggleDisplay show={cmp.pipes.length > 0}>
                        <hr />
                        <CardSubtitle>Pipes:</CardSubtitle>
                        <ListGroup>
                            {cmp.pipes.map(pipe => {
                                return (
                                    <ListGroupItem
                                        className="p-0"
                                        key={pipe.a + " --" + pipe.z}
                                        onClick={() => {
                                            this.onPipeClicked(pipe);
                                        }}
                                    >
                                        <NavLink href="#">
                                            <small>
                                                {pipe.a} -- {pipe.z}
                                            </small>
                                        </NavLink>
                                    </ListGroupItem>
                                );
                            })}
                        </ListGroup>
                    </ToggleDisplay>
                </CardBody>
            </Card>
        );
    }
}

export default DetailsComponents;
