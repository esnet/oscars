import { size } from "lodash-es";
import { observable, action } from "mobx";

import myClient from "../agents/client";

class AccountStore {
    @observable loggedin = {
        username: "",
        token: "",
        admin: false
    };

    @observable attempt = {
        username: "",
        password: "",
        error: ""
    };

    isLoggedIn() {
        return size(this.loggedin.username);
    }

    isAdmin() {
        return this.loggedin.admin;
    }

    @action clearAttempt() {
        this.attempt.username = "";
        this.attempt.password = "";
        this.attempt.error = "";
    }

    @action logout() {
        this.loggedin.username = "";
        this.loggedin.token = "";
        this.loggedin.admin = false;
        localStorage.removeItem("loggedin.token");
        localStorage.removeItem("loggedin.username");
        localStorage.removeItem("loggedin.admin");
    }

    @action setAttemptUsername(u) {
        this.attempt.username = u;
    }

    @action setAttemptPassword(p) {
        this.attempt.password = p;
    }

    @action setAttemptError(e) {
        this.attempt.error = e;
    }

    @action setLoggedinUsername(u) {
        this.loggedin.username = u;
        localStorage.setItem("loggedin.username", u);
    }

    @action setLoggedinAdmin(a) {
        this.loggedin.admin = a;
        localStorage.setItem("loggedin.admin", a);
    }

    @action setLoggedinToken(t) {
        this.loggedin.token = t;
        localStorage.setItem("loggedin.token", t);
    }

    login() {
        let request = { username: this.attempt.username, password: this.attempt.password };
        return myClient.submit("POST", "/api/account/login", request).then(
            response => {
                console.log(response)
                this.setLoggedinUsername(this.attempt.username);
                let parsed = JSON.parse(response);
                this.setLoggedinAdmin(parsed.admin);
                this.setLoggedinToken(parsed.token);

                if (!this.isLoggedIn()) {
                    this.setAttemptError("User not found or wrong password.");
                }
            },
            failure => {
                console.log(failure);
                this.setAttemptError("User not found or wrong password.");
            }
        );
    }
}
export default new AccountStore();
