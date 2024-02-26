import {observable, action} from "mobx";
import {mergeWith} from "lodash-es";

class ModifyStore {

    @observable editDescription = {
        beingEdited: false,
        value: {
            original: null,
            current: null,
        },
        validation: {
            valid: true,
            feedback: {
                valid: "",
                invalid: ""
            },
        },
        help: {
            display: true,
            text: ""
        }
    };

    @observable editEnding = {
        beingEdited: false,
        value: {
            original: null,
            current: null,
            visible: null
        },

        validation: {
            valid: true,
            allowed: {
                floor: null,
                ceiling: null
            },
            feedback: {
                valid: "",
                invalid: ""
            },
        },
        help: {
            display: true,
            text: ""
        }
    }
    @observable editBandwidth = {
        beingEdited: false,
        value: {
            original: null,
            current: null,
        },

        validation: {
            valid: true,
            allowed: {
                floor: null,
                ceiling: null
            },
            feedback: {
                valid: "",
                invalid: ""
            },
        },
        help: {
            display: true,
            text: ""
        }
    }

    @action setParamsForEditEnding(params) {
        mergeWith(this.editEnding, params);
    }

    @action setParamsForEditBandwidth(params) {
        mergeWith(this.editBandwidth, params);
    }

    @action setParamsForEditDescription(params) {
        mergeWith(this.editDescription, params);
    }

}

export default new ModifyStore();
