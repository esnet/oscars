import React, { Component } from "react";
import { observer, inject } from "mobx-react";
import {
    Modal,
    ModalBody,
    ModalHeader,
    Button,
    Input,
    Container,
    FormGroup,
    Card,
    CardBody,
    CardHeader,
    TabContent,
    TabPane,
    Alert,
    Label,
    Row,
    Col,
    Nav,
    NavItem,
    NavLink,
    ButtonToolbar,
    ListGroup,
    ListGroupItem,
    Form
} from "reactstrap";

import { autorun, toJS } from "mobx";
import classnames from "classnames";

import ToggleDisplay from "react-toggle-display";
import EroSelect from "./eroSelect";
import DeviceFixtures from "./deviceFixtures";
import PathModeSelect from "./pathModeSelect";
import myClient from "../../agents/client";
import ConfirmModal from "../confirmModal";
import HelpPopover from "../helpPopover";
import PipeBwInput from "./pipeBwInput";
import EroDrawing from "./eroDrawing";

const modalName = "editPipe";

@inject("designStore", "controlsStore", "topologyStore", "modalStore")
@observer
class EditPipeModal extends Component {
    constructor(props) {
        super(props);
        this.bwCtlRefs = {};
    }

    componentWillMount() {
        this.setState({
            eroTab: "drawing"
        });
    }

    componentWillUnmount() {
        this.props.controlsStore.setParamsForEditPipe({
            ero: {
                include: [],
                exclude: [],
                acceptable: false,
                message: ""
            }
        });
        this.pathUpdateDispose();
        this.validationDispose();
    }

    closeModal = () => {
        this.props.controlsStore.setParamsForEditPipe({ pipeId: null });
        this.props.modalStore.closeModal(modalName);
    };

    toggleModal = () => {
        if (this.props.modalStore.modals.get(modalName)) {
            this.closeModal();
        } else {
            this.props.modalStore.openModal(modalName);
        }
    };

    protectClicked = e => {
        let nextValue = false;
        if (e.target.checked) {
            nextValue = true;
        }
        this.props.controlsStore.setParamsForEditPipe({
            protect: nextValue
        });
        let ep = this.props.controlsStore.editPipe;
        console.log(toJS(ep));
    };

