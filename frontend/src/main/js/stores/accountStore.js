import { size } from "lodash-es";
import { observable, action } from "mobx";

class AccountStore {
    @observable loggedin = {
        username: "",
        token: "",
        admin: false,
        anonymous: false,
        allowed: false,
        claimedGroups: [],
        allowedGroups: [],
    };

    isLoggedIn() {
        return size(this.loggedin.username);
    }

    isAdmin() {
        return this.loggedin.admin;
    }


    @action logout() {
        this.loggedin.username = "";
        this.loggedin.token = "";
        this.loggedin.admin = false;
        localStorage.removeItem("loggedin.token");
        localStorage.removeItem("loggedin.username");
        localStorage.removeItem("loggedin.admin");
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
    @action setLoggedinAnonymous(a) {
        this.loggedin.anonymous = a;
    }
    @action setClaimedGroups(a) {
        this.loggedin.claimedGroups = a;
    }
    @action setAllowedGroups(a) {
        this.loggedin.allowedGroups = a;
    }

    @action setAllowed(a) {
        this.loggedin.allowed = a;
    }
}
export default new AccountStore();
