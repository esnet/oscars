import React, { Component } from "react";

import { observer, inject } from "mobx-react";
import { action } from "mobx";
import BootstrapTable from "react-bootstrap-table-next";

import {
    Card,
    CardBody,
    CardHeader,
    Nav,
    NavItem,
    NavLink,
    ListGroup,
    ListGroupItem,
    TabPane,
    TabContent,
    Button,
    Collapse
} from "reactstrap";
import DetailsGeneral from "./detailsGeneral";
import PropTypes from "prop-types";
import myClient from "../../agents/client";
import Moment from "moment";
import classnames from "classnames";

import "react-bootstrap-table-next/dist/react-bootstrap-table2.min.css";
import JunctionInfo from "./junctionInfo";

// const format = "Y/MM/DD HH:mm:ss";

@inject("connsStore")
@observer
class DetailsInfo extends Component {
    constructor(props) {
        super(props);
    }

    componentWillMount() {
        this.refreshStatuses();
    }

    componentWillUnmount() {
        clearTimeout(this.refreshTimeout);
    }

    refreshStatuses = () => {

        this.props.connsStore.refreshCommands();
        this.props.connsStore.refreshAddresses();

        this.refreshTimeout = setTimeout(this.refreshStatuses, 5000); // update per 5 seconds
    };



    render() {
        const selected = this.props.connsStore.store.selected;
        if (typeof selected.type === "undefined") {
            return <DetailsGeneral />;
        }
        if (selected.type === "fixture") {
            return this.fixtureInfo();
        } else if (selected.type === "junction") {
            return <JunctionInfo />;
        } else if (selected.type === "pipe") {
            return this.pipeInfo();
        } else if (selected.type === "connection") {
            return <DetailsGeneral />;
        }
        return <h3>error!</h3>;
    }

    fixtureInfo() {
        const d = this.props.connsStore.store.selected.data;
        let policingText = "Soft";
        if (d.strict) {
            policingText = "Strict";
        }
        const info = [
            {
                k: "Port",
                v: d.portUrn
            },
            {
                k: "Vlan ID",
                v: d.vlan.vlanId
            },
            {
                k: "Ingress",
                v: d.ingressBandwidth + " Mbps"
            },
            {
                k: "Egress",
                v: d.egressBandwidth + " Mbps"
            },
            {
                k: "Policing",
                v: policingText
            }
        ];

        const columns = [
            {
                dataField: "k",
                text: "Field",
                headerTitle: true
            },
            {
                dataField: "v",
                text: "Value",
                headerTitle: true
            }
        ];
        return (
            <Card>
                <CardHeader className="p-1">Fixture</CardHeader>
                <CardBody>
                    <BootstrapTable
                        tableHeaderClass={"hidden"}
                        keyField="k"
                        data={info}
                        columns={columns}
                        bordered={false}
                    />
                </CardBody>
            </Card>
        );
    }





    pipeInfo() {
        const d = this.props.connsStore.store.selected.data;

        let ero = (
            <ListGroup>
                <ListGroupItem active>ERO</ListGroupItem>
                {d.azERO.map(entry => {
                    return <ListGroupItem key={entry.urn}>{entry.urn}</ListGroupItem>;
                })}
            </ListGroup>
        );

        let protectTxt = "No";
        if (d.protect) {
            protectTxt = "Yes";
        }
        const info = [
            {
                k: "A",
                v: d.a
            },
            {
                k: "Z",
                v: d.z
            },
            {
                k: "A-Z Bandwidth",
                v: d.azBandwidth + " Mbps"
            },
            {
                k: "Z-A Bandwidth",
                v: d.zaBandwidth + " Mbps"
            },
            {
                k: "Protect path?",
                v: protectTxt
            }
        ];

        const columns = [
            {
                dataField: "k",
                text: "Field",
                headerTitle: true
            },
            {
                dataField: "v",
                text: "Value",
                headerTitle: true
            }
        ];

        return (
            <Card>
                <CardHeader className="p-1">Pipes</CardHeader>
                <CardBody>
                    <BootstrapTable keyField="k" columns={columns} data={info} bordered={false} />
                    <hr />
                    {ero}
                </CardBody>
            </Card>
        );
    }
}

DetailsInfo.propTypes = {
    refresh: PropTypes.func.isRequired
};

export default DetailsInfo;
