import {useContext} from 'react';

import {ConnectionParamsContext} from "../../app/create/context";

export default function DisplayParameters() {
    const { connectionParams, setConnectionParams } = useContext(ConnectionParamsContext);


    return (
        <>
            <div>
                <p>connectionId: {connectionParams.description}</p>
                <p>description: {connectionParams.connectionId}</p>
            </div>
            <div>
                <p>start date: {connectionParams.startDate.toISOString()}</p>
                <p>end date: {connectionParams.endDate.toISOString()}</p>
            </div>
        </>
    )
}
