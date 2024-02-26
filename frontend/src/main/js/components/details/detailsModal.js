import React, { Component } from "react";
import { Modal, ModalHeader, ModalBody } from "reactstrap";
import { inject, observer } from "mobx-react";
import PropTypes from "prop-types";

@inject("connsStore", "modalStore")
@observer
class DetailsModal extends Component {
    constructor(props) {
        super(props);
    }

    componentDidMount() {
        this.toggle();
    }

    closeModal = () => {
        const modalName = this.props.name;
        this.props.modalStore.closeModal(modalName);
    };

    updateParams = modalName => {
        if (modalName === "editEnding") {
            this.props.connsStore.setParamsForEditSchedule({
                ending: {
                    updated: false
                }
            });
        } else {
            this.props.connsStore.setParamsForEditSchedule({
                description: {
                    saved: false
                }
            });
        }
    };

    toggle = () => {
        const modalName = this.props.name;
        if (this.props.modalStore.modals.get(modalName)) {
            this.props.modalStore.closeModal(modalName);
            this.updateParams(modalName);
        } else {
            this.props.modalStore.openModal(modalName);
        }
    };

    render() {
        const modalName = this.props.name;
        let showModal = this.props.modalStore.modals.get(modalName);
        return (
            <Modal
                size="lg"
                fade={false}
                isOpen={showModal}
                toggle={this.toggle}
                onExit={this.closeModal}
            >
                <ModalHeader toggle={this.toggle}>Confirmation Message</ModalHeader>
                <ModalBody>
                    {this.props.name === "editEnding"
                        ? `End time was successfully changed`
                        : `Description was successfully changed`}
                </ModalBody>
            </Modal>
        );
    }
}

DetailsModal.propTypes = {
    name: PropTypes.string.isRequired
};

export default DetailsModal;
