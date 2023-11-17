import React, {useContext} from "react";
import {AuthContext} from "react-oauth2-code-pkce";
import {BrowserRouter, Route, Switch} from "react-router-dom";
import {Col, Container, Row} from "reactstrap";
import Ping from "./components/ping";
import NavBar from "./components/navbar";
import WelcomeApp from "./apps/welcome";
import AboutApp from "./apps/about";
import ListConnectionsApp from "./apps/listConnections";
import ConnectionDetails from "./apps/detailsApp";
import NewDesignApp from "./apps/designApp";
import TimeoutApp from "./apps/timeout";
import ErrorApp from "./apps/error";
import StatusApp from "./apps/statusApp";
import MapApp from "./apps/mapApp";

function Root(props) {
    let allowed = false;
    if (props.anonymous) {
        allowed = true;
    } else {
        const {tokenData} = useContext(AuthContext);
        if (tokenData) {
            for (const group of tokenData.groups) {
                if (props.allowedGroups.includes(group)) {
                    allowed = true;
                    break;
                }
            }
        }
    }

    if (!allowed) {
        return <Container fluid={true}>
            <Row>
                <Col sm={4}>Your account is not allowed on OSCARS</Col>
            </Row>
        </Container>
    } else {
        return <BrowserRouter>
            <Container fluid={true}>
                <Ping/>
                <Row>
                    <NavBar/>
                </Row>
                <Row>
                    <Col sm={4}></Col>
                </Row>
                <Switch>
                    <Route exact path="/" component={WelcomeApp}/>
                    <Route exact path="/pages/about" component={AboutApp}/>
                    <Route exact path="/pages/list" component={ListConnectionsApp}/>
                    <Route path="/pages/details/:connectionId?" component={ConnectionDetails} />
                    <Route exact path="/pages/newDesign" component={NewDesignApp}/>
                    <Route exact path="/pages/timeout" component={TimeoutApp}/>
                    <Route exact path="/pages/error" component={ErrorApp}/>
                    <Route exact path="/pages/status" component={StatusApp}/>
                    <Route exact path="/pages/map" component={MapApp}/>
                </Switch>
            </Container>
        </BrowserRouter>
    }


}

export default Root;