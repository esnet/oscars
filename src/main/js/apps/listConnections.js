import React, {Component} from 'react';
import {Row, Col} from 'reactstrap';
import {inject} from 'mobx-react';


import ConnectionsList from '../components/connectionsList';


@inject('mapStore', 'commonStore')
export default class ListConnectionsApp extends Component {

    constructor(props) {
        super(props);
    }

    componentWillMount() {
        this.props.mapStore.setColoredNodes([]);
        this.props.mapStore.setColoredEdges([]);
        this.props.commonStore.setActiveNav('list');
    }

    render() {
        return (
            <Row>
                <Col md={{size: 10, offset: 1}}><ConnectionsList/></Col>

            </Row>
        );
    }

}
