import React, {useContext} from "react";

import ReactDOM from "react-dom";

import {AuthContext, AuthProvider} from "react-oauth2-code-pkce"

import "bootstrap/dist/css/bootstrap.css";

import {configure} from "mobx";
import {Provider} from "mobx-react";

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
import Root from "./Root";

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

let authConfig = {
    clientId: '',
    scope: '',
    redirectUri: '',
    authorizationEndpoint: '',
    tokenEndpoint: '',
    logoutEndpoint: '',
    onRefreshTokenExpire: (event) => window.confirm('Session expired. Refresh page to continue using the site?') && event.login(),
}
const UserInfo = () => {
    if (!accountStore.loggedin.anonymous) {
        const {token, tokenData} = useContext(AuthContext);
        if (token) {
            accountStore.setLoggedinToken(token);
        }
        if (tokenData) {
            console.log(tokenData)
            accountStore.setLoggedinUsername(tokenData.preferred_username)
        }
    }
    return <>
    </>
}

let allowedGroups = [];
let anonymous = false;

const auth_init = () => {
    if (authConfig.clientId === '') {
        const xhr = new XMLHttpRequest();
        xhr.open("GET", "/api/frontend/oauth", false); // `false` makes the request synchronous
        xhr.overrideMimeType("application/json");
        xhr.send(null);

        if (xhr.status === 200) {
            let data = JSON.parse(xhr.responseText);
            if (data.enabled) {
                anonymous = false;
                allowedGroups = data.allowedGroups;

                authConfig.clientId = data.clientId;
                authConfig.scope = data.scope;
                authConfig.redirectUri = data.redirectUri;
                authConfig.authorizationEndpoint = data.authorizationEndpoint;
                authConfig.tokenEndpoint = data.tokenEndpoint;
                authConfig.logoutEndpoint = data.logoutEndpoint;
                console.log(data)
            } else {
                anonymous = true;
            }
        }
    }
}

auth_init();
configure({enforceActions: "observed"});


if (anonymous) {
    ReactDOM.render(
        <Provider {...stores}>
            <Root anonymous={true} />
        </Provider>,
        document.getElementById("react"));
} else {
    ReactDOM.render(
        <AuthProvider authConfig={props.authConfig}>
            <Provider {...stores}>
                <UserInfo/>
                <Root allowedGroups={allowedGroups} anonymous={false} />
            </Provider>
        </AuthProvider>,
        document.getElementById("react"));
}

