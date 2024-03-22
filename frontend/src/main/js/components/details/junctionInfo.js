import Moment from "moment/moment";
import {
    Button,
    Card,
    CardBody,
    CardHeader,
    Collapse, Form,
    FormGroup, Input, InputGroup, Label, ListGroup, ListGroupItem,
    Nav,
    NavItem,
    NavLink,
    TabContent,
    TabPane
} from "reactstrap";
import classnames from "classnames";
import React, {Component} from "react";
import {inject, observer} from "mobx-react";
import Octicon from "react-octicon";
import myClient from "../../agents/client";

@inject("connsStore")
@observer
class JunctionInfo extends Component {
    constructor(props) {
        super(props);
    }

    componentWillMount() {
        this.setState({
            junctionTab: "commands",
            historyId: null,
            ipAddressInput: "",
            commands: {}
        });
    }

    setJunctionTab = tab => {
        if (this.state.junctionTab !== tab) {
            this.setState({
                junctionTab: tab
            });
        }
    };

    handleIpAddressChange = (e) => {
        this.setState({ipAddressInput: e.target.value})
    }

    toggleCommandCollapse = (urn, type) => {
        //       console.log('toggling '+urn+' '+type);
        //        console.log(this.state);
        let newSt = {};
        if (urn in this.state.commands) {
            newSt[urn] = {};
            if (type in this.state.commands[urn]) {
                newSt[urn][type] = !this.state.commands[urn][type];
            } else {
                newSt[urn][type] = true;
            }
        } else {
            newSt[urn] = {};
            newSt[urn][type] = true;
        }
        this.setState({commands: newSt});
        //        console.log(this.state);
    };

    toggleHistoryCollapse = historyId => {
        if (this.state.historyId === historyId) {
            this.setState({historyId: null});
        } else {
            this.setState({historyId: historyId});
        }
    };
    handleAddClicked = (connectionId, device, ipAddress) => {
        let addRequest = {
            'connection-id': connectionId,
            'device': device,
            'ip-address': ipAddress,
        }
        console.log(addRequest);

        myClient.submitWithToken("POST", "/api/interface/add", addRequest);
    }
    handleDeleteClicked = (connectionId, device, ipAddress) => {
        let deleteRequest = {
            'connection-id': connectionId,
            'device': device,
            'ip-address': ipAddress,
        }
        console.log(deleteRequest);

        myClient.submitWithToken("POST", "/api/interface/remove", deleteRequest);

    }


