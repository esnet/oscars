import React, {Component} from 'react';
import {Card, CardBody, ListGroupItem, ListGroup} from 'reactstrap';
import Moment from 'moment';
import {toJS, autorun} from 'mobx';
import {observer, inject} from 'mobx-react';
import transformer from '../lib/transform';
import {withRouter, Link} from 'react-router-dom';

import BootstrapTable from 'react-bootstrap-table-next';
import 'react-bootstrap-table2-filter/dist/react-bootstrap-table2-filter.min.css';
import filterFactory, {textFilter, selectFilter} from 'react-bootstrap-table2-filter';
import paginationFactory from 'react-bootstrap-table2-paginator';
import myClient from '../agents/client';

@inject('controlsStore', 'connsStore', 'mapStore', 'modalStore', 'commonStore')
@observer
class ConnectionsList extends Component {

    componentWillMount() {
        this.updateList();
    }

    componentWillUnmount() {
        this.disposeOfUpdateList();
    }

    disposeOfUpdateList = autorun(() => {
        this.updateList();
    }, {delay: 1000});

    updateList = () => {
        let csFilter = this.props.connsStore.filter;
        let filter = {};
        csFilter.criteria.map((c) => {
            filter[c] = this.props.connsStore.filter[c];
        });
        filter.page = csFilter.page;
        filter.sizePerPage = csFilter.sizePerPage;
        filter.phase = csFilter.phase;

        myClient.submit('POST', '/api/conn/list', filter)
            .then(
                (successResponse) => {
                    let conns = JSON.parse(successResponse);
                    conns.map((conn) => {
                        transformer.fixSerialization(conn);
                    });
                    this.props.connsStore.updateList(conns);
                }
                ,
                (failResponse) => {
                    this.props.commonStore.addAlert({
                        id: (new Date()).getTime(),
                        type: 'danger',
                        headline: 'Error loading connection list',
                        message: failResponse.status + ' ' + failResponse.statusText
                    });

                    console.log('Error: ' + failResponse.status + ' - ' + failResponse.statusText);
                }
            );

    };

    hrefIdFormatter = (cell, row) => {
        const href = '/pages/details/' + row.connectionId;
        return <Link to={href}>{row.connectionId}</Link>;

    };

    portsFormatter = (cell, row) => {

        let added = [];
        let result = row.fixtures.map((f) => {
            let key = row.connectionId + ':' + f.portUrn;
            if (added.includes(key)) {
                return null;
            } else {
                added.push(key);
                return <ListGroupItem className='m-1 p-1' key={key}>
                    <small>{f.portUrn}</small>
                </ListGroupItem>
            }
        });
        return <ListGroup className='m-0 p-0'>{result}</ListGroup>
    };


    vlansFormatter = (cell, row) => {
        let added = [];
        let result = row.fixtures.map((f) => {
            let key = row.connectionId + ':' + f.vlan.vlanId;
            if (added.includes(key)) {
                return null;
            } else {
                added.push(key);
                return <ListGroupItem className='m-1 p-1' key={key}>
                    <small>{f.vlan.vlanId}</small>
                </ListGroupItem>
            }

        });
        return <ListGroup className='m-0 p-0'>{result}</ListGroup>
    };
    onTableChange = (type, newState) => {
        console.log(type);
        console.log(newState);
        const cs =  this.props.connsStore;
        if (type === 'pagination') {
            cs.setFilter({
                page: newState.page,
                sizePerPage: newState.sizePerPage
            });
        }
        if (type === 'filter') {
            cs.setFilter({
                phase: newState.filters.phase.filterVal
            });
            const fields = ['username', 'connectionId', 'vlans', 'ports', 'description'];
            let params = {
                criteria: []
            };
            for (let field of fields) {
                if (newState.filters[field] !== undefined) {
                    if (field === 'vlans' || field === 'ports') {
                        params[field] = [newState.filters[field].filterVal];
                    } else {
                        params[field] = newState.filters[field].filterVal;
                    }
                    params.criteria.push(field);
                }
            }
            console.log(params);
            cs.setFilter(params);
        }
        this.updateList();
    };

    phaseOptions = {
        'RESERVED': 'Reserved',
        'ARCHIVED': 'Archived'

    };

    columns = [
        {
            text: 'Connection ID',
            dataField: 'connectionId',
            filter: textFilter({delay: 100}),
            formatter: this.hrefIdFormatter

        },
        {
            dataField: 'description',
            text: 'Description',
            filter: textFilter({delay: 100})
        },
        {
            dataField: 'phase',
            text: 'Phase',
            filter: selectFilter({options: this.phaseOptions, defaultValue: 'RESERVED'})
        },

        {
            dataField: 'username',
            text: 'User',
            filter: textFilter({delay: 100})
        },
        {
            dataField: 'ports',
            text: 'Ports',
            formatter: this.portsFormatter,
            filter: textFilter({delay: 100})
        },
        {
            dataField: 'vlans',
            text: 'VLANs',
            formatter: this.vlansFormatter,
            filter: textFilter({delay: 100})
        },
    ];
    render() {
        const format = 'Y/MM/DD HH:mm';

        let rows = [];

        this.props.connsStore.store.conns.map((c) => {
            const beg = Moment(c.archived.schedule.beginning * 1000);
            const end = Moment(c.archived.schedule.ending * 1000);

            let beginning = beg.format(format) + ' (' + beg.fromNow() + ')';
            let ending = end.format(format) + ' (' + end.fromNow() + ')';
            let fixtures = [];
            let fixtureBits = [];
            c.archived.cmp.fixtures.map((f) => {
                fixtures.push(f);
                const fixtureBit = f.portUrn + '.' + f.vlan.vlanId;
                fixtureBits.push(fixtureBit);
            });
            let fixtureString = fixtureBits.join(' ');

            let row = {
                connectionId: c.connectionId,
                description: c.description,
                phase: c.phase,
                state: c.state,
                username: c.username,
                fixtures: fixtures,
                fixtureString: fixtureString,
                beginning: beginning,
                ending: ending
            };
            rows.push(row);
        });
        let remote = {
            filter: true,
            pagination: true,
            sort: false,
            cellEdit: false
        };


        return <Card>
            <CardBody>
                <BootstrapTable keyField='connectionId' data={rows} columns={this.columns}
                                remote={remote} onTableChange={ this.onTableChange }
                                pagination={paginationFactory()}
                                filter={filterFactory()}/>

            </CardBody>


        </Card>


    }
}

export default withRouter(ConnectionsList);