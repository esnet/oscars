import { createContext } from 'react';
import dayjs from "dayjs";

export const ConnectionParamsContext = createContext({
});
export const InitialConnectionParams = {
    startDate: new dayjs(),
    endDate: new dayjs(),
    connectionId: "",
    description: "",
    endpoints: [],
    routers: [],
    paths: []
}

export const InitialEndpointParams = {
    port: "",
    device: "",
    vlan: 0,
    ingressMbps: 0,
    egressMbps: 0,
    strict: false,
}

export const InitialPathParams = {
    a: "",
    z: "",
    azMbps: 0,
    zaMbps: 0,
    protect: true,
    ero: [],
    pceMode: ""
}