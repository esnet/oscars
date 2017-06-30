import React, {Component} from 'react';
import {observer, inject} from 'mobx-react';
import {Modal, Button, Form, FormGroup, FormControl, ControlLabel, Checkbox, Grid, Row, Col} from 'react-bootstrap';


@inject('sandboxStore')
@observer
export default class FixtureParamsModal extends Component {
    constructor(props) {
        super(props);
        this.closeModal = this.closeModal.bind(this);
        this.deleteFixture = this.deleteFixture.bind(this);
        this.updateFixture = this.updateFixture.bind(this);
        this.toggleSymmetrical = this.toggleSymmetrical.bind(this);
        this.onIngressBwChange = this.onIngressBwChange.bind(this);
    }

    state = {
        symmetrical: true,
    };

    closeModal() {
        this.props.sandboxStore.closeModal('fixture');
    }
    
    updateFixture() {
        console.log('udating')
    }

    deleteFixture() {
        this.props.sandboxStore.deleteFixture(this.props.sandboxStore.selection.fixture);
        this.closeModal();
    }

    toggleSymmetrical() {
        this.setState({
            symmetrical: !this.state.symmetrical
        });
    }

    onIngressBwChange(e) {
        if (this.state.symmetrical) {
            this.egress.value = e.target.value;
        }
    }

    render() {
        let fixture = this.props.sandboxStore.selection.fixture;
        let showModal = this.props.sandboxStore.modals.get('fixture');

        return (
            <div>
                <Modal show={showModal} onHide={this.closeModal}>
                    <Modal.Header closeButton>
                        <Modal.Title>{fixture}</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        <form>
                            <Grid>
                                <Row>
                                    <Col md={3} sm={3}>
                                        <FormGroup controlId="vlan">
                                            <ControlLabel>VLAN</ControlLabel>
                                            {' '}
                                            <FormControl type="text" placeholder="2000-2999"/>
                                        </FormGroup>
                                    </Col>
                                    <Col md={3} sm={3}>
                                        <FormGroup controlId="ingress">
                                            <ControlLabel>Bandwidth:</ControlLabel>
                                            <FormControl type="text" placeholder="0-100000"
                                                         onChange={this.onIngressBwChange}/>
                                        </FormGroup>
                                        <FormGroup controlId="symmetrical">
                                            <Checkbox defaultChecked={this.state.symmetrical} inline
                                                      onChange={this.toggleSymmetrical}>
                                                Symmetrical
                                            </Checkbox>
                                        </FormGroup>
                                        <FormGroup controlId="egress">
                                            <ControlLabel>Egress</ControlLabel>
                                            <FormControl inputRef={ref => {
                                                this.egress = ref;
                                            }}
                                                         disabled={this.state.symmetrical}

                                                         type="text" placeholder="0-10000"/>
                                        </FormGroup>
                                    </Col>
                                </Row>
                            </Grid>
                            <Button onClick={this.updateFixture}>Update</Button>
                            <Button onClick={this.deleteFixture}>Delete</Button>
                        </form>
                    </Modal.Body>
                    <Modal.Footer>
                        <Button onClick={this.closeModal}>Close</Button>
                    </Modal.Footer>
                </Modal>


            </div>
        );
    }
}