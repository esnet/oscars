import React, {Component} from "react";
import {inject, observer} from "mobx-react";
import {Col, Form, FormGroup, Input, Label} from "reactstrap";

import ScheduleEditForm from "./modifySchedule";
import BandwidthEditForm from "./modifyBandwidth";
import DescriptionEditForm from "./modifyDescription";

@inject("connsStore")
@observer
class DetailsEditForm extends Component {
    constructor(props) {
        super(props);
    }


    render() {
        const conn = this.props.connsStore.store.current;
        return (
            <Form>
                <DescriptionEditForm/>

                <FormGroup row>
                    <Label for="serviceId" sm={2}>
                        Service Id
                    </Label>
                    <Col sm={10}>
                        <Input type="text" defaultValue={conn.serviceId} disabled/>
                    </Col>
                </FormGroup>
                <FormGroup row>
                    <Label for="projectId" sm={2}>
                        Project Id
                    </Label>
                    <Col sm={10}>
                        <Input type="text" defaultValue={conn.projectId} disabled/>
                    </Col>
                </FormGroup>

                <FormGroup row>
                    <Label for="username" sm={2}>
                        Username
                    </Label>
                    <Col sm={10}>
                        <Input type="text" defaultValue={conn.username} disabled/>
                    </Col>
                </FormGroup>

                <BandwidthEditForm/>

                <ScheduleEditForm/>

            </Form>
        );
    }
}

export default DetailsEditForm;
