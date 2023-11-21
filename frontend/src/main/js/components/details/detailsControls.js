import React, { Component } from "react";

import { observer, inject } from "mobx-react";

import { withRouter } from "react-router-dom";

import {Button, Card, CardBody, CardHeader, Label, Input, InputGroup} from "reactstrap";
import PropTypes from "prop-types";

@inject("connsStore", "commonStore")
@observer
class DetailsControls extends Component {
    constructor(props) {
        super(props);
    }

    handleKeyPress = e => {
        if (e.key === "Enter") {
            this.load();
        }
    };

    load = () => {
        let connectionId = this.connectionIdRef.value;
        this.props.history.push("/pages/details/" + connectionId);
        this.props.load(connectionId);
    };

    render() {
        const pathConnectionId = this.props.match.params.connectionId;

        let connLoaded = false;
        if (this.props.connsStore.store.foundCurrent) {
            connLoaded = true;
        }

        return (
            <Card>
                <CardHeader>Search</CardHeader>
                <CardBody>
                    <InputGroup>
                        <Label hidden for="connectionId">Connection ID:</Label>
                        <Input
                            id="connectionId"
                            type="text"
                            innerRef={ref => {
                                this.connectionIdRef = ref;
                            }}
                            defaultValue={pathConnectionId}
                            onKeyPress={this.handleKeyPress}
                            placeholder='Connection ID ("Z0K2")'
                        />
                        <Button
                            color="info"
                            disabled={!connLoaded}
                            onClick={() => {
                                this.props.refresh();
                            }}
                            className="float-left"
                        >
                            Refresh
                        </Button>
                        <Button
                            color="primary"
                            onClick={() => {
                                this.load();
                            }}
                            className="float-right"
                        >
                            Load
                        </Button>

                    </InputGroup>
                </CardBody>
            </Card>
        );
    }
}

export default withRouter(DetailsControls);

DetailsControls.propTypes = {
    refresh: PropTypes.func.isRequired,
    load: PropTypes.func.isRequired
};
