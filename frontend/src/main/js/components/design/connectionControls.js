import React, { Component } from "react";

import { observer, inject } from "mobx-react";
import { action, autorun } from "mobx";
import Octicon from "react-octicon";
import ToggleDisplay from "react-toggle-display";
import { Alert, Form, Label, Button, Card, CardBody, FormGroup, Input, Collapse } from "reactstrap";

import myClient from "../../agents/client";
import validator from "../../lib/validation";
import CommitButton from "./commitButton";
import HelpPopover from "../helpPopover";
import TagControls from "./tagControls";

@inject("controlsStore", "designStore", "modalStore")
@observer
class ConnectionControls extends Component {
    constructor(props) {
        super(props);
        this.toggle = this.toggle.bind(this);
        this.state = { collapse: false };
    }

    componentWillMount() {
        if (this.props.controlsStore.connection.connectionId === "") {
            myClient.submitWithToken("GET", "/protected/conn/generateId").then(
                action(response => {
                    let params = {
                        description: "",
                        phase: "HELD",
                        connectionId: response,
                        mode: "AUTOMATIC",
                        connection_mtu: 9000
                    };
                    this.props.controlsStore.setParamsForConnection(params);
                })
            );
        }
    }

    // TODO: make sure you can't uncommit past start time

    disposeOfValidate = autorun(
        () => {
            let validationParams = {
                connection: this.props.controlsStore.connection,
                junctions: this.props.designStore.design.junctions,
                pipes: this.props.designStore.design.pipes,
                fixtures: this.props.designStore.design.fixtures
            };

            const result = validator.validateConnection(validationParams);
            this.props.controlsStore.setParamsForConnection({
                validation: {
                    errors: result.errors,
                    acceptable: result.ok
                }
            });
        },
        { delay: 1000 }
    );

    componentWillUnmount() {
        this.disposeOfValidate();
    }

    onDescriptionChange = e => {
        const params = {
            description: e.target.value
        };
        this.props.controlsStore.setParamsForConnection(params);
    };

    onBuildModeChange = e => {
        const params = {
            mode: e.target.value
        };
        this.props.controlsStore.setParamsForConnection(params);
    };

    onMTUChange = e => {
        const params = {
            connection_mtu: parseInt(e.target.value, 10)
        };
        this.props.controlsStore.setParamsForConnection(params);
    };

    toggle() {
        this.setState(state => ({ collapse: !state.collapse }));
    }

    render() {
        const conn = this.props.controlsStore.connection;
        let connectionId = conn.connectionId;

        const buildHelpHeader = <span>Build mode help</span>;
        const buildHelpBody = (
            <span>
                <p>
                    Auto: The connection will be configured on network devices ("built") on schedule
                    at start time. No further action needed.
                </p>
                <p>
                    Manual: The connection will <b>not</b> be built at start time. Once the
                    connection has been committed, you can use the controls in the connection
                    details page to build / dismantle it.
                </p>
                <p>
                    Mode seleciton is not final. In the connection details page, you can switch
                    between modes, as long as end time has not been reached.
                </p>
                <p>In either mode, once end time is reached the connection will be dismantled.</p>
            </span>
        );

        const buildHelp = (
            <span className="float-right">
                <HelpPopover
                    header={buildHelpHeader}
                    body={buildHelpBody}
                    placement="right"
                    popoverId="buildHelp"
                />
            </span>
        );

        const mtuHelpHeader = <span>Connection MTU help</span>;
        const mtudHelpBody = (
            <span>
                <p>MTU is the desired data size that the frame will carry.</p>
                <p>
                    The default value is 9000, without the overhead. The user can provide a value
                    between 1500 and 9000 (inclusive).
                </p>
            </span>
        );

        const mtuHelp = (
            <span className="float-right">
                <HelpPopover
                    header={mtuHelpHeader}
                    body={mtudHelpBody}
                    placement="right"
                    popoverId="mtuHelp"
                />
            </span>
        );

        return (
            <Card>
                <CardBody>
                    <Form
                        onSubmit={e => {
                            e.preventDefault();
                        }}
                    >
                        <Alert color="info">
                            <strong>
                                Help me!
                                <span className="float-right">
                                    <span>
                                        <Octicon
                                            name="info"
                                            style={{
                                                height: "18px",
                                                width: "18px",
                                                cursor: "pointer"
                                            }}
                                            onClick={() => {
                                                this.props.modalStore.openModal("designHelp");
                                            }}
                                        />
                                    </span>
                                </span>
                            </strong>
                            <div>Connection id: {connectionId}</div>
                        </Alert>
                        <FormGroup>
                            {" "}
                            <Label>Description:</Label>
                            <Input
                                type="text"
                                placeholder="Type a description"
                                valid={validator.descriptionControl(conn.description) === "success"}
                                invalid={
                                    validator.descriptionControl(conn.description) !== "success"
                                }
                                defaultValue={conn.description}
                                onChange={this.onDescriptionChange}
                            />
                        </FormGroup>
                        <FormGroup>
                            <Label>Build Mode:</Label>
                            {buildHelp}{" "}
                            <Input type="select" onChange={this.onBuildModeChange}>
                                <option value="AUTOMATIC">Scheduled</option>
                                <option value="MANUAL">Manual</option>
                            </Input>
                        </FormGroup>
                        <FormGroup>
                            <Label>Connection MTU:</Label>
                            {mtuHelp}{" "}
                            <Input
                                type="number"
                                placeholder="Desired data MTU size"
                                valid={validator.mtuControl(conn.connection_mtu) === "success"}
                                invalid={validator.mtuControl(conn.connection_mtu) !== "success"}
                                defaultValue={conn.connection_mtu}
                                onChange={this.onMTUChange}
                            />
                        </FormGroup>
                        <FormGroup>
                            <Button
                                color="secondary"
                                onClick={this.toggle}
                                style={{ marginBottom: "0.5rem" }}
                            >
                                Click to fill project details
                            </Button>
                            <Collapse isOpen={this.state.collapse}>
                                <TagControls />
                            </Collapse>
                        </FormGroup>
                        <FormGroup>
                            <ToggleDisplay show={!conn.validation.acceptable}>
                                <Button
                                    color="warning"
                                    className="float-left"
                                    onClick={() => {
                                        this.props.modalStore.openModal("connectionErrors");
                                    }}
                                >
                                    Display errors
                                </Button>
                            </ToggleDisplay>
                            <ToggleDisplay
                                show={conn.validation.acceptable && conn.phase === "HELD"}
                            >
                                <CommitButton />
                            </ToggleDisplay>
                        </FormGroup>
                    </Form>
                </CardBody>
            </Card>
        );
    }
}

export default ConnectionControls;
