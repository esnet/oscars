'use client'
import Head from 'next/head'
import ScheduleAndDescription from "../../components/create/ScheduleAndDescription";
import {useState} from 'react';
import {ConnectionParamsContext, InitialConnectionParams} from "./context";
import DisplayParameters from "../../components/create/DisplayParameters";
import { LocalizationProvider } from '@mui/x-date-pickers';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs'

export default function List() {
    const [connectionParams, setConnectionParams] = useState(InitialConnectionParams);

    return (
        <LocalizationProvider dateAdapter={AdapterDayjs}>
        <ConnectionParamsContext.Provider value={{ connectionParams, setConnectionParams }}>

        <div>
            <Head>
                <title>OSCARS 1.1 Create New Connection</title>
                <link rel="icon" href="/favicon.ico" />
            </Head>
            <div className="flex w-full">
                <div className="grid h-20 flex-grow card bg-base-300 rounded-box place-items-center"><DisplayParameters /></div>
                <div className="divider divider-horizontal"/>
                <div className="grid h-20 flex-grow card bg-base-300 rounded-box place-items-center"><ScheduleAndDescription /></div>
            </div>
        </div>

        </ConnectionParamsContext.Provider>
        </LocalizationProvider>
    )
}
