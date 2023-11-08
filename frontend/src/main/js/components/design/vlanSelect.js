import React, { Component } from "react";
import { observer, inject } from "mobx-react";
import {
    Card,
    CardBody,
    CardHeader,
    Form,
    InputGroup,
    InputGroupText,
    FormGroup,
    Input,
    Label,
    FormFeedback,
    FormText,
    Alert
} from "reactstrap";
import HelpPopover from "../helpPopover";

import ToggleDisplay from "react-toggle-display";

import Validator from "../../lib/validation";

@inject("designStore", "controlsStore", "topologyStore")
@observer
class VlanSelect extends Component {
    updateForm = () => {
        if (!this.props.controlsStore.connection.schedule.locked) {
            return;
        }

        const ef = this.props.controlsStore.editFixture;

        const baseline = this.props.topologyStore.baseline[ef.port];
        const baselineVlanExpression = baseline.vlanExpression.replace(new RegExp(":", "g"), "-");
        const baselineVlanRanges = baseline.vlanRanges;

        if (!(ef.port in this.props.topologyStore.available)) {
            console.error("internal error: port not found in topology.");
            return;
        }

        let portVlans = this.props.designStore.vlansLockedOnPort(ef.port);
        portVlans.sort();

        const available = this.props.topologyStore.available[ef.port];
        let availableVlanRanges = available.vlanRanges.slice();

        for (let vlan of portVlans) {
            let temp = [];
            for (let range of availableVlanRanges) {
                if (vlan === range.floor) {
                    if (range.floor === range.ceiling) {
                        // do not append this range, single-element range gets completely removed
                    } else {
                        range.floor = range.floor + 1;
                        temp.push(range);
                    }
                } else if (vlan === range.ceiling) {
                    range.ceiling = range.ceiling - 1;
                    temp.push(range);
                } else if (vlan > range.floor && vlan < range.ceiling) {
                    let r1 = { floor: range.floor, ceiling: vlan - 1 };
                    let r2 = { floor: vlan + 1, ceiling: range.ceiling };
                    temp.push(r1);
                    temp.push(r2);
                } else {
                    temp.push(range);
                }
            }
            availableVlanRanges = temp.slice();
        }

        let expressionParts = [];
        availableVlanRanges.sort((a, b) => {
            return a.floor - b.floor;
        });

        for (let range of availableVlanRanges) {
            if (range.floor === range.ceiling) {
                expressionParts.push(range.floor);
            } else {
                expressionParts.push(range.floor + "-" + range.ceiling);
            }
        }
        const availableVlanExpression = expressionParts.join(",");

        let suggestion = this.props.topologyStore.suggestions.globalVlan;

        let lowest = 99999;
        for (let rng of availableVlanRanges) {
            if (lowest > rng.floor) {
                lowest = rng.floor;
            }
        }

        if (suggestion === -1) {
            suggestion = lowest;
        }

        for (let f of this.props.designStore.design.fixtures) {
            if (f.locked && f.port !== ef.port) {
                for (let rng of availableVlanRanges) {
                    if (f.vlan >= rng.floor && f.vlan <= rng.ceiling) {
                        suggestion = f.vlan;
                    }
                }
            }
        }

        if (ef.locked) {
            return;
        }

        this.props.controlsStore.setParamsForEditFixture({
            vlan: {
                available: {
                    expression: availableVlanExpression,
                    ranges: availableVlanRanges,
                    suggestion: suggestion
                },
                baseline: {
                    expression: baselineVlanExpression,
                    ranges: baselineVlanRanges
                },
                validationState: "success",
                validationText: "VLAN available",
                vlanId: suggestion,
                acceptable: true
            }
        });
    };

    componentWillMount() {
        this.updateForm();
    }