    performPathfinding = () => {
        let conn = this.props.controlsStore.connection;
        let ep = this.props.controlsStore.editPipe;

        let pipe = this.props.designStore.findPipe(ep.pipeId);

        if (pipe === null || pipe.locked || !conn.schedule.locked) {
            return;
        }

        // clear ERO, show loading state
        this.props.controlsStore.setParamsForEditPipe({
            paths: {
                sync: {
                    loading: true
                }
            },
            ero: {
                message: "Updating path..",
                acceptable: false,
                hops: []
            }
        });

        let params = {
            interval: {
                beginning: conn.schedule.start.at.getTime() / 1000,
                ending: conn.schedule.end.at.getTime() / 1000
            },
            a: ep.a,
            z: ep.z,
            azBw: ep.A_TO_Z.bw,
            zaBw: ep.Z_TO_A.bw,
            include: ep.ero.include
        };

        myClient
            .loadJSON({ method: "POST", url: "/api/pce/paths", params })
            .then(response => {
                let parsed = JSON.parse(response);
                let uiParams = {
                    paths: {
                        sync: {
                            loading: false,
                            initialized: true
                        },
                        fits: {},
                        shortest: {},
                        leastHops: {},
                        widestSum: {},
                        widestAZ: {},
                        widestZA: {}
                    },
                    A_TO_Z: {},
                    Z_TO_A: {}
                };

                const modes = [
                    "fits",
                    "shortest",
                    "leastHops",
                    "widestSum",
                    "widestAZ",
                    "widestZA"
                ];
                modes.map(mode => {
                    let ero = [];
                    if (parsed[mode] === null) {
                        uiParams.paths[mode].acceptable = false;
                        uiParams.paths[mode].azAvailable = -1;
                        uiParams.paths[mode].zaAvailable = -1;
                        uiParams.paths[mode].azBaseline = -1;
                        uiParams.paths[mode].zaBaseline = -1;
                        uiParams.paths[mode].ero = [];
                        return;
                    }

                    parsed[mode]["azEro"].map(e => {
                        ero.push(e["urn"]);
                    });
                    uiParams.paths[mode].ero = ero;
                    if (ero.length > 0) {
                        uiParams.paths[mode].acceptable = true;
                        uiParams.paths[mode].azAvailable = parsed[mode].azAvailable;
                        uiParams.paths[mode].zaAvailable = parsed[mode].zaAvailable;
                        uiParams.paths[mode].azBaseline = parsed[mode].azBaseline;
                        uiParams.paths[mode].zaBaseline = parsed[mode].zaBaseline;
                    } else {
                        uiParams.paths[mode].acceptable = false;
                        uiParams.paths[mode].azAvailable = -1;
                        uiParams.paths[mode].zaAvailable = -1;
                        uiParams.paths[mode].azBaseline = -1;
                        uiParams.paths[mode].zaBaseline = -1;
                    }
                    if (mode === "widestAZ") {
                        uiParams.A_TO_Z.widest = uiParams.paths[mode].azAvailable;
                    }
                    if (mode === "widestZA") {
                        uiParams.Z_TO_A.widest = uiParams.paths[mode].zaAvailable;
                    }
                });
                if (ep.ero.mode === "") {
                    console.log("no mode set; updating to fits");
                    this.props.controlsStore.setParamsForEditPipe({
                        ero: {
                            mode: "fits"
                        }
                    });
                }

                // the selected mode was just synced from the server; update the ERO.
                // the validate() call that comes later will take care of the bandwidth validation

                if (uiParams.paths[ep.ero.mode].acceptable) {
                    uiParams.ero = {
                        acceptable: true,
                        message: "Path found.",
                        hops: uiParams.paths[ep.ero.mode].ero
                    };
                } else {
                    uiParams.ero = {
                        acceptable: false,
                        message: "No path found!",
                        hops: []
                    };
                }

                this.props.controlsStore.setParamsForEditPipe(uiParams);
            })
            .then(() => {
                this.validate();
            });
    }

    validationDispose = autorun(
        () => {
            this.validate();
        },
        { delay: 1000 }
    );

