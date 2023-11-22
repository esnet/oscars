import { observable, action } from "mobx";
import { isArray, mergeWith, size } from "lodash-es";

import myClient from "../agents/client";
import transformer from "../lib/transform";

class ConnectionsStore {
    @observable store = {
        conns: [],
        current: {
            archived: {
                cmp: {},
                schedule: {
                    beginning: null,
                    ending: null
                }
            },
            serviceId: "",
            tags: [],
            dirty: false
        },
        foundCurrent: false,
        pss: {
            work: null,
            explanation: "Status not loaded yet...",
        },
        selected: {},
        commands: new Map(),
        statuses: new Map(),
        history: new Map(),
        eventLog: [],
        macInfo: {},
        cloned: {
            cloneable: false,
            message: ""
        }
    };

    @observable editSchedule = {
        connectionId: "",
        description: {
            originalDescription: "",
            updatedDescription: "",
            acceptable: false,
            validationText: "",
            validationState: "success",
            saved: false,
            buttons: {
                buttonText: "Edit",
                edit: true,
                save: true,
                input: true,
                collapseText: true
            }
        },
        ending: {
            originalTime: "",
            newTime: "",
            acceptable: false,
            validationText: "",
            validationState: "success",
            validSchedule: {
                beginning: null,
                ending: null
            },
            parsedValue: null,
            saved: false,
            buttons: {
                buttonText: "Edit",
                edit: true,
                save: true,
                input: true,
                collapseText: true
            }
        }
    };

    @observable drawing = {
        redraw: true
    };

    @observable controls = {
        show: false,
        buildmode: {
            allowed: false,
            working: false,
            show: false,
            text: ""
        },
        build: {
            allowed: false,
            working: false,
            show: false,
            text: ""
        },
        dismantle: {
            allowed: false,
            working: false,
            show: false,
            text: ""
        },
        release: {
            allowed: false,
            working: false,
            show: false,
            text: ""
        },
        help: {
            build: {
                header: null,
                body: null
            },
            buildMode: {
                header: null,
                body: null
            },
            release: {
                header: null,
                body: null
            },
            dismantle: {
                header: null,
                body: null
            }
        },
        regenerate: {
            allowed: false,
            working: false,
            show: false,
            text: ""
        },
        overrideState: {
            newState: null
        }
    };

    @observable filter = {
        criteria: [],
        ports: [],
        vlans: [],
        connectionId: "",
        username: "",
        description: "",
        phase: "RESERVED",
        state: "ACTIVE",
        sizePerPage: 5,
        page: 0,
        totalPages: 1,
        filtered: []
    };

    @action setParamsForEditSchedule(params) {
        mergeWith(this.editSchedule, params);
    }

    @action setCloned(value) {
        this.store.cloned = value;
    }

    @action setRedraw(value) {
        this.drawing.redraw = value;
    }

    @action setCommands(commands) {
        this.store.commands = commands;
    }

    @action setHistory(history) {
        this.store.history = history;
    }

    @action setEventLog(eventLog) {
        this.store.eventLog = eventLog;
    }

    @action setPss(status) {
        this.store.pss = status;
    }

    @action showControls(value) {
        this.controls.show = value;
    }

    @action setControl(unit, params) {
        this.controls[unit] = params;
    }

    @action setControlHelp(unit, params) {
        this.controls.help[unit] = params;
    }

    @action setStatuses(device, statuses) {
        this.store.statuses[device] = statuses;
    }

    @action setFilter(params) {
        mergeWith(this.filter, params, this.customizer);
    }

    @action setCurrent(conn) {
        this.store.current = conn;
        this.store.foundCurrent = true;
    }

    @action clearCurrent() {
        this.store.current = {};
        this.store.foundCurrent = false;
    }

    @action setSelected(component) {
        this.store.selected = component;
    }

    @action updateList(resvs) {
        this.store.conns = [];
        this.store.conns = resvs;
    }

    @action setMacInfo(macInfo) {
        this.store.macInfo = macInfo;
    }

    @action refreshCommands() {
        if (size(this.store.current.connectionId) > 0) {
            myClient
                .submitWithToken("GET", "/protected/pss/commands/" + this.store.current.connectionId)
                .then(
                    action(response => {
                        let commands = JSON.parse(response);
                        if (commands.length !== 0) {
                            this.setCommands(commands);
                        } else {
                            this.setCommands({});
                        }
                    })
                );
        }
    }

    @action refreshCurrent() {
        myClient.submitWithToken("GET", "/api/conn/info/" + this.store.current.connectionId).then(
            action(response => {
                let conn = JSON.parse(response);
                transformer.fixSerialization(conn);
                this.setCurrent(conn);
            })
        );
    }

    findConnection(connectionId) {
        for (let conn of this.store.conns) {
            if (conn.connectionId === connectionId) {
                return conn;
            }
        }
    }

    customizer = (objValue, srcValue) => {
        if (isArray(srcValue)) {
            return srcValue;
        }
    };
}

export default new ConnectionsStore();
