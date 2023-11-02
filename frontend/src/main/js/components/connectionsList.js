import Moment from "moment";
import { size } from "lodash-es";
import { toJS, autorun } from "mobx";
import React, { Component } from "react";
import { observer, inject } from "mobx-react";
import { withRouter, Link } from "react-router-dom";
import { ListGroupItem, ListGroup } from "reactstrap";

import transformer from "../lib/transform";
import myClient from "../agents/client";

import ReactTable from "react-table";
import "react-table/react-table.css";

@observer
class HrefIdFormatter extends Component {
    render() {
        const href = "/pages/details/" + this.props.row.connectionId;
        return <Link to={href}>{this.props.row.connectionId}</Link>;
    }
}

@observer
class DescFormatter extends Component {
    render() {
        let tagList = null;
        if ("tags" in this.props.row && size(this.props.row.tags) > 0) {
            let i = 0;
            let items = [];
            let key = this.props.row.connectionId + ":header";

            items.push(
                <ListGroupItem color="info" className="p-1" key={key}>
                    <small>Tags</small>
                </ListGroupItem>
            );

            for (let tag of this.props.row.tags) {
                console.log(tag);
                key = this.props.row.connectionId + ":" + i;
                let item = (
                    <ListGroupItem className="p-1" key={key}>
                        <small>
                            {tag.category}: {tag.contents}
                        </small>
                    </ListGroupItem>
                );
                items.push(item);
                i++;
            }
            tagList = <ListGroup className="m-0 p-0">{items}</ListGroup>;
        }

        return (
            <div>
                {this.props.row.description}
                {tagList}
            </div>
        );
    }
}

@observer
class PortsFormatter extends Component {
    render() {
        let added = [];
        let result = this.props.row.fixtures.map(f => {
            let key = this.props.row.connectionId + ":" + f.portUrn;
            if (added.includes(key)) {
                return null;
            } else {
                added.push(key);
                return (
                    <ListGroupItem className="p-1" key={key}>
                        <small>{f.portUrn}</small>
                    </ListGroupItem>
                );
            }
        });
        return <ListGroup className="m-0 p-0">{result}</ListGroup>;
    }
}

@observer
class VlansFormatter extends Component {
    render() {
        let added = [];
        let result = this.props.row.fixtures.map(f => {
            let key = this.props.row.connectionId + ":" + f.vlan.vlanId;
            if (added.includes(key)) {
                return null;
            } else {
                added.push(key);
                return (
                    <ListGroupItem className="m-1 p-1" key={key}>
                        <small>{f.vlan.vlanId}</small>
                    </ListGroupItem>
                );
            }
        });
        return <ListGroup className="m-0 p-0">{result}</ListGroup>;
    }
}

@inject("controlsStore", "connsStore", "mapStore", "modalStore", "commonStore")
@observer
class ConnectionsList extends Component {
    constructor() {
        super();
        this.state = {
            loading: true
        };
    }

    componentWillMount() {
        this.updateList();
    }

    componentWillUnmount() {
        this.disposeOfUpdateList();
    }

    disposeOfUpdateList = autorun(
        () => {
            this.updateList();
        },
        { delay: 1000 }
    );

    // Whenever the table model changes, or the user sorts or changes pages,
    // this method gets called and passed the current table model.
    fetchData = (state, instance) => {
        this.setState({ loading: true });

        this.props.connsStore.setFilter({
            sizePerPage: state.pageSize,
            page: state.page,
            filtered: state.filtered
        });

        this.updateList();
    };

    filterData = (filter, filtered) => {
        if (filtered.length > 0) {
            for (let key in filtered) {
                let itemKey = filtered[key]["id"];
                let itemValue = filtered[key]["value"];

                if (itemKey === "ports") {
                    filter[itemKey] = [itemValue];
                } else if (itemKey === "fixtures") {
                    filter["vlans"] = [itemValue];
                } else if (itemKey === "phase") {
                    filter[itemKey] = itemValue.toLocaleUpperCase();
                } else {
                    filter[itemKey] = itemValue;
                }
            }
        }
        return filter;
    };

