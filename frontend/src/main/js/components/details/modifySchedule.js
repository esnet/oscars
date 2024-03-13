import {inject, observer} from "mobx-react";
import React, {Component} from "react";
import myClient from "../../agents/client";
import {action} from "mobx";
import {Button, Col, FormFeedback, FormGroup, FormText, Input, InputGroup, Label} from "reactstrap";
import ToggleDisplay from "react-toggle-display";
import Util from "../../lib/util";
import * as chrono from "chrono-node";
import moment from "moment";

@inject("connsStore", "modifyStore")
@observer
class ScheduleEditForm extends Component {
    constructor(props) {
        super(props);
    }
    componentDidMount() {
        this.resetForm(null);
    }

    resetForm = (event) => {
        if (event != null) {
            const form = event.currentTarget.form;
            form.reset();
        }

        const conn = this.props.connsStore.store.current;
        const endingValue = conn.archived.schedule.ending
        const visibleValue = Util.formatSchedule(endingValue).verbose;

        this.props.modifyStore.setParamsForEditEnding({
            beingEdited: false,
            value: {
                original: endingValue,
                current: endingValue,
                visible: visibleValue
            },
            validation: {
                valid: true,
                allowed: {
                    beginning: null,
                    ending: null
                },
                feedback: {
                    valid: "",
                    invalid: ""
                }
            },
            help: {
                display: false,
                text: ""
            }
        })

    }



    handleEndingValueChange = e => {
        const es = this.props.modifyStore.editEnding;
        const inputParsed = chrono.parseDate(e.target.value);

        if (inputParsed != null) {
            const inputMoment = moment(inputParsed);

            const allowedFloorMoment = moment.unix(es.validation.allowed.floor);
            const allowedCeilingMoment = moment.unix(es.validation.allowed.ceiling);

            const isInRange =  (inputMoment.isSameOrAfter(allowedFloorMoment) && inputMoment.isSameOrBefore(allowedCeilingMoment));

            const parseMsg = "Parsed: "+inputMoment.format("Y/MM/DD HH:mm") + " (" + inputMoment.fromNow() + ")";

            if (isInRange) {
                this.props.modifyStore.setParamsForEditEnding({
                    validation: {
                        valid: true,
                        feedback: {
                            valid: parseMsg
                        },
                    },
                    value: {
                        current: inputMoment
                    }
                });
            } else {
                this.props.modifyStore.setParamsForEditEnding({
                    validation: {
                        valid: false,
                        feedback: {
                            invalid: parseMsg+" is not within the valid date range"
                        },
                    }
                });
            }
        } else {
            this.props.modifyStore.setParamsForEditEnding({
                validation: {
                    valid: false,
                    feedback: {
                        invalid: "Input is not parsable"
                    },
                }
            });
        }
    };

    handleEditClicked = () => {
        const conn = this.props.connsStore.store.current;
        const es = this.props.modifyStore.editEnding;

        const validRangeRequest = {
            connectionId: conn.connectionId,
            type: "END"
        };

        myClient.submitWithToken("POST", "/api/valid/schedule", validRangeRequest).then(
            action(response => {

                let rangeResponse = JSON.parse(response);

                // if we CAN change the schedule at all:
                if (rangeResponse.allowed) {
                    const formattedCeiling = Util.formatSchedule(rangeResponse.ceiling).verbose;
                    const formattedFloor = Util.formatSchedule(rangeResponse.floor).verbose;

                    const help = <>
                        <p key="1">Original value: {es.value.visible}</p>
                        <p key="2">Allowed range: {formattedFloor} - {formattedCeiling}</p>
                    </>

                    this.props.modifyStore.setParamsForEditEnding({
                        beingEdited: true,

                        validation: {
                            allowed: {
                                floor: Math.floor(rangeResponse.floor),
                                ceiling: Math.floor(rangeResponse.ceiling)
                            },
                            valid: true,
                            feedback: {
                                valid: ""
                            },
                        },
                        help: {
                            display: true,
                            text: help
                        }
                    });

                } else {
                    this.resetForm(null);
                    this.props.modifyStore.setParamsForEditEnding({
                        beingEdited: false,
                        help: {
                            text: <div>Modification not allowed; server response was: <pre>{rangeResponse.explanation}</pre></div>,
                            display: true,
                        }
                    });
                }
            })
        );

    };

    handleSaveClicked = () => {
        const conn = this.props.connsStore.store.current;
        const endingMoment = this.props.modifyStore.editEnding.value.current;

        const modification = {
            connectionId: conn.connectionId,
            type: "END",
            timestamp: endingMoment.unix()
        };

        myClient.submitWithToken("POST", "/protected/modify/schedule", modification).then(
            action(response => {
                this.resetForm(null);
                this.props.connsStore.refreshCurrentPlease();

            })
        );
    };

    render() {
        const conn = this.props.connsStore.store.current;
        const es = this.props.modifyStore.editEnding;
        const editAllowed = conn.phase === "RESERVED" && conn.southbound === 'NSO';

        const beginning = Util.formatSchedule(conn.archived.schedule.beginning).verbose;

        let input = <Input
            disabled
            defaultValue={es.value.visible || ""}
        />

        let saveButton = <></>
        let cancelButton = <></>
        let editButton = <></>
        let formFeedback = <></>

        if (es.beingEdited) {
            if (es.validation.valid) {
                input = <Input valid onChange={this.handleEndingValueChange} />
                saveButton = <Button color="success"  onClick={this.handleSaveClicked}>Save</Button>
                formFeedback = <FormFeedback valid>{es.validation.feedback.valid}</FormFeedback>
            } else {
                input = <Input invalid onChange={this.handleEndingValueChange} />
                saveButton = <Button color="success" disabled>Save</Button>
                formFeedback = <FormFeedback>{es.validation.feedback.invalid}</FormFeedback>
            }
            cancelButton= <Button color="primary" onClick={this.resetForm}>Cancel</Button>
        } else {
            if (editAllowed) {
                editButton = <Button color="primary" onClick={this.handleEditClicked}>Edit</Button>
            } else {
                editButton = <Button color="primary" disabled>Edit</Button>
            }
        }
        return (
            <>
                { /* TODO: allow modifying beginning time*/ }
                <FormGroup row>
                    <Label for="beginning" sm={2}>
                        Beginning
                    </Label>
                    <Col sm={10}>
                        <Input type="text" defaultValue={beginning} disabled />
                    </Col>
                </FormGroup>

                <FormGroup row>

                    <InputGroup>
                        <Label for="ending" sm={2}>Ending</Label>
                        {input}
                        {editButton}
                        {cancelButton}
                        {saveButton}
                        {formFeedback}
                    </InputGroup>

                    <ToggleDisplay show={es.help.display}>
                        <FormText>{es.help.text}</FormText>
                    </ToggleDisplay>

                </FormGroup>
            </>)


    }
}

export default ScheduleEditForm;