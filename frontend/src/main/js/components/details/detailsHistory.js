import { action } from "mobx";
import { inject, observer } from "mobx-react";
import Moment from "moment";
import React, { Component } from "react";
import ReactTable from "react-table";
import "react-table/react-table.css";

import myClient from "../../agents/client";

@inject("connsStore")
@observer
class DetailsHistory extends Component {
    constructor(props) {
        super(props);
        this.state = {
            loading: true
        };
    }
    
    componentWillMount() {
        const pathConnectionId = this.props.connsStore.store.current.connectionId;
        this.updateList(pathConnectionId);
    }

    componentWillUnmount() {
        // this.props.connsStore.clearCurrent();
    }

    refresh = () => {
        const pathConnectionId = this.props.connsStore.store.current.connectionId;
        this.updateList(pathConnectionId);
    };

    updateList = connectionId => {
        myClient.submitWithToken("GET", "/api/log/conn/" + connectionId).then(
            action(response => {
                let eventLog = JSON.parse(response);
                this.props.connsStore.setEventLog(eventLog);
                this.setState({loading: false});
            })
        );
    };

    columns = [
        {
            accessor: "at",
            Header: props => (
                <div>
                    <br />
                    <b>Timestamp</b>
                    <br />
                    <br />
                </div>
            )
          },
          {
            accessor: "description",
            Header: props => (
                <div>
                    <br />
                    <b>Description</b>
                    <br />
                    <br />
                </div>
            )
          }
    ];

    render() {
        let cs = this.props.connsStore;
        const eventlog = cs.store.eventLog;

        if (typeof eventlog === 'undefined') {
            console.log('undefined event log');
            return (
                <div>
                    Waiting for event log.
                </div>
            )
        } else {
            let rows = [];
            if (typeof eventlog.events !== 'undefined') {
                eventlog.events.slice().reverse().map(c => {
                    console.log(c)
                    let row = {
                        at: Moment(c.occurrence).format('Y/MM/DD HH:mm:ss'),
                        type: c.type,
                        description: c.description
                    };
                    rows.push(row);
                });
            }

            return (
                <ReactTable
                    data={rows}
                    columns={this.columns}
                    minRows={3}
                    className="-striped -highlight"
                    getTrProps={(state, rowInfo, column) => {
                        return {
                          style: {
                            padding: '10px'
                          }
                        }
                      }}
                />
            );
        }
    }
}

export default DetailsHistory;
