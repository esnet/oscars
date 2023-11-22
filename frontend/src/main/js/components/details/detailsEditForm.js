import React, {Component} from "react";
import chrono from "chrono-node";
import {action} from "mobx";
import {inject, observer} from "mobx-react";
import Moment from "moment";
import {Button, Col, Collapse, Form, FormFeedback, FormGroup, FormText, Input, InputGroup, Label} from "reactstrap";
import ToggleDisplay from "react-toggle-display";

import myClient from "../../agents/client";
import DetailsModal from "./detailsModal";

@inject("connsStore", "modalStore")
@observer
class DetailsEditForm extends Component {
    constructor(props) {
        super(props);
    }

    componentDidMount() {
        const conn = this.props.connsStore.store.current;
        const endingValue = this.formatSchedule(conn.archived.schedule.ending).formattedTime;

        this.props.connsStore.setParamsForEditSchedule({
            ending: {
                originalTime: endingValue
            },
            description: {
                originalDescription: conn.description
            }
        });
    }

    formatSchedule(timems) {
        const format = "Y/MM/DD HH:mm";
        const timeSec = Moment(timems * 1000);
        const time = timeSec.format(format);
        const formattedTime = time + " (" + timeSec.fromNow() + ")";

        return {
            time: time,
            formattedTime: formattedTime
        };
    }

    handleDescriptionCancel = () => {
        const description = this.props.connsStore.editSchedule.description;
        this.description.value = description.originalDescription;

        this.props.connsStore.setParamsForEditSchedule({
            description: {
                acceptable: true,
                validationState: "success",
                buttons: {
                    input: true,
                    buttonText: "Edit",
                    collapseText: true,
                    save: true
                }
            }
        });
    };

    handleDescriptionChange = e => {
        let value = e.target.value;

        if (value !== "") {
            this.props.connsStore.setParamsForEditSchedule({
                description: {
                    acceptable: true,
                    validationState: "success",
                    updatedDescription: value,
                    buttons: {
                        save: false
                    }
                }
            });
        } else {
            this.props.connsStore.setParamsForEditSchedule({
                description: {
                    acceptable: false,
                    validationState: "error",
                    validationText: "Can't be empty",
                    buttons: {
                        save: true
                    }
                }
            });
        }
    };

    handleDescriptionEdit = () => {
        this.props.connsStore.setParamsForEditSchedule({
            description: {
                buttons: {
                    input: false,
                    buttonText: "Cancel",
                    collapseText: false
                }
            }
        });
    };

    handleDescriptionSave = () => {
        const conn = this.props.connsStore.store.current;
        const desc = this.props.connsStore.editSchedule.description;
        const modification = {
            connectionId: conn.connectionId,
            description: desc.updatedDescription
        };

        myClient.submitWithToken("POST", "/protected/modify/description", modification).then(
            action(response => {
                this.description.value = desc.updatedDescription;

                this.props.connsStore.setParamsForEditSchedule({
                    description: {
                        originalDescription: desc.updatedDescription,
                        saved: true,
                        buttons: {
                            edit: false,
                            save: true,
                            buttonText: "Edit",
                            collapseText: true,
                            input: true
                        }
                    }
                });
            })
        );
    };

    handleEndingCancel = () => {
        const ending = this.props.connsStore.editSchedule.ending;
        this.endingDate.value = ending.originalTime;

        this.props.connsStore.setParamsForEditSchedule({
            ending: {
                acceptable: true,
                validationState: "success",
                buttons: {
                    input: true,
                    buttonText: "Edit",
                    collapseText: true,
                    save: true
                }
            }
        });
    };

    handleEndingChange = e => {
        let parsed = chrono.parseDate(e.target.value);
        const es = this.props.connsStore.editSchedule;

        if (parsed != null) {
            let currentms = parsed.getTime() / 1000;
            if (
                currentms >= es.ending.validSchedule.beginning &&
                currentms <= es.ending.validSchedule.ending
            ) {
                this.props.connsStore.setParamsForEditSchedule({
                    ending: {
                        acceptable: true,
                        validationState: "success",
                        newEndTime: parsed.getTime() / 1000,
                        parsedValue: parsed,
                        buttons: {
                            save: false
                        }
                    }
                });
            } else {
                this.props.connsStore.setParamsForEditSchedule({
                    ending: {
                        acceptable: false,
                        validationState: "error",
                        validationText: "Ending Time is not within the valid time range",
                        parsedValue: parsed,
                        buttons: {
                            save: true
                        }
                    }
                });
            }
        } else {
            this.props.connsStore.setParamsForEditSchedule({
                ending: {
                    acceptable: false,
                    validationState: "error",
                    validationText: "Ending Time is not a valid date",
                    parsedValue: parsed,
                    buttons: {
                        save: true
                    }
                }
            });
        }
    };

    handleEndingEdit = () => {
        const conn = this.props.connsStore.store.current;
        const validRangeRequest = {
            connectionId: conn.connectionId,
            type: "END"
        };

        myClient.submitWithToken("POST", "/api/valid/schedule", validRangeRequest).then(
            action(response => {
                let status = JSON.parse(response);

                this.props.connsStore.setParamsForEditSchedule({
                    ending: {
                        validSchedule: {
                            beginning: status.floor,
                            ending: status.ceiling
                        },
                        buttons: {
                            input: false,
                            buttonText: "Cancel",
                            collapseText: false
                        }
                    }
                });
            })
        );

        // Format input box text
        const splitValue = this.endingDate.value.split(" ");
        this.endingDate.value = splitValue[0] + " " + splitValue[1];
    };

