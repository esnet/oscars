import React, { Component } from "react";

import { observer, inject } from "mobx-react";
import { toJS } from "mobx";
import ToggleDisplay from "react-toggle-display";

import * as chrono from "chrono-node";
import Moment from "moment";
import jstz from "jstz";
import { size } from "lodash-es";

import {
    FormFeedback,
    Form,
    FormText,
    Button,
    Card,
    CardHeader,
    CardBody,
    FormGroup,
    Input,
    Label
} from "reactstrap";

import ConfirmModal from "../confirmModal";
import HelpPopover from "../helpPopover";

const format = "Y/MM/DD HH:mm:ss";

@inject("controlsStore", "designStore", "topologyStore")
@observer
class ScheduleControls extends Component {
    constructor(props) {
        super(props);
        this.parser = chrono.casual.clone();
        this.parser.parsers.push({
            pattern: () => {
                return /\basap\b/i;
            },
            extract: (context,match) => {
                let date = new Date();
                return {
                    day: date.getDate(),
                    month: date.getMonth(),
                    year: date.getFullYear(),
                    hours: date.getHours(),
                    minutes: date.getMinutes(),
                    seconds: date.getSeconds()
                }
            }
        });

        this.state = {
        };
    }

    componentWillMount() {
        let conn = this.props.controlsStore.connection;
        let sch = conn.schedule;
        let startAt = new Date();
        startAt.setTime(startAt.getTime());
        let params = {
            schedule: {
                acceptable: true,
                locked: true,
                start: {
                    at: startAt,
                    choice: "ASAP",
                    parsed: true,
                    readable: "",
                    validationState: "success",
                    validationText: ""
                },
            }
        };
        this.props.controlsStore.setParamsForConnection(params);

        if (!sch.cloned) {
            let endAt = new Date();
            endAt.setDate(endAt.getDate() + 3650);
            endAt.setTime(endAt.getTime());
            let params = {
                schedule: {
                    acceptable: true,
                    locked: true,
                    end: {
                        at: endAt,
                        choice: "in 10 year",
                        parsed: true,
                        readable: Moment(endAt).format(format),
                        validationState: "success",
                        validationText: ""
                    }
                }
            };
            this.props.controlsStore.setParamsForConnection(params);
        }

        this.periodicCheck();
    }

    periodicCheck() {
        let conn = this.props.controlsStore.connection;

        // if the schedule input is not acceptable after it's been changed etc, unlock all resources
        if (!conn.schedule.acceptable) {
            this.props.designStore.unlockAll();
            return;
        }

        /*
        now check if we're past the start time
        it's ok to be past the start time if we are in ASAP mode
        */

        if (
            conn.schedule.start.at < new Date() &&
            conn.schedule.start.choice.toUpperCase() !== "ASAP"
        ) {
            this.props.controlsStore.setParamsForConnection({
                schedule: {
                    locked: false,
                    acceptable: false,
                    start: {
                        validationState: "error",
                        validationText: "Start time is before now."
                    }
                }
            });
            this.props.designStore.unlockAll();
        }

        // now do this check again in 5 sec

        this.timeoutId = setTimeout(() => {
            this.periodicCheck();
        }, 5000);
    }

    componentWillUnmount() {
        clearTimeout(this.timeoutId);
        this.props.controlsStore.setParamsForConnection({ schedule: { locked: false } });
    }

    onStartDateChange = e => {
        let expr = e.target.value;
        let conn = this.props.controlsStore.connection;
        let parsed = this.parser.parseDate(expr);

        let params = {
            schedule: {
                start: {
                    validationState: "error",
                    validationText: "",
                    parsed: false
                },
                end: {
                    validationState: conn.schedule.end.validationState,
                    validationText: conn.schedule.end.validationText,
                    parsed: conn.schedule.end.parsed
                }
            }
        };

        if (parsed !== null) {
            params.schedule.start.choice = expr;
            params.schedule.start.parsed = true;
            params.schedule.end.choice = toJS(conn.schedule.end.choice);
            this.validateStartEnd(params);
        } else {
            params.schedule.start.validationText = "Invalid date";
            params.schedule.acceptable = false;
        }
        this.props.controlsStore.setParamsForConnection(params);
    };

    onEndDateChange = e => {
        let expr = e.target.value;
        let conn = this.props.controlsStore.connection;

        let params = {
            schedule: {
                start: {
                    validationState: conn.schedule.start.validationState,
                    validationText: conn.schedule.start.validationText,
                    parsed: conn.schedule.start.parsed
                },
                end: {
                    validationState: "error",
                    validationText: "",
                    parsed: false
                }
            }
        };

        let parsed = chrono.parseDate(expr);
        if (parsed !== null) {
            params.schedule.start.choice = toJS(conn.schedule.start.choice);
            params.schedule.end.choice = expr;
            params.schedule.end.parsed = true;
            this.validateStartEnd(params);
        } else {
            params.schedule.end.validationText = "Invalid date";
            params.schedule.acceptable = false;
        }
        this.props.controlsStore.setParamsForConnection(params);
    };

