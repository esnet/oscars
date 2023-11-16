import {useContext} from "react";
import {AuthContext} from "react-oauth2-code-pkce";

class MyXHRClient {
    loadJSON(opts) {
        return new Promise(function(resolve, reject) {
            let xhr = new XMLHttpRequest();
            xhr.overrideMimeType("application/json");
            xhr.open(opts.method, opts.url);

            if ("timeout" in opts) {
                xhr.timeout = opts.timeout;
            }
            xhr.ontimeout = () => {
                reject({ status: 0, statusText: "Timed out" });
            };

            xhr.onload = function() {
                if (this.status >= 200 && this.status < 300) {
                    resolve(xhr.response);
                } else {
                    reject({
                        status: this.status,
                        statusText: xhr.statusText
                    });
                }
            };

            xhr.onerror = function() {
                reject({
                    status: this.status,
                    statusText: xhr.statusText
                });
            };

            if (opts.headers) {
                Object.keys(opts.headers).forEach(function(key) {
                    xhr.setRequestHeader(key, opts.headers[key]);
                });
            }
            xhr.setRequestHeader("Content-type", "application/json; charset=UTF-8");
            xhr.setRequestHeader("Accept", "application/json");
            let params = opts.params;
            if (params) {
                if (typeof params === "object") {
                    params = JSON.stringify(params);
                }
                xhr.send(params);
            } else {
                xhr.send();
            }
        });
    }

    submit(method, url, payload) {
        let headers = {};

        return this.loadJSON({ method: method, url: url, headers: headers, params: payload });
    }

    submitWithToken(method, url, payload) {
        const {token} = useContext(AuthContext)

        let headers = { Authorization: "Bearer "+token };

        return this.loadJSON({ method: method, url: url, headers: headers, params: payload });
    }
}

export default new MyXHRClient();
