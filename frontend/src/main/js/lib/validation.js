import React from "react";
import Octicon from "react-octicon";
import { Graph, alg } from "graphlib";

class Validator {
    label(state) {
        if (!state) {
            return <Octicon style={{ color: "orange" }} name="alert" />;
        }
        return <Octicon style={{ color: "green" }} name="check" />;
    }

    mapNodeColor(state) {
        if (!state) {
            return "orange";
        }
        return "lightblue";
    }

    mapEdgeColor(state) {
        if (!state) {
            return "orange";
        }
        return null;
    }

    fixtureState(fixture) {
        return fixture.locked;

    }

    pipeState(pipe) {
        return pipe.locked;

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
            errors: []
        };

        const junctions = cmp.junctions;
        const pipes = cmp.pipes;
        const fixtures = cmp.fixtures;

        if (typeof junctions === "undefined") {
            console.log("undefined junctions ");
            result.ok = false;
            result.errors.push("Internal error - undefined junctions");
        } else if (junctions.length < 1) {
            result.ok = false;
            result.errors.push("Not enough junctions - at least 1 required. Add more fixtures.");
        }

        if (typeof fixtures === "undefined") {
            result.ok = false;
            result.errors.push("Internal error - undefined fixtures");
        } else if (fixtures.length < 2) {
            result.ok = false;
            result.errors.push("Not enough fixtures - at least 2 required. Add more fixtures.");
        } else {
            for (let f of fixtures) {
                if (!f.locked) {
                    result.ok = false;
                    result.errors.push("Fixture " + f.label + ": not locked.");
                }
            }
        }
        if (typeof pipes !== "undefined") {
            for (let p of pipes) {
                if (!p.locked) {
                    result.ok = false;
                    result.errors.push("Pipe " + p.id + ": not locked. ");
                }
            }
        } else {
            result.ok = false;
            result.errors.push("Internal error - undefined pipes");
        }
        if (!this.connectedGraph(cmp)) {
            result.ok = false;
            result.errors.push("Disjoint connection graph.");
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
            g.setEdge(p.a, p.z, p.a + " - " + p.z);
        });
        const connected = alg.components(g);
        return connected.length === 1;

    }

    validateConnection(params) {
        let result = this.validateDesign(params);

        const connection = params.connection;

        if (connection.description === "") {
            result.ok = false;
            result.errors.push("Description not set.");
        }

        if (connection.connection_mtu === "") {
            result.ok = false;
            result.errors.push("MTU not set.");
        } else if (connection.connection_mtu < 1500) {
            result.ok = false;
            result.errors.push("MTU too small");
        } else if (connection.connection_mtu > 9000) {
            result.ok = false;
            result.errors.push("MTU too large");
        }

        if (connection.connectionId === "") {
            result.ok = false;
            result.errors.push("Connection id missing!");
        }

        if (connection.mode === "MANUAL" || connection.mode === "AUTOMATIC") {
            // ok
        } else {
            result.ok = false;
            result.errors.push("Mode is invalid " + connection.mode);
        }

        if (!connection.schedule.locked) {
            result.ok = false;
            result.errors.push("Schedule not locked.");
        }

        const now = Date.now();
        if (connection.schedule.end.at < now) {
            result.ok = false;
            result.errors.push("End time set in the past!");
        }
        if (
            connection.schedule.start.at < now &&
            connection.schedule.start.choice.toUpperCase() !== "ASAP"
        ) {
            result.ok = false;
            result.errors.push("Start time (not-ASAP) in the past!");
        }

        if (connection.schedule.start.at > connection.schedule.end.at) {
            result.ok = false;
            result.errors.push("End time set before start time.");
        }

        for (let ctgKey in connection.categories) {
            let ctgEntry = connection.categories[ctgKey];
            if (ctgEntry["mandatory"]) {
                let category = ctgEntry["category"];
                let found = false;
                for (let tagKey in connection.tags) {
                    let tagEntry = connection.tags[tagKey];
                    if (tagEntry["category"] === category) {
                        if (tagEntry["contents"] != null && tagEntry["contents"] !== "") {
                            found = true;
                        }
                    }
                }
                if (!found) {
                    result.ok = false;
                    result.errors.push("Empty tag for mandatory category " + category);
                }
            }
        }

        return result;
    }

    descriptionControl(val) {
        if (val === "") {
            return "error";
        }
        if (/^[a-zA-Z0-9\s\]\[)(@!#%{};<>._-]+$/.test(val)) {
            return "success";
        }
        return "error"
    }
    serviceIdControl(val) {
        if (typeof val === "undefined") {
            return "success";
        }
        if (val.length === 0) {
            return "success";
        }
        if (val.length > 0 && val.length < 4) {
            return "error";
        }
        // must be alphanumeric
        if (/^[a-zA-Z0-9]+$/.test(val)) {
            return "success";
        }
        return "error";
    }
    projectIdControl(val) {
        if (typeof val === "undefined") {
            return "success";
        }
        if (val.length === 0) {
            return "success";
        }
        if (val.length > 48) {
            return "error";
        }
        // must be alphanumeric with dash
        if (/^[a-zA-Z0-9-]+$/.test(val)) {
            return "success";
        }
        return "error";
    }
    mtuControl(val) {
        if (val >= 1500 && val <= 9000) {
            return "success";
        }
        return "error";
    }

    cleanBandwidth(inputStr, control) {
        let cleanedUp = false;
        let ws_re = /\s+/;
        let ws_idx = inputStr.search(ws_re);
        let g_re = /g/i;
        let g_idx = inputStr.search(g_re);

        if (ws_idx >= 0) {
            inputStr = inputStr.replace(ws_re, "");
            cleanedUp = true;
        }
        if (g_idx >= 0) {
            inputStr = inputStr.replace(g_re, "000");
            cleanedUp = true;
        }

        if (cleanedUp) {
            control.value = inputStr;
        }
        return inputStr;
    }

    tagsControl(categories, category, mandatory) {
        if (mandatory === false) {
            return "success";
        } else {
            for (let i in categories) {
                let c = categories[i].category;
                let s = categories[i].selected;
                if (c === category && (s === undefined || s.length === 0)) {
                    return "error";
                }
            }
            return "success";
        }
    }
}

export default new Validator();