    onTypeIn = e => {
        const vlanId = parseInt(e.target.value);
        const ef = this.props.controlsStore.editFixture;

        let valText = "VLAN available";
        let valState = "success";
        let hasError = false;

        let inBaseline = false;
        ef.vlan.baseline.ranges.map(rng => {
            if (rng.floor <= vlanId && rng.ceiling >= vlanId) {
                inBaseline = true;
            }
        });
        if (e.target.value === "") {
            hasError = true;
            valText = "No input.";
        } else if (isNaN(vlanId)) {
            hasError = true;
            valText = "Unable to parse.";
        } else if (!inBaseline) {
            hasError = true;
            valText = "VLAN not in baseline.";
        } else {
            let inAvailable = false;
            ef.vlan.available.ranges.map(rng => {
                if (rng.floor <= vlanId && rng.ceiling >= vlanId) {
                    inAvailable = true;
                }
            });
            if (!inAvailable) {
                hasError = true;
                valText = "VLAN reserved by another connection.";

                let portVlans = this.props.designStore.vlansLockedOnPort(ef.port);
                if (portVlans.includes(vlanId)) {
                    valText =
                        "VLAN being used in this connection (by another fixture on this port).";
                }
            }
        }

        if (hasError) {
            valState = "error";
        }

        this.props.controlsStore.setParamsForEditFixture({
            vlan: {
                acceptable: !hasError,
                validationState: valState,
                validationText: valText,
                vlanId: vlanId
            }
        });
    };

    render() {
        const ef = this.props.controlsStore.editFixture;

        const helpHeader = <span>VLAN selection</span>;
        const helpBody = (
            <span>
                <p>Here you can set the VLAN id for this fixture. </p>

                <p>
                    In 'unlocked' mode, when the dialog opens the value will be editable and set to
                    a suggested VLAN that is likely globally available on the network. You may type
                    in a different value; if it is not available on this fixture you will receive
                    feedback explaining why.
                </p>
                <p>You will also see 'baseline' and 'available' ranges displayed.</p>
                <p>
                    The <u>baseline</u> range is what would be available if there were no other
                    reservations, and generally does not change.
                </p>
                <p>
                    The <u>available</u> range is calculated by taking the baseline and removing any
                    resources used by other reservations overlapping the selected schedule.
                </p>
                <p>
                    When the fixture is locked, this control will display the selected value, and
                    will not be editable. Unlock the fixture to edit.
                </p>
            </span>
        );

        const help = (
            <span className="float-right">
                <HelpPopover
                    header={helpHeader}
                    body={helpBody}
                    placement="left"
                    popoverId="vlanSelectHelp"
                />
            </span>
        );

        return (
            <Card style={{ width: "100%", height: "100%" }}>
                <CardHeader className="p-1">VLAN selection {help}</CardHeader>
                <CardBody>
                    <ToggleDisplay show={!ef.locked}>
                        <Form>
                            <FormGroup>
                                <Label>VLAN:</Label>{" "}
                                <InputGroup>
                                    <Input
                                        type="text"
                                        invalid={ef.vlan.validationState === "error"}
                                        defaultValue={ef.vlan.available.suggestion}
                                        onChange={this.onTypeIn}
                                    />
                                    <InputGroupText>
                                        {Validator.label(ef.vlan.acceptable)}
                                    </InputGroupText>
                                </InputGroup>{" "}
                                <FormFeedback>{ef.vlan.validationText}</FormFeedback>
                            </FormGroup>{" "}
                            <FormGroup>
                                <FormText>Available : {ef.vlan.available.expression}</FormText>
                            </FormGroup>
                            <FormGroup>
                                <FormText>Baseline: {ef.vlan.baseline.expression}</FormText>
                            </FormGroup>
                        </Form>
                    </ToggleDisplay>{" "}
                    <ToggleDisplay show={ef.locked}>
                        <Alert color="info">Locked VLAN: {ef.vlan.vlanId}</Alert>
                    </ToggleDisplay>
                </CardBody>
            </Card>
        );
    }
}

export default VlanSelect;