    updateList = () => {
        let csFilter = this.props.connsStore.filter;
        let filter = {};
        csFilter.criteria.map(c => {
            filter[c] = this.props.connsStore.filter[c];
        });

        filter.page = csFilter.page + 1;
        filter.sizePerPage = csFilter.sizePerPage;
        filter.phase = csFilter.phase;

        // If any filters are applied, apply the filter here
        filter = this.filterData(filter, csFilter.filtered);

        myClient.submit("POST", "/api/conn/list", filter).then(
            successResponse => {
                let result = JSON.parse(successResponse);
                let conns = result.connections;

                this.props.connsStore.setFilter({
                    totalPages: Math.ceil(result.totalSize / result.sizePerPage)
                });

                conns.map(conn => {
                    transformer.fixSerialization(conn);
                });

                this.props.connsStore.updateList(conns);

                this.setState({ loading: false });
            },
            failResponse => {
                this.props.commonStore.addAlert({
                    id: new Date().getTime(),
                    type: "danger",
                    headline: "Error loading connection list",
                    message: failResponse.status + " " + failResponse.statusText
                });

                console.log("Error: " + failResponse.status + " - " + failResponse.statusText);
            }
        );
    };

    columns = [
        {
            accessor: "connectionId",
            Header: props => (
                <div>
                    <br />
                    <b>Connection ID</b>
                    <br />
                    <br />
                </div>
            ),
            Cell: d => <HrefIdFormatter {...d} />,
            filterMethod: (filter, row) => {
                let upperCaseId = row[filter.id].toUpperCase();
                let upperCaseValue = filter.value.toUpperCase();
                return upperCaseId.includes(upperCaseValue);
            },
            Filter: ({ filter, onChange }) => (
                <input
                    type="text"
                    placeholder="Enter Connection ID"
                    value={filter ? filter.value : ""}
                    onChange={event => onChange(event.target.value)}
                    style={{ fontStyle: "italic", width: "100%" }}
                />
            )
        },
        {
            accessor: "description",
            Header: props => (
                <div>
                    <br />
                    <b>Description and Tags</b>
                    <br />
                </div>
            ),
            Cell: d => <DescFormatter {...d} />,
            filterMethod: (filter, row) => {
                let upperCaseDesc = row[filter.id].toUpperCase();
                let upperCaseValue = filter.value.toUpperCase();
                return upperCaseDesc.includes(upperCaseValue);
            },
            Filter: ({ filter, onChange }) => (
                <input
                    type="text"
                    placeholder="Enter Description"
                    value={filter ? filter.value : ""}
                    onChange={event => onChange(event.target.value)}
                    style={{ fontStyle: "italic", width: "100%" }}
                />
            )
        },
        {
            accessor: "phase",
            Header: props => (
                <div>
                    <br />
                    <b>Phase</b>
                    <br />
                </div>
            ),
            filterMethod: (filter, row) => {
                if (filter.value === "any") {
                    return true;
                }
                if (filter.value === "reserved") {
                    return row[filter.id] === "RESERVED";
                }
                return row[filter.id] === "ARCHIVED";
            },
            Filter: ({ filter, onChange }) => (
                <select
                    onChange={event => onChange(event.target.value)}
                    style={{ width: "100%" }}
                    value={filter ? filter.value : "reserved"}
                >
                    <option value="any">Any</option>
                    <option value="reserved">Reserved</option>
                    <option value="archived">Archived</option>
                </select>
            )
        },
        {
            accessor: "username",
            Header: props => (
                <div>
                    <br />
                    <b>User</b>
                    <br />
                </div>
            ),
            filterMethod: (filter, row) => {
                let upperCaseUser = row[filter.id].toUpperCase();
                let upperCaseValue = filter.value.toUpperCase();
                return upperCaseUser.includes(upperCaseValue);
            },
            Filter: ({ filter, onChange }) => (
                <input
                    type="text"
                    placeholder="Enter User"
                    value={filter ? filter.value : ""}
                    onChange={event => onChange(event.target.value)}
                    style={{ fontStyle: "italic", width: "100%" }}
                />
            )
        },
        {
            id: "ports",
            Header: props => (
                <div>
                    <br />
                    <b>Ports</b>
                    <br />
                </div>
            ),
            Cell: props => <PortsFormatter {...props} />,
            filterMethod: (filter, row) => {
                let filtered = false;
                row.fixtures.map(f => {
                    let vlanId = String(f.portUrn).toLocaleUpperCase();
                    let value = String(filter.value).toLocaleUpperCase();
                    if (vlanId.includes(value)) {
                        filtered = true;
                    }
                });
                return filtered;
            },
            Filter: ({ filter, onChange }) => (
                <input
                    type="text"
                    placeholder="Enter Ports"
                    value={filter ? filter.value : ""}
                    onChange={event => onChange(event.target.value)}
                    style={{ fontStyle: "italic", width: "100%" }}
                />
            )
        },
        {
            accessor: "fixtures",
            Header: props => (
                <div>
                    <br />
                    <b>VLANs</b>
                    <br />
                </div>
            ),
            Cell: d => <VlansFormatter {...d} />,
            filterMethod: (filter, row) => {
                let filtered = false;
                row.fixtures.map(f => {
                    let vlanId = String(f.vlan.vlanId);
                    let value = String(filter.value);
                    if (vlanId.includes(value)) {
                        filtered = true;
                    }
                });
                return filtered;
            },
            Filter: ({ filter, onChange }) => (
                <input
                    type="text"
                    placeholder="Enter VLANs"
                    value={filter ? filter.value : ""}
                    onChange={event => onChange(event.target.value)}
                    style={{ fontStyle: "italic", width: "100%" }}
                />
            )
        },
        {
            accessor: "southbound",
            Header: props => (
                <div>
                    <br />
                    <b>Southbound</b>
                    <br />
                </div>
            ),
            filterMethod: (filter, row) => {
                if (filter.value === "any") {
                    return true;
                }
                if (filter.value === "NSO") {
                    return row[filter.id] === "NSO";
                }
                if (filter.value === "RANCID") {
                    return row[filter.id] === "RANCID";
                }
            },
            Filter: ({ filter, onChange }) => (
                <select
                    onChange={event => onChange(event.target.value)}
                    style={{ width: "100%" }}
                    value={filter ? filter.value : "any"}
                >
                    <option value="any">Any</option>
                    <option value="NSO">NSO</option>
                    <option value="RANCID">RANCID</option>
                </select>
            )
        }
    ];

