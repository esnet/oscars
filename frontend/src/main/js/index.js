import React, {useContext} from "react";

import ReactDOM from "react-dom";

import {AuthContext, AuthProvider} from "react-oauth2-code-pkce"

import {BrowserRouter, Route, Switch} from "react-router-dom";
import {Container, Row, Col} from "reactstrap";
import "bootstrap/dist/css/bootstrap.css";

import {configure} from "mobx";
import {Provider} from "mobx-react";

import ListConnectionsApp from "./apps/listConnections";
import NewDesignApp from "./apps/designApp";
import WelcomeApp from "./apps/welcome";
import AboutApp from "./apps/about";
import TimeoutApp from "./apps/timeout";
import ErrorApp from "./apps/error";

import StatusApp from "./apps/statusApp";
import MapApp from "./apps/mapApp";
import ConnectionDetails from "./apps/detailsApp";


import NavBar from "./components/navbar";
import Ping from "./components/ping";

import accountStore from "./stores/accountStore";
import commonStore from "./stores/commonStore";
import controlsStore from "./stores/controlsStore";
import heldStore from "./stores/heldStore";
import mapStore from "./stores/mapStore";
import designStore from "./stores/designStore";
import topologyStore from "./stores/topologyStore";
import connsStore from "./stores/connsStore";
import userStore from "./stores/userStore";
import modalStore from "./stores/modalStore";
import tagStore from "./stores/tagStore";



require("../css/styles.css");


const stores = {
    accountStore,
    commonStore,
    connsStore,
    controlsStore,
    mapStore,
    designStore,
    heldStore,
    topologyStore,
    userStore,
    tagStore,
    modalStore
};
const UserInfo = () => {
    const {token, tokenData} = useContext(AuthContext);
    if (token) {
        accountStore.setLoggedinToken(token);
    }
    if (tokenData) {
        accountStore.setLoggedinUsername(tokenData.preferred_username)
    }

    return <>
        <h4>Access Token</h4>
        <pre>{token}</pre>
        <h4>User Information from JWT</h4>
        <pre>{JSON.stringify(tokenData, null, 2)}</pre>
    </>
}

let authConfig = {
    clientId: '',
    scope: '',
    redirectUri: '',
    authorizationEndpoint: '',
    tokenEndpoint:         '',
    logoutEndpoint:        '',
    onRefreshTokenExpire: (event) => window.confirm('Session expired. Refresh page to continue using the site?') && event.login(),
}


const authConfig_init = () => {
    if (authConfig.clientId === '') {
        const xhr = new XMLHttpRequest();
        xhr.open("GET", "/api/frontend/oauth", false); // `false` makes the request synchronous
        xhr.overrideMimeType("application/json");
        xhr.send(null);

        if (xhr.status === 200) {
            let data = JSON.parse(xhr.responseText);
            authConfig.clientId = data.clientId;
            authConfig.scope = data.scope;
            authConfig.redirectUri = data.redirectUri;
            authConfig.authorizationEndpoint = data.authorizationEndpoint;
            authConfig.tokenEndpoint = data.tokenEndpoint;
            authConfig.logoutEndpoint = data.logoutEndpoint;
            console.log(authConfig)
        }
    }
}

authConfig_init();

configure({enforceActions: "observed"});

ReactDOM.render(
    <Provider {...stores}>
        <AuthProvider authConfig={authConfig}>

            <BrowserRouter>
                <Container fluid={true}>
                    <Ping/>
                    <Row>
                        <NavBar/>
                    </Row>
                    <Row>
                        <Col sm={4}><UserInfo/></Col>
                    </Row>
                    <Switch>
                        <Route exact path="/" component={WelcomeApp}/>
                        <Route exact path="/pages/about" component={AboutApp}/>
                        <Route exact path="/pages/list" component={ListConnectionsApp}/>
                        <Route path="/pages/details/:connectionId?"
                            component={ConnectionDetails}
                        />
                        <Route exact path="/pages/newDesign" component={NewDesignApp}/>
                        <Route exact path="/pages/timeout" component={TimeoutApp}/>
                        <Route exact path="/pages/error" component={ErrorApp}/>
                        <Route exact path="/pages/status" component={StatusApp}/>
                        <Route exact path="/pages/map" component={MapApp}/>

                    </Switch>
                </Container>
            </BrowserRouter>
        </AuthProvider>
    </Provider>,
    document.getElementById("react")
)
;
