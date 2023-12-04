import React, { Component } from "react";
import { Typeahead } from "react-bootstrap-typeahead";
import { inject, observer } from "mobx-react";
import { toJS } from "mobx";
import { size } from "lodash-es";

import { Form, FormGroup, InputGroup, InputGroupText, Card } from "reactstrap";

import transformer from "../../lib/transform";
import HelpPopover from "../helpPopover";

require("react-bootstrap-typeahead/css/Typeahead.css");

@inject("topologyStore", "controlsStore", "designStore", "mapStore", "modalStore")
@observer
class SelectPortTypeahead extends Component {
    constructor(props) {
        super(props);
    }

    componentWillMount() {
        this.props.topologyStore.loadEthernetPorts();
    }

    onTypeaheadSelection = selected => {
        if (size(selected) !== 1) {
            return;
        }
        let port = selected[0].id;
        let device = selected[0].device;

        let params = {
            port: port,
            device: device
        };

        let fixture = this.props.designStore.addFixtureDeep(params);

        const editFixtureParams = transformer.newFixtureToEditParams(fixture);
        this.props.controlsStore.setParamsForEditFixture(editFixtureParams);

        this.props.modalStore.openModal("editFixture");
    };

    render() {
        const { ethPorts } = this.props.topologyStore;

        let options = [];
        if (typeof ethPorts !== "undefined") {
            for (let option of toJS(ethPorts)) {
                if (size(option.reservableVlans) > 0) {
                    options.push(option);
                }
            }
        }

        const helpHeader = <span>Text-based fixture selection</span>;
        const helpBody = (
            <span>
                <p>
                    Start typing in the text box to bring up an auto-complete list of ports matching
                    your input. Select with up and down arrow keys.
                </p>
                <p>
                    Click on a port (or finish typing and press Enter) to add a new fixture with
                    that port.
                </p>
            </span>
        );

        const help = (
            <HelpPopover
                header={helpHeader}
                body={helpBody}
                placement="left"
                popoverId="spAddHelp"
            />
        );

        return (
            <Card>
                <Form inline="true">
                    <FormGroup>
                        <InputGroup>
                            <Typeahead
                                id="portUrn"
                                placeholder="Type port urn to add"
                                minLength={2}
                                options={options}
                                onChange={this.onTypeaheadSelection}
                            />
                            <InputGroupText>{help}</InputGroupText>
                        </InputGroup>
                    </FormGroup>
                </Form>
            </Card>
        );
    }
}

export default SelectPortTypeahead;