    validateStartEnd(params) {
        if (!params.schedule.start.parsed || !params.schedule.end.parsed) {
            return;
        }

        params.schedule.start.validationState = "success";
        params.schedule.end.validationState = "success";

        let startChoice = params.schedule.start.choice;

        let startAt = this.parser.parseDate(startChoice);
        let endAt = this.parser.parseDate(params.schedule.end.choice);

        let startError = false;
        let endError = false;
        let startAtReadable = Moment(startAt).format(format);

        if (startChoice.toUpperCase() === "ASAP") {
            startAtReadable = "";
        }

        let endAtReadable = Moment(endAt).format(format);

        if (startAt < new Date() && startChoice.toUpperCase() !== "ASAP") {
            params.schedule.start.validationState = "error";
            params.schedule.start.validationText = "Start time is before now.";
            startError = true;
        }

        if (endAt < new Date()) {
            params.schedule.end.validationState = "error";
            params.schedule.end.validationText = "End time is before now.";
            endError = true;
        }

        if (startAt > endAt) {
            params.schedule.start.validationState = "error";
            params.schedule.end.validationState = "error";
            params.schedule.end.validationText = "Start time after end time.";
            params.schedule.start.validationText = "Start time after end time.";
            startError = true;
            endError = true;
        }

        if (!startError) {
            params.schedule.start.readable = startAtReadable;
            params.schedule.start.at = startAt;
        }

        if (!endError) {
            params.schedule.end.readable = endAtReadable;
            params.schedule.end.at = endAt;
        }

        params.schedule.acceptable = !(startError || endError);
    }

    lockSchedule = () => {
        let conn = this.props.controlsStore.connection;
        let params = {
            schedule: {
                start: {
                    choice: toJS(conn.schedule.start.choice)
                },
                end: {
                    choice: toJS(conn.schedule.end.choice)
                }
            }
        };

        this.validateStartEnd(params);
        if (
            params.schedule.start.validationState === "error" ||
            params.schedule.end.validationState === "error"
        ) {
            // do not lock
        } else {
            params.schedule.locked = true;
        }
        this.props.controlsStore.setParamsForConnection(params);
    };

    unlockSchedule = () => {
        let conn = this.props.controlsStore.connection;
        let params = {
            schedule: {
                locked: false,
                start: {
                    choice: toJS(conn.schedule.start.choice)
                },
                end: {
                    choice: toJS(conn.schedule.end.choice)
                }
            }
        };

        this.validateStartEnd(params);
        this.props.controlsStore.setParamsForConnection(params);
        this.props.designStore.unlockAll();
    };

    render() {
        const conn = this.props.controlsStore.connection;
        const sched = conn.schedule;
        const timezone = jstz.determine();

        const helpHeader = <span>Schedule help</span>;
        const helpBody = (
            <span>
                <p>
                    Type in the desired date / time for your connection to start and end. A start
                    time either in the past or after the end time is not accepted.
                </p>
                <p>
                    Then, click "Lock schedule", so that the system can then calculate resource
                    availability.
                </p>
                <p>Unlocking the schedule will also unlock all other resources.</p>
                <p>
                    The (default) start time of "ASAP" is a special expression and means that the
                    reservation should start as soon as the <u>Commit</u> button is pressed. The
                    reservation start time will be about 30 seconds after the <u>Commit</u> action.
                </p>
                <p>
                    Other time expressions such as "in 10 minutes" are accepted for convenience's
                    sake, but they are only evaluated when you <b>type them in</b>. The resulting
                    reservation interval will <b>not</b> change as time passes. It is recommended to
                    use such expressions only when precision is <b>not</b> important, i.e. "in three
                    months". When precision is needed please use specific dates and times instead.
                </p>
            </span>
        );

        const help = (
            <span className="float-right">
                <HelpPopover
                    header={helpHeader}
                    body={helpBody}
                    placement="right"
                    popoverId="scheduleHelp"
                />
            </span>
        );

        let unlockControl = (
            <div>
                <ConfirmModal
                    onConfirm={this.unlockSchedule}
                    uiElement={<Button color="warning">{"Unlock"}</Button>}
                    header="Unlock schedule"
                    body={
                        "Unlocking the schedule will unlock all components and\n" +
                        "                    release any held resources, including pipe and fixture bandwidths and VLANs."
                    }
                />
            </div>
        );

        if (size(this.props.designStore.design.fixtures) === 0) {
            unlockControl = (
                <Button className="float-right" onClick={this.unlockSchedule} color="primary">
                    Unlock
                </Button>
            );
        }
        let endText = "in 10 years";
        if (sched.cloned) {
            endText = sched.end.readable;
        }

        return (
            <Card className="p-1">
                <CardHeader className="p-1">Schedule {help}</CardHeader>
                <CardBody className="p-1">
                    <Form className="p-1 m-1">
                        <FormGroup className="p-1">
                            <Label className="p-1">Start:</Label>
                            <Input
                                type="text"
                                valid={sched.start.validationState === "success"}
                                invalid={sched.start.validationState === "error"}
                                defaultValue="ASAP"
                                disabled={sched.locked}
                                onChange={this.onStartDateChange}
                            />
                            <FormFeedback className="p-1">
                                {sched.start.validationText}
                            </FormFeedback>
                            <FormText className="p-1">{sched.start.readable}</FormText>
                        </FormGroup>{" "}
                        <FormGroup className="p-1">
                            <Label className="p-1">End:</Label>
                            <Input
                                type="text"
                                valid={sched.end.validationState === "success"}
                                invalid={sched.end.validationState === "error"}
                                disabled={sched.locked}
                                defaultValue={endText}
                                onChange={this.onEndDateChange}
                            />
                            <FormFeedback className="p-1">{sched.end.validationText}</FormFeedback>
                            <FormText className="p-1">{sched.end.readable}</FormText>
                        </FormGroup>
                        <ToggleDisplay
                            show={!sched.locked && sched.acceptable && conn.phase === "HELD"}
                        >
                            <Button
                                color="primary"
                                className="float-right"
                                onClick={this.lockSchedule}
                            >
                                Lock schedule
                            </Button>
                        </ToggleDisplay>
                        <ToggleDisplay show={sched.locked && conn.phase === "HELD"}>
                            {unlockControl}
                        </ToggleDisplay>
                        <small>Timezone: {timezone.name()}</small>
                    </Form>
                </CardBody>
            </Card>
        );
    }
}

export default ScheduleControls;
