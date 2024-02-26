import {inject, observer} from "mobx-react";
import React, {Component} from "react";
import myClient from "../../agents/client";
import {action} from "mobx";
import {Button, Col, FormFeedback, FormGroup, FormText, Input, InputGroup, Label} from "reactstrap";

@inject("connsStore", "modifyStore")
@observer
class DescriptionEditForm extends Component {
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

        this.props.modifyStore.setParamsForEditDescription({
            beingEdited: false,
            value: {
                original: conn.description,
                current: conn.description,
            },
            validation: {
                valid: true,
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


    handleValueChange = e => {

        if (e.target.value != null && e.target.value !== "") {
            this.props.modifyStore.setParamsForEditDescription({
                validation: {
                    valid: true,
                    feedback: {
                        valid: ""
                    },
                },
                value: {
                    current: e.target.value
                }
            });
        } else {
            this.props.modifyStore.setParamsForEditDescription({
                validation: {
                    valid: false,
                    feedback: {
                        invalid: "Input is empty"
                    },
                }
            });
        }
    };

    handleEditClicked = () => {
        this.props.modifyStore.setParamsForEditDescription({
            beingEdited: true,
        });

    };

    handleSaveClicked = () => {
        const conn = this.props.connsStore.store.current;
        const newDescription = this.props.modifyStore.editDescription.value.current;

        const modification = {
            connectionId: conn.connectionId,
            description: newDescription
        };
        myClient.submitWithToken("POST", "/protected/modify/description", modification).then(
            action(response => {
                this.resetForm();
                this.props.connsStore.refreshCurrentPlease();
            })
        );
    };





    render() {
        const conn = this.props.connsStore.store.current;
        const ed = this.props.modifyStore.editDescription;

        let input = <Input
            disabled
            defaultValue={ed.value.original || ""}
        />

        let saveButton = <></>
        let cancelButton = <></>
        let editButton = <></>
        let formFeedback = <></>

        if (ed.beingEdited) {
            if (ed.validation.valid) {
                input = <Input valid onChange={this.handleValueChange}/>
                saveButton = <Button color="success" onClick={this.handleSaveClicked}>Save</Button>
                formFeedback = <FormFeedback valid>{ed.validation.feedback.valid}</FormFeedback>
            } else {
                input = <Input invalid onChange={this.handleValueChange}/>
                saveButton = <Button color="success" disabled>Save</Button>
                formFeedback = <FormFeedback invalid>{ed.validation.feedback.invalid}</FormFeedback>
            }
            cancelButton = <Button color="primary" onClick={this.resetForm}>Cancel</Button>
        } else {
            editButton = <Button color="primary" onClick={this.handleEditClicked}>Edit</Button>
        }
        return (
            <FormGroup row>
                <InputGroup>
                    <Label for="description" sm={2}>Description</Label>
                    {input}
                    {editButton}
                    {cancelButton}
                    {saveButton}
                    {formFeedback}
                </InputGroup>
            </FormGroup>
        )


    }
}

export default DescriptionEditForm;