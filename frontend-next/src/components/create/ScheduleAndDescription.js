import {useContext} from 'react';

import {ConnectionParamsContext} from "../../app/create/context";
import {DateTimePicker} from "@mui/x-date-pickers";

export default function ScheduleAndDescription() {
    const { connectionParams, setConnectionParams } = useContext(ConnectionParamsContext);
    const setStartDate = (date) => {
        setConnectionParams({...connectionParams, startDate: date})
    };
    const setEndDate = (date) => {
        setConnectionParams({...connectionParams, endDate: date})
    };
    console.log(connectionParams)


    return (
        <div>
            <DateTimePicker onChange={date => setStartDate(date)} value={connectionParams.startDate} />
            <DateTimePicker onChange={date => setEndDate(date)} value={connectionParams.endDate} />
        </div>
    )
}
