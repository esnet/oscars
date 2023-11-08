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
        const pathConnectionId = this.props.connsStore.store.current.connectionId;
        this.updateMacInfo(pathConnectionId);
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
                        macInfo['results'].map( result => {
                            let info = result['fdb'];
                            if (!result['status']) {
                                info = result['error-message']
                            }
                            return (
                                <AccordionItem>
                                    <AccordionHeader targetId={result['device']}>{result['device']} ({Moment(result['timestamp']).fromNow()})</AccordionHeader>
                                    <AccordionBody accordionId={result['device']}>
                                        <pre>{info}</pre>
                                    </AccordionBody>
                                </AccordionItem>
                            )
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
