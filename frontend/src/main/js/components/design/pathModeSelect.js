import React, { Component } from "react";
import { inject, observer } from "mobx-react";
import {
    Input,
    Label,
    FormGroup,
    Modal,
    ModalHeader,
    ModalBody,
    Card,
    CardBody,
    CardHeader,
    InputGroup,
} from "reactstrap";
import PropTypes from "prop-types";
import Octicon from "react-octicon";

@inject("controlsStore", "designStore")
@observer
class PathModeSelect extends Component {
    componentWillMount() {
        this.setState({
            showHelpModal: false
        });
    }

    toggle = () => {
        this.setState({
            showHelpModal: !this.state.showHelpModal
        });
    };

    render() {
        const help = (
            <span>
                <Modal isOpen={this.state.showHelpModal} fade={false} toggle={this.toggle}>
                    <ModalHeader toggle={this.toggle}>Path mode help</ModalHeader>
                    <ModalBody>
                        <Card>
                            <CardHeader>Fits</CardHeader>
                            <CardBody>
                                <p>
                                    In this mode OSCARS will calculate the shortest path (based on
                                    policy metric) that will fit the bandwidth you want. You will
                                    always be able to find a zero-bandwidth path. This is the
                                    recommended mode to use for most connections.
                                </p>

                                <p>
                                    This mode will re-calculate the path every time you change the
                                    bandwidth, as well as when you make changes to the ERO.
                                    Depending on your input and the state of the network, a path
                                    might not be found; in that case you won't be able to lock the
                                    pipe.{" "}
                                </p>
                            </CardBody>
                        </Card>
                        <Card>
                            <CardHeader>Shortest</CardHeader>
                            <CardBody>
                                <p>
                                    {" "}
                                    The shortest path on the network as calculated by the policy
                                    metric. Will be the most economical on resources and will
                                    maximize the overall network throughput, and minimize latency.
                                </p>

                                <p>
                                    {" "}
                                    This mode, given a specific schedule and start/end points, will
                                    always provide the <b>same path</b>. You will not be able to
                                    give ERO constraints. If you change the bandwidth, the path will
                                    not change as it does in 'Fits' mode; instead, your input will
                                    be validated against previously calculated maximum values. If it
                                    exceeds those values, you won't be able to lock the pipe.
                                </p>
                            </CardBody>
                        </Card>
                        <Card>
                            <CardHeader>Least hops</CardHeader>
                            <CardBody>
                                <p>
                                    {" "}
                                    The path that goes over the least number of network devices and
                                    connections. use this to minimize the chance that the path will
                                    be disrupted by outages.
                                </p>

                                <p>
                                    {" "}
                                    This mode, given a specific schedule and start/end points, will
                                    always provide the <b>same path</b>. You will not be able to
                                    give ERO constraints. If you change the bandwidth, the path will
                                    not change as it does in 'Fits' mode; instead, your input will
                                    be validated against previously calculated maximum values. If it
                                    exceeds those values, you won't be able to lock the pipe.
                                </p>
                            </CardBody>
                        </Card>
                        <Card>
                            <CardHeader>Widest</CardHeader>
                            <CardBody>
                                <p>
                                    {" "}
                                    These modes find the path that has the maximum available
                                    bandwidth, considered either per direction, or as the sum of the
                                    available bandwidth in both directions. Use when you want the
                                    most possible bandwidth over the network with less regard about
                                    keeping the path short.
                                </p>
                                <p>
                                    These modes will take into account any ERO constraints, but will{" "}
                                    <b>not</b> take bandwidth into account for their calculations;
                                    their function is to find the maximum. If you change the
                                    bandwidth, the path will not change as it does in 'Fits' mode;
                                    instead, your input will be validated against previously
                                    calculated.
                                </p>
                            </CardBody>
                        </Card>
                    </ModalBody>
                </Modal>
                <Octicon
                    name="info"
                    style={{ height: "12px", width: "12px" }}
                    onClick={this.toggle}
                />
            </span>
        );

        const pathSelectModeOpts = [
            { value: "fits", label: "Fit to bandwidth" },
            { value: "shortest", label: "Shortest (by metric)" },
            { value: "leastHops", label: "Least hops" },
            { value: "widestSum", label: "Widest overall" },
            { value: "widestAZ", label: "Widest, priority =>" },
            { value: "widestZA", label: "Widest, priority <=" }
        ];
        return (
            <FormGroup className="mt-1 mb-1 pt-1 pb-1">
                <Label>
                    <b>PCE Mode:</b>
                </Label>
                <InputGroup size={"sm"} className={"p-1 m-1"}>
                    <Input
                        id={"pathModeSelect"}
                        type="select"
                        onChange={this.props.onSelectModeChange}
                    >
                        {pathSelectModeOpts.map((option, index) => {
                            return (
                                <option key={index} value={option.value}>
                                    {option.label}
                                </option>
                            );
                        })}
                    </Input>
                </InputGroup>
                <Label>{help}</Label>
            </FormGroup>
        );
    }
}

PathModeSelect.propTypes = {
    onSelectModeChange: PropTypes.func.isRequired
};

export default PathModeSelect;