    render() {
        const selected = this.props.connsStore.store.selected;
        let deviceUrn = selected.data.deviceUrn;
        let history = this.props.connsStore.store.history;
        let addresses = this.props.connsStore.store.addresses;

        const conn = this.props.connsStore.store.current;
        const editAllowed = conn.phase === "RESERVED" && conn.southbound === 'NSO';
        let addForm = <></>

        if (editAllowed) {
            addForm = <Form>
                <FormGroup row>
                    <InputGroup>

                        <Label sm={2}>Add IP address</Label>
                        <Input type="text" onChange={this.handleIpAddressChange} defaultValue=""/>
                        <Button color="success"  onClick={() => this.handleAddClicked(conn.connectionId, deviceUrn, this.state.ipAddressInput)}>Add</Button>
                    </InputGroup>
                </FormGroup>
            </Form>
        }



        return (
            <Card>
                <CardHeader className="p-1">{deviceUrn}</CardHeader>
                <CardBody>
                    <div>
                        <Nav tabs>
                            <NavItem>
                                <NavLink
                                    href="#"
                                    className={classnames({
                                        active: this.state.junctionTab === "commands"
                                    })}
                                    onClick={() => {
                                        this.setJunctionTab("commands");
                                    }}
                                >
                                    Config commands
                                </NavLink>
                            </NavItem>
                            <NavItem>
                                <NavLink
                                    href="#"
                                    className={classnames({
                                        active: this.state.junctionTab === "ip-addresses"
                                    })}
                                    onClick={() => {
                                        this.setJunctionTab("ip-addresses");
                                    }}
                                >
                                    IP addresses
                                </NavLink>
                            </NavItem>
                            <NavItem>
                                <NavLink
                                    href="#"
                                    className={classnames({
                                        active: this.state.junctionTab === "history"
                                    })}
                                    onClick={() => {
                                        this.setJunctionTab("history");
                                    }}
                                >
                                    Config History
                                </NavLink>
                            </NavItem>
                        </Nav>
                        <TabContent activeTab={this.state.junctionTab}>
                            <TabPane tabId="commands" title="Router commands">
                                {this.props.connsStore.store.commands.map(c => {
                                    if (c.deviceUrn === selected.data.deviceUrn) {
                                        let isOpen = true;
                                        if (deviceUrn in this.state.commands) {
                                            if (c.type in this.state.commands[deviceUrn]) {
                                                isOpen = this.state.commands[deviceUrn][c.type];
                                            } else {
                                                isOpen = false;
                                            }
                                        } else {
                                            isOpen = false;
                                        }

                                        return (
                                            <Card key={c.type}>
                                                <CardHeader
                                                    className="p-1"
                                                    onClick={() =>
                                                        this.toggleCommandCollapse(
                                                            c.deviceUrn,
                                                            c.type
                                                        )
                                                    }
                                                >
                                                    <NavLink href="#">{c.type}</NavLink>
                                                </CardHeader>
                                                <CardBody>
                                                    <Collapse isOpen={isOpen}>
                                                        <pre>{c.contents}</pre>
                                                    </Collapse>
                                                </CardBody>
                                            </Card>
                                        );
                                    } else {
                                        return null;
                                    }
                                })}
                            </TabPane>
                            <TabPane tabId="ip-addresses" title="IP addresses">
                                {
                                    addresses.map((entry) => {
                                        let items = []
                                        if (entry.device === deviceUrn) {
                                            entry['ip-address'].map(ipAddress => {
                                                let item = (
                                                    <ListGroupItem className="p-1" key={ipAddress}>{ipAddress}</ListGroupItem>
                                                );
                                                if (editAllowed) {
                                                    item = (
                                                        <ListGroupItem className="p-1" key={ipAddress}>{ipAddress}             <Octicon
                                                            name="trashcan"
                                                            onClick={() => this.handleDeleteClicked(conn.connectionId, deviceUrn, ipAddress)}
                                                            className="float-right"
                                                            style={{ height: "16px", width: "16px" }}
                                                        /> </ListGroupItem>
                                                    );
                                                }
                                                items.push(item);
                                            })

                                        }
                                        return <ListGroup className="m-0 p-0" key={deviceUrn}>{items}</ListGroup>;
                                    }
                                )}
                                {addForm}

                            </TabPane>

                            <TabPane tabId="history" title="Config history">
                                {history.map(h => {
                                    const format = "Y/MM/DD HH:mm";
                                    if (h.deviceUrn === selected.data.deviceUrn) {
                                        let isOpen = this.state.historyId === h.id;
                                        let tmpVersion = h.templateVersion;
                                        const when = Moment(h.date * 1000);

                                        const humanWhen =
                                            when.format(format) + " (" + when.fromNow() + ")";

                                        return (
                                            <Card key={h.id}>
                                                <CardHeader
                                                    className="p-1"
                                                    onClick={() => this.toggleHistoryCollapse(h.id)}
                                                >
                                                    <NavLink href="#">
                                                        {h.type} - {humanWhen} - (v. {tmpVersion})

                                                    </NavLink>
                                                </CardHeader>
                                                <CardBody>
                                                    <Collapse isOpen={isOpen}>
                                                        <pre>{h.output}</pre>
                                                    </Collapse>
                                                </CardBody>
                                            </Card>
                                        );
                                    } else {
                                        return null;
                                    }
                                })}
                            </TabPane>
                        </TabContent>
                    </div>
                </CardBody>
            </Card>
        );
    }


}


export default JunctionInfo;