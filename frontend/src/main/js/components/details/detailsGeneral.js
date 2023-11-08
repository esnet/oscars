import React, { Component } from "react";
import { observer, inject } from "mobx-react";
import {
    Card,
    CardBody,
    CardHeader,
    Nav,
    NavLink,
    NavItem,
    ListGroup,
    ListGroupItem,
    TabPane,
    TabContent
} from "reactstrap";
import classnames from "classnames";

// import DetailsButtons from "./detailsButtons";
import DetailsDrawing from "./detailsDrawing";
import DetailsEditForm from "./detailsEditForm";
import DetailsHistory from "./detailsHistory";
import DetailsTags from "./detailsTags";
import HelpPopover from "../helpPopover";

@inject("connsStore")
@observer
class DetailsGeneral extends Component {
    constructor(props) {
        super(props);
        this.state = {
            tab: ""
        };
    }

    componentWillMount() {
        this.setState({
            tab: "info"
        });
    }

    setTab = tab => {
        if (this.state.tab !== tab) {
            if (tab === "drawing") {
                this.redraw();
            }
            this.setState({
                tab: tab
            });
        }
    };

    redraw() {
        this.props.connsStore.setRedraw(true);
    }

    render() {
        const conn = this.props.connsStore.store.current;

        if (conn.archived.schedule.beginning != null && conn.archived.schedule.ending != null) {
            const phaseTexts = {
                RESERVED: "Reserved",
                ARCHIVED: "Archived",
                HELD: "Held"
            };

            const modeTexts = {
                MANUAL: "Manual",
                AUTOMATIC: "Scheduled"
            };

            const stateTexts = {
                ACTIVE: "Active",
                WAITING: "Waiting",
                FAILED: "Failed",
                FINISHED: "Finished"
            };

            let editDetails = (
                <ListGroup>
                    <ListGroupItem>
                        <DetailsEditForm />
                    </ListGroupItem>
                </ListGroup>
            );

            let states = (
                <ListGroup>
                    <ListGroupItem>
                        Phase: {phaseTexts[conn.phase]} {this.phaseHelp(conn.phase)}
                    </ListGroupItem>
                    <ListGroupItem>
                        State: {stateTexts[conn.state]} {this.stateHelp(conn.state)}
                    </ListGroupItem>
                    <ListGroupItem>
                        Build mode: {modeTexts[conn.mode]} {this.modeHelp(conn.mode)}
                    </ListGroupItem>
                </ListGroup>
            );

            return (
                <Card>
                    <CardHeader>Connection Information</CardHeader>
                    <CardBody>
                        <Nav tabs>
                            <NavItem>
                                <NavLink
                                    href="#"
                                    className={classnames({ active: this.state.tab === "info" })}
                                    onClick={() => {
                                        this.setTab("info");
                                    }}
                                >
                                    Info
                                </NavLink>
                            </NavItem>
                            <NavItem>
                                <NavLink
                                    href="#"
                                    className={classnames({ active: this.state.tab === "tags" })}
                                    onClick={() => {
                                        this.setTab("tags");
                                    }}
                                >
                                    Tags
                                </NavLink>
                            </NavItem>
                            <NavItem>
                                <NavLink
                                    href="#"
                                    className={classnames({ active: this.state.tab === "drawing" })}
                                    onClick={() => {
                                        this.setTab("drawing");
                                    }}
                                >
                                    Drawing
                                </NavLink>
                            </NavItem>
                            <NavItem>
                                <NavLink
                                    href="#"
                                    className={classnames({ active: this.state.tab === "history" })}
                                    onClick={() => {
                                        this.setTab("history");
                                    }}
                                >
                                    History
                                </NavLink>
                            </NavItem>
                            <NavItem>
                                <NavLink
                                    href="#"
                                    className={classnames({ active: this.state.tab === "troubleshoot" })}
                                    onClick={() => {
                                        this.setTab("troubleshoot");
                                    }}
                                >
                                    History
                                </NavLink>
                            </NavItem>
                        </Nav>
                        <TabContent activeTab={this.state.tab}>
                            <TabPane tabId="info" title="Info">
                                <br />
                                {editDetails}
                                <br />
                                {states}
                                {/* <br /> */}
                                {/* <DetailsButtons /> */}
                            </TabPane>
                            <TabPane tabId="tags" title="Tags">
                                <DetailsTags />
                            </TabPane>
                            <TabPane tabId="drawing" title="Drawing">
                                <DetailsDrawing />
                            </TabPane>
                            <TabPane tabId="history" title="History">
                                <DetailsHistory />
                            </TabPane>
                            <TabPane tabId="troubleshoot" title="Troubleshoot">
                                <DetailsTroubleshoot />
                            </TabPane>
                        </TabContent>
                    </CardBody>
                </Card>
            );
        } else {
            return <div>Loading...</div>;
        }
    }

    phaseHelp(phase) {
        const header = <span>Phase help</span>;
        let body = (
            <span>
                Phases refer to the connection's lifecycle in regards to resource reservation. There
                are three phases:
                <ul>
                    <li>
                        <b>Held</b>: very short term, before the connection has been committed.
                    </li>
                    <li>
                        <b>Reserved</b>: after the connection has been committed and before end
                        time.
                    </li>
                    <li>
                        <b>Archived</b>: after end time or after being released.
                    </li>
                </ul>
            </span>
        );
        return (
            <span className="float-right">
                <HelpPopover header={header} body={body} placement="right" popoverId="phase-help" />
            </span>
        );
    }

    stateHelp(state) {
        const header = <span>State help</span>;
        let body = (
            <span>
                State refers to the connection's lifecycle in regards to network configuration. The
                main states are as follows:
                <ul>
                    <li>
                        <b>Waiting</b>: when the connection is still waiting to be built
                    </li>
                    <li>
                        <b>Active</b>: when successfully configured and operational,
                    </li>
                    <li>
                        <b>Finished</b>: after the connection end time (or after release)
                    </li>
                    <li>
                        <b>Failed</b>: when something's wrong.
                    </li>
                </ul>
            </span>
        );
        return (
            <span className="float-right">
                <HelpPopover header={header} body={body} placement="right" popoverId="state-help" />
            </span>
        );
    }

    modeHelp(mode) {
        const header = <span>Build mode help</span>;
        let body = (
            <span>
                Build mode refers to the connection's setting regarding when / how it will configure
                network devices. There are two modes:
                <ul>
                    <li>
                        <b>Scheduled</b>: OSCARS will build the connection automatically
                    </li>
                    <li>
                        <b>Manual</b>: OSCARS will wait for a user command to build{" "}
                    </li>
                </ul>
            </span>
        );
        return (
            <span className="float-right">
                <HelpPopover header={header} body={body} placement="right" popoverId="build-help" />
            </span>
        );
    }
}

export default DetailsGeneral;
