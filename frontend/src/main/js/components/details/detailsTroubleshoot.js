import { action } from "mobx";
import { inject, observer } from "mobx-react";
import React, { Component } from "react";
import Moment from "moment";
import {
    Card,
    CardHeader,
    CardBody,
    Button,
    Form,
    UncontrolledAccordion,
    AccordionItem,
    AccordionHeader,
    AccordionBody
} from "reactstrap";

import myClient from "../../agents/client";

@inject("connsStore")
@observer
class DetailsTroubleshoot extends Component {
    constructor(props) {
        super(props);
        this.state = {
            loading: true
        };
    }

    componentWillMount() {
        const connectionId = this.props.connsStore.store.current.connectionId;
        this.updateMacInfo(connectionId);
    }

    componentWillUnmount() {
        // this.props.connsStore.clearCurrent();
    }

    refresh = () => {
        const connectionId = this.props.connsStore.store.current.connectionId;
        this.updateMacInfo(connectionId);
    };

    updateMacInfo = connectionId => {
        let macInfoRequest = {
            'connection-id': connectionId
        }

        myClient.submitWithToken("POST", "/api/mac/info", macInfoRequest).then(
            action(response => {
                let macInfo = JSON.parse(response);
                this.props.connsStore.setMacInfo(macInfo);
                this.setState({loading: false});
            })
        );
    };


    render() {
        let cs = this.props.connsStore;
        const macInfo = cs.store.macInfo;
        let contents = <div>Loading..</div>;

        if (!this.state.loading) {
            let macLearning = <div>No mac learning data loaded</div>
            if (macInfo['connection-id']) {
                macLearning = <UncontrolledAccordion>
                    {
                        macInfo['results'].map( (result, idx) => {
                            let message = result['fdb'];
                            let timestamp = 'unknown';
                            if (!result['status']) {
                                message = result['error-message']
                            }
                            if (result['timestamp']) {
                                timestamp = Moment(result['timestamp']).fromNow();
                            }
                            let headerString = result['device']+' ('+timestamp+')';
                            return <AccordionItem key={idx}>
                                    <AccordionHeader targetId={idx}>{headerString}</AccordionHeader>
                                    <AccordionBody accordionId={idx}>
                                        <pre>{message}</pre>
                                    </AccordionBody>
                                </AccordionItem>
                        })
                    }
                </UncontrolledAccordion>
            }
            contents = <Form inline onSubmit={e => { e.preventDefault();}}>
                {macLearning}
                <Button  className="float-right btn-sm" onClick={this.refresh}>Refresh</Button>
            </Form>
        }

        return <Card>
            <CardHeader>Troubleshooting</CardHeader>
            <CardBody>
                <p>MAC learning</p>
                {contents}
            </CardBody>
        </Card>



            }
}

export default DetailsTroubleshoot;
