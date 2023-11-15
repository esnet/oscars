import React, { Component } from "react";

import {
    Navbar,
    NavbarBrand,
    Nav,
    UncontrolledDropdown,
    DropdownMenu,
    DropdownToggle,
    NavLink
} from "reactstrap";

import { observer, inject } from "mobx-react";
import { AlertList } from "react-bs-notifier";
import { toJS } from "mobx";

@inject("accountStore", "commonStore")
@observer
class NavBar extends Component {



    render() {

        return (
            <Navbar color="faded" light expand="md">
                <AlertList
                    position="top-right"
                    alerts={toJS(this.props.commonStore.alerts)}
                    timeout={1000}
                    dismissTitle="Dismiss"
                    onDismiss={alert => {
                        this.props.commonStore.removeAlert(alert);
                    }}
                />
                <NavbarBrand href="/pages/about">OSCARS</NavbarBrand>

                <Nav navbar>
                    <NavLink
                        href="/pages/list"
                        active={this.props.commonStore.nav.active === "list"}
                    >
                        List
                    </NavLink>
                    <NavLink
                        href="/pages/newDesign"
                        active={this.props.commonStore.nav.active === "newDesign"}
                    >
                        New
                    </NavLink>
                    <NavLink
                        href="/pages/status"
                        active={this.props.commonStore.nav.active === "status"}
                    >
                        Status
                    </NavLink>
                    <UncontrolledDropdown>
                        <DropdownToggle nav caret>
                            Help
                        </DropdownToggle>
                        <DropdownMenu>
                            <NavLink
                                href="//github.com/esnet/oscars/issues/new"
                                target="_blank"
                                rel="noopener noreferrer"
                            >
                                Report an issue
                            </NavLink>
                        </DropdownMenu>
                    </UncontrolledDropdown>
                </Nav>
            </Navbar>
        );
    }
}

export default NavBar;