    handleEndingSave = () => {
        const conn = this.props.connsStore.store.current;
        const ending = this.props.connsStore.editSchedule.ending;
        const modification = {
            connectionId: conn.connectionId,
            type: "END",
            timestamp: ending.newEndTime
        };

        myClient.submitWithToken("POST", "/protected/modify/schedule", modification).then(
            action(response => {
                let status = JSON.parse(response);
                if (status.success === true) {
                    const newTime = this.formatSchedule(status.end).formattedTime;
                    this.endingDate.value = newTime;

                    this.props.connsStore.setParamsForEditSchedule({
                        ending: {
                            originalTime: newTime,
                            saved: true,
                            buttons: {
                                edit: false,
                                save: true,
                                buttonText: "Edit",
                                collapseText: true,
                                input: true
                            }
                        }
                    });
                }
            })
        );
    };

    render() {
        const conn = this.props.connsStore.store.current;
        const es = this.props.connsStore.editSchedule;

        const beginning = this.formatSchedule(conn.archived.schedule.beginning).formattedTime;
        const validStartingTime = this.formatSchedule(es.ending.validSchedule.beginning).time;
        const validEndingTime = this.formatSchedule(es.ending.validSchedule.ending).time;

        return (
            <Form>
                <InputGroup>
                    <Label for="description" sm={2}>
                        Description
                    </Label>
                    <Input
                        type="text"
                        defaultValue={conn.description}
                        innerRef={ref => {
                            this.description = ref;
                        }}
                        disabled={es.description.buttons.input}
                        invalid={es.description.validationState === "error"}
                        onChange={this.handleDescriptionChange}
                    />
                    <FormFeedback>{es.description.validationText}</FormFeedback>
                    {es.description.saved === true ? (
                        <DetailsModal name="editDescription" />
                    ) : (
                        ""
                    )}
                    <ToggleDisplay show={es.description.buttons.buttonText === "Edit"}>
                        <Button color="primary" onClick={this.handleDescriptionEdit}>
                            Edit
                        </Button>
                    </ToggleDisplay>
                    <ToggleDisplay show={es.description.buttons.buttonText === "Cancel"}>
                        <Button color="primary" onClick={this.handleDescriptionCancel}>
                            Cancel
                        </Button>
                    </ToggleDisplay>
                    <Button
                        color="success"
                        disabled={es.description.buttons.save}
                        onClick={this.handleDescriptionSave}
                    >
                        Save
                    </Button>
                </InputGroup>

                <FormGroup row>
                    <Label for="serviceId" sm={2}>
                        ServiceId
                    </Label>
                    <Col sm={10}>
                        <Input type="text" defaultValue={conn.serviceId} disabled />
                    </Col>
                </FormGroup>

                <FormGroup row>
                    <Label for="username" sm={2}>
                        Username
                    </Label>
                    <Col sm={10}>
                        <Input type="text" defaultValue={conn.username} disabled />
                    </Col>
                </FormGroup>

                <FormGroup row>
                    <Label for="beginning" sm={2}>
                        Beginning
                    </Label>
                    <Col sm={10}>
                        <Input type="text" defaultValue={beginning} disabled />
                    </Col>
                </FormGroup>

                <InputGroup>
                    <Label for="ending" sm={2}>
                        Ending
                    </Label>
                    <Input
                        type="text"
                        defaultValue={es.ending.originalTime}
                        innerRef={ref => {
                            this.endingDate = ref;
                        }}
                        disabled={es.ending.buttons.input}
                        invalid={es.ending.validationState === "error"}
                        onChange={this.handleEndingChange}
                    />
                    <FormFeedback>{es.ending.validationText}</FormFeedback>
                    {es.ending.saved ? <DetailsModal name="editEnding" /> : ""}
                    <Collapse isOpen={!es.ending.buttons.collapseText}>
                        <FormGroup>
                            <FormText>Original Ending Time: {es.ending.originalTime}</FormText>
                            <FormText>
                                Valid Range between {validStartingTime} to {validEndingTime}
                            </FormText>
                            <FormText>{`Parsed Value: ${es.ending.parsedValue}`}</FormText>
                        </FormGroup>
                    </Collapse>
                    <ToggleDisplay show={es.ending.buttons.buttonText === "Edit"}>
                        <Button
                            color="primary"
                            disabled={conn.phase === "ARCHIVED"}
                            onClick={this.handleEndingEdit}
                        >
                            Edit
                        </Button>
                    </ToggleDisplay>
                    <ToggleDisplay show={es.ending.buttons.buttonText === "Cancel"}>
                        <Button color="primary" onClick={this.handleEndingCancel}>
                            Cancel
                        </Button>
                    </ToggleDisplay>
                    <Button
                        color="success"
                        disabled={es.ending.buttons.save}
                        onClick={this.handleEndingSave}
                    >
                        Save
                    </Button>
                </InputGroup>
            </Form>
        );
    }
}

export default DetailsEditForm;
