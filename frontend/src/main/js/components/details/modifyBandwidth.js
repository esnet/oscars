import {inject, observer} from "mobx-react";
import React, {Component} from "react";
import myClient from "../../agents/client";
import {action} from "mobx";
import {Button, Col, FormFeedback, FormGroup, FormText, Input, InputGroup, Label} from "reactstrap";
import ToggleDisplay from "react-toggle-display";
import Util from "../../lib/util";

@inject("connsStore", "modifyStore")
@observer
class BandwidthEditForm extends Component {
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

        const bandwidth = Util.determineBandwidth(conn);

        this.props.modifyStore.setParamsForEditBandwidth({
            beingEdited: false,
            value: {
                original: bandwidth,
                current: bandwidth,
            },

            validation: {
                valid: true,
                allowed: {
                    floor: null,
                    ceiling: null
                },
                feedback: {
                    valid: "Valid",
                    invalid: "Invalid"
                },
            },
            help: {
                display: false,
                text: ""
            }
        })
    }



    handleBwValueChange = e => {

        const eb = this.props.modifyStore.editBandwidth;
        const parsed = parseInt(e.target.value)

        if (Number.isNaN(parsed )) {
            this.props.modifyStore.setParamsForEditBandwidth({
                validation: {
                    valid: false,
                    feedback: {
                        valid: "",
                        invalid: "Input is not parsable"
                    },
                }
            });
        } else {

            const isInRange =  (parsed >= eb.validation.allowed.floor && parsed <= eb.validation.allowed.ceiling);

            if (isInRange) {
                this.props.modifyStore.setParamsForEditBandwidth({
                    validation: {
                        valid: true,
                        feedback: {
                            valid: "Bandwidth in range",
                            invalid: "",
                        },
                    },
                    value: {
                        current: parsed
                    }
                });
            } else {
                this.props.modifyStore.setParamsForEditBandwidth({
                    validation: {
                        valid: false,
                        feedback: {
                            valid: "",
                            invalid: parsed+" is not within the valid range"
                        },
                    }
                });
            }

        }
    };

    handleEditClicked = () => {
        const conn = this.props.connsStore.store.current;
        const eb = this.props.modifyStore.editBandwidth;

        const validRangeRequest = {
            connectionId: conn.connectionId
        };

        myClient.submitWithToken("POST", "/api/valid/bandwidth", validRangeRequest).then(
            action(response => {
                let rangeResponse = JSON.parse(response);
                const help = <p>Allowed: {rangeResponse.floor} - {rangeResponse.ceiling}; original value: {eb.value.original}; </p>
                this.props.modifyStore.setParamsForEditBandwidth({
                    beingEdited: true,
                    validation: {
                        allowed: {
                            floor: rangeResponse.floor,
                            ceiling: rangeResponse.ceiling
                        },
                        valid: true,
                        feedback: {
                            valid: "",
                            invalid: ""
                        },
                    },
                    help: {
                        display: true,
                        text: help
                    }
                });

            })
        );

    };

    handleSaveClicked = () => {
        const conn = this.props.connsStore.store.current;

        const bandwidth = this.props.modifyStore.editBandwidth.value.current;
        const modification = {
            connectionId: conn.connectionId,
            bandwidth: bandwidth
        };

        myClient.submitWithToken("POST", "/protected/modify/bandwidth", modification).then(
            action(response => {
                this.resetForm();
                this.props.connsStore.refreshCurrentPlease();
            })
        );
    };

    render() {

        const conn = this.props.connsStore.store.current;
        const editAllowed = conn.phase === "RESERVED" && conn.southbound === 'NSO';

        const eb = this.props.modifyStore.editBandwidth;
        let input = <Input
            type="text"
            disabled
            defaultValue={eb.value.original || ""}
        />
        let saveButton = <></>
        let cancelButton = <></>
        let editButton = <></>
        let formFeedback = <></>

        if (eb.beingEdited) {
            if (eb.validation.valid) {
                input = <Input type="text" valid onChange={this.handleBwValueChange} />
                saveButton = <Button color="success"  onClick={this.handleSaveClicked}>Save</Button>
                formFeedback = <FormFeedback valid>{eb.validation.feedback.valid}</FormFeedback>
            } else {
                input = <Input type="text" invalid onChange={this.handleBwValueChange} />
                saveButton = <Button color="success" disabled>Save</Button>
                formFeedback = <FormFeedback>{eb.validation.feedback.invalid}</FormFeedback>
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
            <FormGroup row >

                <InputGroup>
                    <Label for="bandwidth" sm={2}>Bandwidth</Label>

                    {input}
                    {editButton}

                    {cancelButton}
                    {saveButton}
                    {formFeedback}


                </InputGroup>
                <ToggleDisplay show={eb.help.display}>
                    <FormText>{eb.help.text}</FormText>
                </ToggleDisplay>

            </FormGroup>)


    }
}

export default BandwidthEditForm;