    render() {
        let cs = this.props.connsStore;
        const format = "Y/MM/DD HH:mm";
        const { loading } = this.state;

        let rows = [];

        cs.store.conns.map(c => {
            const beg = Moment(c.archived.schedule.beginning * 1000);
            const end = Moment(c.archived.schedule.ending * 1000);

            let beginning = beg.format(format) + " (" + beg.fromNow() + ")";
            let ending = end.format(format) + " (" + end.fromNow() + ")";
            let fixtures = [];
            let fixtureBits = [];
            c.archived.cmp.fixtures.map(f => {
                fixtures.push(f);
                const fixtureBit = f.portUrn + "." + f.vlan.vlanId;
                fixtureBits.push(fixtureBit);
            });
            let fixtureString = fixtureBits.join(" ");

            let row = {
                connectionId: c.connectionId,
                description: c.description,
                phase: c.phase,
                state: c.state,
                southbound: c.southbound,
                tags: toJS(c.tags),
                username: c.username,
                fixtures: fixtures,
                fixtureString: fixtureString,
                beginning: beginning,
                ending: ending
            };
            rows.push(row);
        });

        return (
            <ReactTable
                manual
                pages={cs.filter.totalPages}
                loading={loading}
                onFetchData={this.fetchData}
                data={rows}
                columns={this.columns}
                filterable
                minRows={3}
                defaultPageSize={5}
                className="-striped -highlight"
            />
        );
    }
}

export default withRouter(ConnectionsList);
