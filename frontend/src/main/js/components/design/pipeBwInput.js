import React, { Component } from "react";
import ToggleDisplay from "react-toggle-display";
import {
    Input,
    FormGroup,
    Alert,
    FormText,
    FormFeedback,
    InputGroup,
    InputGroupText
} from "reactstrap";
import Validator from "../../lib/validation";
import PropTypes from "prop-types";

export default class PipeBwInput extends Component {
    constructor(props) {
        super(props);
    }

    onBwChange = e => {
        let inputStr = Validator.cleanBandwidth(e.target.value, this.bwControl);
        const newBw = Number(inputStr);

        if (isNaN(newBw) || e.target.value.length === 0) {
            this.props.onBwInvalid(this.props.direction, "Not a number");
        } else if (newBw < 0) {
            this.props.onBwInvalid(this.props.direction, "Negative value");
        } else {
            this.props.onBwValid(this.props.direction, newBw);
        }
    };

    render() {
        let prepend = null;
        let append = null;
        let input = null;
        let feedback = null;
        if (this.props.direction === "A_TO_Z") {
            prepend = (
                <InputGroupText>&gt;</InputGroupText>
            );
        } else {
            append = (
                <InputGroupText>&lt;</InputGroupText>
            );
        }

        if (this.props.inputMode === "manual") {
            input = (
                <Input
                    type="text"
                    placeholder="0-100000"
                    defaultValue={this.props.defaultValue}
                    innerRef={ref => {
                        this.bwControl = ref;
                        this.props.refCallback(this.props.direction, ref);
                    }}
                    invalid={this.props.isInvalid(this.props.direction)}
                    onChange={this.onBwChange}
                />
            );
            feedback = (
                <FormFeedback>
                    <small>{this.props.getInvalidReason(this.props.direction)}</small>
                </FormFeedback>
            );
        } else if (this.props.inputMode === "locked") {
            input = <Alert color="info">{this.props.defaultValue} Mbps</Alert>;
        } else if (this.props.inputMode === "auto") {
            input = (
                <Input
                    type="text"
                    defaultValue={this.props.defaultValue}
                    innerRef={ref => {
                        this.bwControl = ref;
                        this.props.refCallback(this.props.direction, ref);
                    }}
                    invalid={this.props.isInvalid(this.props.direction)}
                    disabled={true}
                />
            );

            feedback = (
                <FormFeedback>
                    <small>{this.props.getInvalidReason(this.props.direction)}</small>
                </FormFeedback>
            );
        }

        return (
            <div>
                <FormGroup>
                    <InputGroup>
                        {prepend}
                        {input}
                        {append}
                        {feedback}
                    </InputGroup>
                </FormGroup>
                <FormGroup className="pt-0 pb-0 m-0">
                    <FormText className="m-0 p-0">
                        <small>
                            <p className="m-0">Reservable: {this.props.available} Mbps</p>
                            <ToggleDisplay show={this.props.eroMode === "fits"}>
                                <p className="m-0">Widest: {this.props.widest} Mbps</p>
                            </ToggleDisplay>
                            <p className="m-0">Baseline: {this.props.baseline} Mbps</p>
                        </small>
                    </FormText>
                </FormGroup>
            </div>
        );
    }
}

PipeBwInput.propTypes = {
    direction: PropTypes.string.isRequired,
    defaultValue: PropTypes.number.isRequired,
    inputMode: PropTypes.string.isRequired,

    available: PropTypes.number.isRequired,
    widest: PropTypes.number.isRequired,
    baseline: PropTypes.number.isRequired,
    eroMode: PropTypes.string.isRequired,

    isInvalid: PropTypes.func.isRequired,
    getInvalidReason: PropTypes.func.isRequired,
    refCallback: PropTypes.func.isRequired,

    onBwValid: PropTypes.func.isRequired,
    onBwInvalid: PropTypes.func.isRequired
};