    validate() {
        let ep = this.props.controlsStore.editPipe;

        if (ep.paths.sync.loading || !ep.paths.sync.initialized) {
            return;
        }
        if (typeof ep.paths[ep.ero.mode] === "undefined") {
            console.log("undefined path for " + ep.ero.mode);
            return;
        }

        let aIngress = 0;
        let aEgress = 0;
        let zIngress = 0;
        let zEgress = 0;

        let pipe = this.props.designStore.findPipe(ep.pipeId);
        if (pipe != null) {
            let design = this.props.designStore.design;
            design.fixtures.map(f => {
                if (f.device === pipe.a) {
                    aIngress = aIngress + f.ingress;
                    aEgress = aEgress + f.egress;
                }
                if (f.device === pipe.z) {
                    zIngress = zIngress + f.ingress;
                    zEgress = zEgress + f.egress;
                }
            });
        }

        let azAvailable = ep.paths[ep.ero.mode].azAvailable;
        let zaAvailable = ep.paths[ep.ero.mode].zaAvailable;
        let azBaseline = ep.paths[ep.ero.mode].azBaseline;
        let zaBaseline = ep.paths[ep.ero.mode].zaBaseline;

        let params = {
            A_TO_Z: {
                fixturesIngress: aIngress,
                fixturesEgress: aEgress,
                available: azAvailable,
                baseline: azBaseline
            },
            Z_TO_A: {
                fixturesIngress: zIngress,
                fixturesEgress: zEgress,
                available: zaAvailable,
                baseline: zaBaseline
            }
        };
        if (ep.bwMode === "auto") {
            this.setBwControls("auto", aIngress, zIngress);
            params.A_TO_Z.bw = aIngress;
            params.Z_TO_A.bw = zIngress;
        }

        if (ep.A_TO_Z.bw > azAvailable) {
            params.A_TO_Z.validationText = "Larger than available";
            params.A_TO_Z.validationState = "error";
            params.A_TO_Z.acceptable = false;
        } else if (ep.A_TO_Z.bw > azBaseline) {
            params.A_TO_Z.validationText = "Larger than baseline";
            params.A_TO_Z.validationState = "error";
            params.A_TO_Z.acceptable = false;
        } else {
            params.A_TO_Z.validationText = "";
            params.A_TO_Z.validationState = "success";
            params.A_TO_Z.acceptable = true;
        }
        if (ep.Z_TO_A.bw > zaAvailable) {
            params.Z_TO_A.validationText = "Larger than available";
            params.Z_TO_A.validationState = "error";
            params.Z_TO_A.acceptable = false;
        } else if (ep.A_TO_Z.bw > zaBaseline) {
            params.Z_TO_A.validationText = "Larger than baseline";
            params.Z_TO_A.validationState = "error";
            params.Z_TO_A.acceptable = false;
        } else {
            params.Z_TO_A.validationText = "";
            params.Z_TO_A.validationState = "success";
            params.Z_TO_A.acceptable = true;
        }
        this.props.controlsStore.setParamsForEditPipe(params);
    }

    deletePipe = () => {
        let pipeId = this.props.controlsStore.editPipe.pipeId;
        this.props.designStore.deletePipe(pipeId);
        this.closeModal();
    };

    lockPipe = () => {
        const ep = this.props.controlsStore.editPipe;
        let params = {
            azBw: ep.A_TO_Z.bw,
            zaBw: ep.Z_TO_A.bw,
            protect: ep.protect,
            mode: ep.ero.mode,
            ero: ep.ero.hops
        };
        this.props.designStore.lockPipe(ep.pipeId, params);
        this.props.controlsStore.setParamsForEditPipe({ locked: true });
        this.closeModal();
    };

    unlockPipe = () => {
        const ep = this.props.controlsStore.editPipe;
        this.props.controlsStore.setParamsForEditPipe({
            locked: false,
            ero: { hops: [], acceptable: false }
        });
        this.props.designStore.unlockPipe(ep.pipeId);
    };

    onSelectModeChange = e => {
        const mode = e.target.value;
        const ep = this.props.controlsStore.editPipe;
        let params = {
            ero: {
                mode: mode,
                acceptable: false,
                message: "",
                hops: []
            }
        };

        if (mode === "manual") {
            // it was just changed, so clear everything
            params.ero.hops = [];
            params.ero.acceptable = false;
        } else {
            params.ero.hops = ep.paths[mode].ero;
            params.ero.acceptable = ep.paths[mode].acceptable;
            if (!params.ero.acceptable) {
                params.ero.message = "No path found";
            } else {
                params.ero.message = "";
            }
        }

        this.props.controlsStore.setParamsForEditPipe(params);
    };

    invalidBwReason = direction => {
        const ep = this.props.controlsStore.editPipe;
        return ep[direction].validationText;
    };

    isBwInvalid = direction => {
        const ep = this.props.controlsStore.editPipe;
        return !ep[direction].acceptable;
    };

    onBwInvalid = (direction, text) => {
        let params = {};
        params[direction] = {
            validationText: text,
            validationState: "error",
            acceptable: false
        };

        this.props.controlsStore.setParamsForEditPipe(params);
    };

    onBwValid = (direction, mbps) => {
        let params = {};
        let ep = this.props.controlsStore.editPipe;
        console.log(toJS(ep));

        params[direction] = {
            validationText: "",
            validationState: "success",
            acceptable: true,
            bw: mbps
        };
        this.props.controlsStore.setParamsForEditPipe(params);
    };

