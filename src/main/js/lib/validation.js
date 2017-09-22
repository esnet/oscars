import React from 'react';


import {Graph, alg} from 'graphlib';
import {Glyphicon, Label} from 'react-bootstrap';

class Validator {
    label(state) {
        let icon = 'ok';
        let bsStyle = 'success';
        if (!state) {
            icon = 'flag';
            bsStyle = 'warning'
        }
        return <Label bsStyle={bsStyle}><Glyphicon glyph={icon}/></Label>
    }

    mapNodeColor(state) {
        if (!state) {
            return 'orange';
        }
        return null;
    }

    mapEdgeColor(state) {
        if (!state) {
            return 'orange';
        }
        return null;
    }


    fixtureState(fixture) {
        if (!fixture.locked ) {
            return false;
        }
        return true;
    }

    pipeState(pipe) {
        if (!pipe.locked) {
            return false;
        }
        return true;
    }

    fixtureMapColor(fixture) {
        return this.mapNodeColor(this.fixtureState(fixture));
    }

    pipeMapColor(pipe) {
        return this.mapEdgeColor(this.pipeState(pipe));
    }

    fixtureLabel(fixture) {
        return this.label(this.fixtureState(fixture));
    }

    pipeLabel(pipe) {
        return this.label(this.pipeState(pipe));
    }

    validateDesign(cmp) {
        let result = {
            ok: true,
            errors: [],
        };

        const junctions = cmp.junctions;
        const pipes = cmp.pipes;
        const fixtures = cmp.fixtures;

        if (typeof junctions === 'undefined') {
            console.log('undefined junctions ');
            result.ok = false;
            result.errors.push('Internal error - undefined junctions');

        } else if (junctions.length < 1) {
            result.ok = false;
            result.errors.push('Not enough junctions - at least 1 required. Add more fixtures.');
        }

        if (typeof fixtures === 'undefined') {
            result.ok = false;
            result.errors.push('Internal error - undefined fixtures');

        } else if (fixtures.length < 2) {
            result.ok = false;
            result.errors.push('Not enough fixtures - at least 2 required. Add more fixtures.');
        } else {
            for (let f of fixtures) {
                if (!f.locked) {
                    result.ok = false;
                    result.errors.push('Fixture '+f.label+': not locked.');
                }
            }
        }
        if (typeof pipes !== 'undefined') {
            for (let p of pipes) {
                if (!p.locked) {
                    result.ok = false;
                    result.errors.push('Pipe '+p.id+': not locked. ');
                }
            }
        } else {
            result.ok = false;
            result.errors.push('Internal error - undefined pipes');
        }
        if (!this.connectedGraph(cmp)) {
            result.ok = false;
            result.errors.push('Disjoint connection graph.');

        }

        return result;

    }

    connectedGraph(cmp) {
        let g = new Graph();

        const junctions = cmp.junctions;
        if (junctions.length === 0) {
            return true;
        }

        const pipes = cmp.pipes;
        junctions.map(j => {
            g.setNode(j.id, j.id);
        });
        pipes.map(p => {
            g.setEdge(p.a, p.z, p.a+' - '+p.z);

        });
        const connected = alg.components(g);
        if (connected.length === 1) {
            return true;
        }
        return false;
    }

    validateConnection(params) {
        let result = this.validateDesign(params);

        const connection = params.connection;

        if (connection.description === '') {
            result.ok = false;
            result.errors.push('Description not set.');
        }
        if (connection.connectionId === '') {
            result.ok = false;
            result.errors.push('Connection id missing!');
        }

        if (!connection.schedule.locked) {
            result.ok = false;
            result.errors.push('Schedule not locked.');
        }

        const now = Date.now();
        if (connection.schedule.start.at < now || connection.schedule.end.at < now) {
            result.ok = false;
            result.errors.push('Start or end time set in the past!');
        }
        if (connection.schedule.start.at > connection.schedule.end.at) {
            result.ok = false;
            result.errors.push('End time set before start time.');
        }
        return result;
    }

    descriptionControl(val) {
        if (val === '') {
            return 'error';
        }
        return 'success';
    }

}

export default new Validator();
