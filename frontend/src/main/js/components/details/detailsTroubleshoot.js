import {action} from "mobx";
import {inject, observer} from "mobx-react";
import React, {Component} from "react";
import Moment from "moment";
import {
    Card,
    CardHeader,
    CardBody,
    Button,
    Form,
    AccordionItem,
    AccordionHeader,
    AccordionBody, Accordion, ListGroupItem, ListGroup
} from "reactstrap";

import myClient from "../../agents/client";

@inject("connsStore")
@observer
class DetailsTroubleshoot extends Component {
    constructor(props) {
        super(props);
        this.state = {
            loading: true,
            open: ''
        };
    }

    componentWillMount() {
        const connectionId = this.props.connsStore.store.current.connectionId;
        this.updateMacInfo(connectionId);
        this.updateOpStateInfo(connectionId);

    }

    refresh = () => {
        const connectionId = this.props.connsStore.store.current.connectionId;
        this.updateMacInfo(connectionId);
        this.updateOpStateInfo(connectionId);
    };

    updateMacInfo = connectionId => {
        let macInfoRequest = {
            'connection-id': connectionId
        }
        if (this.props.connsStore.store.current.phase !== 'RESERVED') {
            return;
        }
        this.setState({loading: true})

        myClient.submitWithToken("POST", "/api/mac/info", macInfoRequest).then(
            action(response => {
                let macInfo = JSON.parse(response);
                this.props.connsStore.setMacInfo(macInfo);
                this.setState({loading: false});
            })
        );
    };

    updateOpStateInfo = connectionId => {
        let opStateInfoRequest = {
            'connection-id': connectionId
        }
        if (this.props.connsStore.store.current.phase !== 'RESERVED') {
            return;
        }
        this.setState({loading: true})

        myClient.submitWithToken("POST", "/api/operational-state/info", opStateInfoRequest).then(
            action(response => {
                let opStateInfo = JSON.parse(response);
                this.props.connsStore.setOpStateInfo(opStateInfo);
                this.setState({loading: false});
            })
        );
    };

    toggle = (id) => {
        if (this.state.open === id) {
            this.setState({open: ''})
        } else {
            this.setState({open: id})
        }
    };

    render() {
        let cs = this.props.connsStore;
        const macInfo = cs.store.macInfo;
        const opStateInfo = cs.store.opStateInfo;

        let contents = <div>Loading..</div>;
        if (cs.store.current.phase !== 'RESERVED') {
            contents = <pre>Only available for RESERVED connections</pre>
        } else if (!this.state.loading) {

            let macLearning = <div>No mac learning data loaded</div>
            let opState = <div>No operational state learned</div>

            if (macInfo['connection-id']) {
                macLearning = <>
                    <p>MAC learning</p>
                    <Accordion open={this.state.open} toggle={this.toggle}>
                        {
                            macInfo['results'].map((result, idx) => {
                                let message = result['fdb'];
                                let timestamp = 'unknown';
                                if (!result['status']) {
                                    message = result['error-message']
                                }
                                if (result['timestamp']) {
                                    timestamp = Moment(result['timestamp']).fromNow();
                                }
                                let headerString = result['device'] + ' (' + timestamp + ')';
                                return <AccordionItem key={idx}>
                                    <AccordionHeader targetId={idx + ""}>{headerString}</AccordionHeader>
                                    <AccordionBody accordionId={idx + ""}>
                                        <pre>{message}</pre>
                                    </AccordionBody>
                                </AccordionItem>
                            })
                        }
                    </Accordion>
                </>
            }

            if (opStateInfo['connection-id']) {
                opState = <>
                    <p>Overall state: {opStateInfo['state']}</p>
                    <p>Endpoints</p>
                    <ListGroup>
                        {opStateInfo['endpoints'].map(e => {
                            let key = e.device + ':' + e.port
                            return (
                                <ListGroupItem className="p-1 m-1"
                                               key={key}>{key}.{e['vlan-id']} : {e['state']}</ListGroupItem>
                            );
                        })}
                    </ListGroup>
                    <p>Tunnels</p>
                    <ListGroup>
                        {opStateInfo['tunnels'].map(t => {
                            let key = t.device + ' => ' + t.remote
                            return (
                                <ListGroupItem className="p-1 m-1" key={key}>{key} {t['state']}</ListGroupItem>
                            );
                        })}
                    </ListGroup>
                </>

            }


            contents = <Form inline="true" onSubmit={e => {
                e.preventDefault();
            }}>
                {macLearning}
                {opState}
                <Button className="float-right btn-sm" onClick={this.refresh}>Refresh</Button>
            </Form>
        }

        return <Card>
            <CardHeader>Troubleshooting</CardHeader>
            <CardBody>
                {contents}
            </CardBody>
        </Card>


    }
}

export default DetailsTroubleshoot;