    setBwCtlRef = (direction, ref) => {
        this.bwCtlRefs[direction] = ref;
    };

    setBwControls(bwMode, azBw, zaBw) {
        let params = {
            bwMode: bwMode,
            A_TO_Z: {
                bw: azBw
            },
            Z_TO_A: {
                bw: zaBw
            }
        };
        if (this.bwCtlRefs["A_TO_Z"] != null) {
            this.bwCtlRefs["A_TO_Z"].value = azBw;
        }
        if (this.bwCtlRefs["Z_TO_A"] != null) {
            this.bwCtlRefs["Z_TO_A"].value = zaBw;
        }

        this.props.controlsStore.setParamsForEditPipe(params);
    }

    modeCheckboxClicked = e => {
        const mustBecomeAuto = e.target.checked;
        const ep = this.props.controlsStore.editPipe;
        if (mustBecomeAuto) {
            this.setBwControls("auto", ep.A_TO_Z.fixturesIngress, ep.Z_TO_A.fixturesIngress);
        } else {
            this.setBwControls("manual", 0, 0);
        }
    };

    render() {
        let ep = this.props.controlsStore.editPipe;
        let conn = this.props.controlsStore.connection;
        let pipe = this.props.designStore.findPipe(ep.pipeId);
        let design = this.props.designStore.design;

        if (pipe === null) {
            return null;
        }

        const acceptable = ep.A_TO_Z.acceptable && ep.Z_TO_A.acceptable && ep.ero.acceptable;
        const disableLockBtn = !acceptable;

        const eroControlModes = ["fits", "widestAZ", "widestSum", "widestZA"];

        const showEroControls = !ep.locked && eroControlModes.includes(ep.ero.mode);

        let showModal = this.props.modalStore.modals.get(modalName);

        let pipeTitle = (
            <span>
                {pipe.a} - {pipe.z}
            </span>
        );
        let aFixtures = [];
        let zFixtures = [];

        design.fixtures.map(f => {
            if (f.device === pipe.a) {
                aFixtures.push(f);
            }
            if (f.device === pipe.z) {
                zFixtures.push(f);
            }
        });

        let azDefaultBw = 0;
        let zaDefaultBw = 0;
        let inputMode = ep.bwMode;
        if (ep.locked) {
            inputMode = "locked";
        }
        if (inputMode === "auto") {
            azDefaultBw = ep.A_TO_Z.fixturesIngress;
            zaDefaultBw = ep.Z_TO_A.fixturesIngress;
        } else if (inputMode === "locked") {
            azDefaultBw = ep.A_TO_Z.bw;
            zaDefaultBw = ep.Z_TO_A.bw;
        }

        // console.log(toJS(ep));
        let modePath = ep.paths[ep.ero.mode];
        // console.log(toJS(modePath));
        let azAvailable = modePath.azAvailable;
        let zaAvailable = modePath.zaAvailable;
        let azBaseline = modePath.azBaseline;
        let zaBaseline = modePath.zaBaseline;
        let azWidest = ep.paths.widestAZ.azAvailable;
        let zaWidest = ep.paths.widestZA.zaAvailable;

        let azBwInput = (
            <PipeBwInput
                direction={"A_TO_Z"}
                available={azAvailable}
                baseline={azBaseline}
                defaultValue={azDefaultBw}
                widest={azWidest}
                refCallback={this.setBwCtlRef}
                getInvalidReason={this.invalidBwReason}
                isInvalid={this.isBwInvalid}
                eroMode={ep.ero.mode}
                inputMode={inputMode}
                onBwInvalid={this.onBwInvalid}
                onBwValid={this.onBwValid}
            />
        );

        let zaBwInput = (
            <PipeBwInput
                direction={"Z_TO_A"}
                available={zaAvailable}
                baseline={zaBaseline}
                defaultValue={zaDefaultBw}
                widest={zaWidest}
                refCallback={this.setBwCtlRef}
                getInvalidReason={this.invalidBwReason}
                isInvalid={this.isBwInvalid}
                eroMode={ep.ero.mode}
                inputMode={inputMode}
                onBwInvalid={this.onBwInvalid}
                onBwValid={this.onBwValid}
            />
        );

        const help = EditPipeModal.makePipeHelp();
        const bwHelp = EditPipeModal.makeBwHelp();

        let alert = null;
        let bwModeBox = (
            <FormGroup check>
                <Label check>
                    <Input
                        type="checkbox"
                        defaultChecked={ep.bwMode === "auto"}
                        onChange={this.modeCheckboxClicked}
                    />{" "}
                    Auto
                </Label>
            </FormGroup>
        );

        let bwControls = (
            <Container fluid={true}>
                <Row>
                    <Col xs="3">
                        <DeviceFixtures
                            fixtures={aFixtures}
                            junction={pipe.a}
                            ingress={ep.A_TO_Z.fixturesIngress}
                            egress={ep.A_TO_Z.fixturesEgress}
                        />
                    </Col>
                    <Col>
                        <Card style={{ width: "100%", height: "100%" }}>
                            <CardHeader className="p-1">Bandwidth {bwHelp}</CardHeader>
                            <CardBody>
                                <Container fluid={true}>
                                    <Row>
                                        <Col>{azBwInput}</Col>
                                        <Col>{zaBwInput}</Col>
                                        <Col>{bwModeBox}</Col>
                                    </Row>
                                </Container>
                            </CardBody>
                        </Card>
                    </Col>
                    <Col xs="3">
                        <DeviceFixtures
                            fixtures={zFixtures}
                            junction={pipe.z}
                            ingress={ep.Z_TO_A.fixturesIngress}
                            egress={ep.Z_TO_A.fixturesEgress}
                        />
                    </Col>
                </Row>
            </Container>
        );

        let pathControls = (
            <Container fluid={true}>
                <Row>
                    <Form inline>
                        <Col>
                            <strong>Path:</strong>
                        </Col>
                        <Col>
                            <FormGroup check>
                                <Label check>
                                    <Input
                                        type="checkbox"
                                        defaultChecked={ep.protect}
                                        disabled={ep.locked}
                                        onChange={this.protectClicked}
                                    />
                                    Protect
                                </Label>
                            </FormGroup>
                        </Col>
                        <ToggleDisplay show={!ep.locked}>
                            <Col>
                                <PathModeSelect onSelectModeChange={this.onSelectModeChange} />
                            </Col>
                        </ToggleDisplay>
                    </Form>
                </Row>
            </Container>
        );

        let eroControls = (
            <Container fluid={true}>
                <Row>
                    <Col>
                        <Nav tabs>
                            <NavItem>
                                <NavLink
                                    href="#"
                                    className={classnames({
                                        active: this.state.eroTab === "drawing"
                                    })}
                                    onClick={() => {
                                        this.setEroTab("drawing");
                                    }}
                                >
                                    Drawing
                                </NavLink>
                            </NavItem>
                            <NavItem>
                                <NavLink
                                    href="#"
                                    className={classnames({ active: this.state.eroTab === "ero" })}
                                    onClick={() => {
                                        this.setEroTab("ero");
                                    }}
                                >
                                    ERO
                                </NavLink>
                            </NavItem>
                        </Nav>
                        <TabContent activeTab={this.state.eroTab}>
                            <TabPane tabId="ero" title="ERO">
                                <ToggleDisplay show={!ep.locked}>
                                    <small>{ep.ero.message}</small>
                                </ToggleDisplay>
                                <ListGroup>
                                    {ep.ero.hops.map(urn => {
                                        return (
                                            <ListGroupItem className="p-1" key={urn}>
                                                <small>{urn}</small>
                                            </ListGroupItem>
                                        );
                                    })}
                                </ListGroup>
                            </TabPane>
                            <TabPane tabId="drawing" title="Drawing">
                                <EroDrawing containerId={"eroDrawing"} />
                            </TabPane>
                        </TabContent>
                    </Col>
                    <ToggleDisplay show={showEroControls}>
                        <Col>
                            <EroSelect />
                        </Col>
                    </ToggleDisplay>
                </Row>
            </Container>
        );

        if (!conn.schedule.locked) {
            alert = <Alert color="info">Schedule must be locked to edit pipe parameters.</Alert>;
            bwControls = null;
            pathControls = null;
            eroControls = null;
        }

        return (
            <Modal
                style={{ maxWidth: "75%" }}
                isOpen={showModal}
                toggle={this.toggleModal}
                fade={false}
                onExit={this.closeModal}
            >
                <ModalHeader className="p-2" toggle={this.toggleModal}>
                    Pipe controls for {pipeTitle} {help}
                </ModalHeader>
                <ModalBody>
                    {alert}
                    {bwControls}
                    {pathControls}

                    {eroControls}

                    <ButtonToolbar className="float-right">
                        <ConfirmModal
                            body="Are you ready to delete this pipe?"
                            header="Delete pipe"
                            uiElement={<Button color="warning">{"Delete"}</Button>}
                            onConfirm={this.deletePipe}
                        />{" "}
                        <Button
                            color="primary"
                            disabled={!disableLockBtn}
                            onClick={this.performPathfinding}
                        >
                            Find path
                        </Button>
                        <ToggleDisplay show={!ep.locked}>

                            <Button
                                color="primary"
                                disabled={disableLockBtn}
                                onClick={this.lockPipe}
                            >
                                Lock
                            </Button>
                        </ToggleDisplay>
                        <ToggleDisplay show={ep.locked}>
                            <Button color="warning" onClick={this.unlockPipe}>
                                Unlock
                            </Button>
                        </ToggleDisplay>
                    </ButtonToolbar>
                </ModalBody>
            </Modal>
        );
    }

