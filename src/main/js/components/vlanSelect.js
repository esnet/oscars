import React, {Component} from 'react';
import {observer, inject} from 'mobx-react';
import {
    OverlayTrigger, Glyphicon, Popover, Panel, FormGroup,
    FormControl, ControlLabel, HelpBlock, Well
} from 'reactstrap';
import ToggleDisplay from 'react-toggle-display';

import Validator from '../lib/validation';


@inject('designStore', 'controlsStore', 'topologyStore')
@observer
export default class VlanSelect extends Component {


    updateForm = () => {
        if (!this.props.controlsStore.connection.schedule.locked) {
            return;
        }

        const ef = this.props.controlsStore.editFixture;

        const baseline = this.props.topologyStore.baseline[ef.port];
        const baselineVlanExpression = baseline.vlanExpression;
        const baselineVlanRanges = baseline.vlanRanges;

        if (!(ef.port in this.props.topologyStore.available)) {
            console.error('internal error: port not found in topology.');
            return;
        }

        const available = this.props.topologyStore.available[ef.port];
        const availableVlanExpression = available.vlanExpression;
        const availableVlanRanges = available.vlanRanges;

        let suggestion = this.props.topologyStore.suggestions.globalVlan;

        let lowest = 99999;
        for (let rng of availableVlanRanges) {
            if (lowest > rng.floor) {
                lowest = rng.floor;
            }
        }

        if (suggestion === -1) {
            suggestion = lowest;
        }

        for (let f of this.props.designStore.design.fixtures) {
            if (f.locked) {
                for (let rng of availableVlanRanges) {
                    if (f.vlan >= rng.floor && f.vlan <= rng.ceiling) {
                        suggestion = f.vlan;
                    }
                }
            }
        }



        if (ef.locked) {
            return;
        }

        this.props.controlsStore.setParamsForEditFixture({
            vlan: {
                available: {
                    expression: availableVlanExpression,
                    ranges: availableVlanRanges,
                    suggestion: suggestion
                },
                baseline: {
                    expression: baselineVlanExpression,
                    ranges: baselineVlanRanges
                },
                validationState: 'success',
                validationText: 'VLAN available',
                vlanId: suggestion,
                acceptable: true
            }
        });


    };


    componentWillMount() {
        this.updateForm();
    }


    onTypeIn = (e) => {
        const vlanId = parseInt(e.target.value);
        const ef = this.props.controlsStore.editFixture;

        let valText = 'VLAN available';
        let valState = 'success';
        let hasError = false;

        let inBaseline = false;
        ef.vlan.baseline.ranges.map((rng) => {
            if (rng.floor <= vlanId && rng.ceiling >= vlanId) {
                inBaseline = true;
            }
        });
        if (!inBaseline) {
            hasError = true;
            valText = 'VLAN not in baseline.';
        } else {

            let inAvailable = false;
            ef.vlan.available.ranges.map((rng) => {
                if (rng.floor <= vlanId && rng.ceiling >= vlanId) {
                    inAvailable = true;
                }
            });
            if (!inAvailable) {
                hasError = true;
                valText = 'VLAN reserved by another connection.';

                let portVlans = this.props.designStore.vlansLockedOnPort(ef.port);
                console.log(portVlans);
                if (portVlans.includes(vlanId)) {
                    valText = 'VLAN being used in this connection (by another fixture on this port).';
                }
            }
        }

        if (hasError) {
            valState = 'error';
        }


        this.props.controlsStore.setParamsForEditFixture({
            vlan: {
                acceptable: !hasError,
                validationState: valState,
                validationText: valText,
                vlanId: vlanId
            }
        });
    };

    render() {
        const ef = this.props.controlsStore.editFixture;

        let helpPopover = <Popover id='help-vlanSelect' title='VLAN selection'>
            <p>Here you can set the VLAN id for this fixture. </p>

            <p>In 'unlocked' mode, when the dialog opens the value will be editable and set to a suggested VLAN that
                is likely globally available on the network. You may type in a different value; if it is not available
                on this fixture you will receive feedback explaining why.</p>
            <p>You will also see 'baseline' and 'available' ranges displayed.</p>
            <p>The <u>baseline</u> range is what would be available if there were no other reservations, and generally
                does not change.</p>
            <p>The <u>available</u> range is calculated by taking the baseline and removing any resources used by other
                reservations overlapping the selected schedule.</p>
            <p>When the fixture is locked, this control will display the selected value, and will not be editable.
                Unlock the fixture to edit.</p>
        </Popover>;


        return (
            <Panel>
                <Panel.Heading>
                    <p>VLAN selection
                        <OverlayTrigger trigger='click' rootClose placement='bottom' overlay={helpPopover}>
                            <Glyphicon className='pull-right' glyph='question-sign'/>
                        </OverlayTrigger>
                    </p>
                </Panel.Heading>
                <Panel.Body>
                    <ToggleDisplay show={!ef.locked}>
                        <FormGroup controlId='vlanExpression'>
                            <HelpBlock>Available for your schedule: {ef.vlan.available.expression}</HelpBlock>
                            <HelpBlock>Baseline: {ef.vlan.baseline.expression}</HelpBlock>
                        </FormGroup>


                        {' '}
                        <FormGroup controlId='vlanChoice' validationState={ef.vlan.validationState}>
                            <ControlLabel>VLAN choice:</ControlLabel>
                            {' '}
                            <FormControl defaultValue={ef.vlan.available.suggestion} type='text' onChange={this.onTypeIn}/>
                            <HelpBlock><p>{ef.vlan.validationText}</p></HelpBlock>
                            {' '}
                        </FormGroup>
                        {' '}
                        {Validator.label(ef.vlan.acceptable)}

                    </ToggleDisplay>
                    {' '}
                    <ToggleDisplay show={ef.locked}>
                        <Well>Locked VLAN: {ef.vlan.vlanId}</Well>
                    </ToggleDisplay>
                </Panel.Body>

            </Panel>

        );

    }
}

