import Moment from "moment";
import moment from "moment";
import {ListGroupItem} from "reactstrap";
import React from "react";

class Util {
    static formatSchedule(timems) {
        const format = "Y/MM/DD HH:mm";
        const timeSec = Moment(timems * 1000);
        const time = timeSec.format(format);
        const formattedTime = time + " (" + timeSec.fromNow() + ")";

        return {
            concise: time,
            verbose: formattedTime
        };
    }

    static determineBandwidth(conn) {
        let bw = 0;
        conn.archived.cmp.fixtures.map(f => {
            if (f.ingressBandwidth > bw) {
                bw = f.ingressBandwidth;
            }
            if (f.egressBandwidth > bw) {
                bw = f.egressBandwidth;
            }
        })
        conn.archived.cmp.pipes.map(p => {
            if (p.azBandwidth > bw) {
                bw = p.azBandwidth;
            }
            if (p.zaBandwidth > bw) {
                bw = p.zaBandwidth;
            }
        })

        return bw;
    }
}

export default Util;