    setEroTab = tab => {
        if (this.state.eroTab !== tab) {
            this.setState({
                eroTab: tab
            });
        }
    };

    static makePipeHelp() {
        const helpHeader = <span>Pipe controls</span>;
        const helpBody = (
            <span>
                <p>
                    Here you can edit the pipe parameters. Every pipe in a design must have locked
                    in A-Z and Z-A bandwidth quantities, as well as an Explicit Route Object (ERO).
                </p>
                <p>
                    Use the textboxes to input the bandwidth you want, and click "Lock" to lock the
                    values in.
                </p>
                <p>
                    As a convenience feature, if you type 'g' or 'G' in the bandwidth controls, that
                    character will be replaced by '000'.
                </p>
                <p>
                    Alternatively, you may click the "Delete" button to remove this pipe from the
                    design.
                </p>
            </span>
        );

        return (
            <span className="float-right">
                <HelpPopover
                    header={helpHeader}
                    body={helpBody}
                    placement="bottom"
                    popoverId="editPipeHelp"
                />
            </span>
        );
    }

    static makeBwHelp() {
        const bwHelpHeader = <span>Pipe controls</span>;
        const bwHelpBody = (
            <span>
                <p>
                    Here you can set your desired bandwidth in each direction. Type in the desired
                    number in Mbps. You can type in the 'g' character to add three 0s quickly.{" "}
                </p>
                <p>
                    Changing the desired bandwidth will cause the computed ERO to be recalculated. A
                    path might not be available for the new value.
                </p>
            </span>
        );

        return (
            <span className="float-right">
                <HelpPopover
                    header={bwHelpHeader}
                    body={bwHelpBody}
                    placement="bottom"
                    popoverId="pipeBwHelp"
                />
            </span>
        );
    }
}

export default EditPipeModal;
