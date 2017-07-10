import React, {Component} from 'react';
import {Grid, Row, Col} from 'react-bootstrap';
import {inject} from 'mobx-react';


import NavBar from '../components/navbar'
import TopologyMap from '../components/topologyMap';
import AddFixtureModal from '../components/addFixtureModal';
import EditFixtureModal from '../components/editFixtureModal';
import EditJunctionModal from '../components/editJunctionModal';
import EditPipeModal from '../components/editPipeModal';
import DisplayErrorsModal from '../components/displayErrorsModal';
import SandboxMap from '../components/sandboxMap';
import Sandbox from '../components/sandbox';
import SandboxControls from '../components/sandboxControls';
import SelectPort from '../components/selectPort';

@inject('controlsStore', 'mapStore', 'sandboxStore')
export default class NewConnectionApp extends Component {

    constructor(props) {
        super(props);
    }

    componentWillMount() {
        this.props.mapStore.setColoredNodes([]);
        this.props.mapStore.setColoredEdges([]);
        this.props.sandboxStore.clear();

        // TODO: a better clear
        this.props.controlsStore.setParamsForConnection({description: ''});
    }
    selectDevice = (device) => {
        this.props.controlsStore.setParamsForAddFixture({device: device});
        this.props.controlsStore.openModal('addFixture');
    };

    render() {

        return (
            <Grid fluid={true}>
                <Row>
                    <NavBar active='new'/>
                </Row>
                <Row>
                    <Col sm={4}>{' '}</Col>
                </Row>
                <Row>
                    <Col md={8} sm={8}>
                        <TopologyMap selectDevice={this.selectDevice}/>
                        <SandboxMap />
                    </Col>
                    <Col md={4} sm={4}>
                        <SelectPort/>
                        <SandboxControls />
                        <Sandbox />
                    </Col>
                </Row>
                <AddFixtureModal />
                <EditFixtureModal />
                <EditJunctionModal />
                <EditPipeModal />
                <DisplayErrorsModal />
            </Grid>
        );
    }

}
