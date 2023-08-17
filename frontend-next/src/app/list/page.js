'use client'

import Head from 'next/head'
import styles from '../../styles/Home.module.css'
import {fetcher, getFrontendServerUrl} from "../../lib/clients";
import useSWR from 'swr'


function useConnectionList () {
    const listApiEndpoint = getFrontendServerUrl()+'/api/list';
    const { data, error, isLoading } = useSWR(listApiEndpoint, fetcher)
    console.log(data)
    return {
        connectionsPage: data,
        isLoading,
        isError: error
    }
}
function ConnectionListWidget() {
    const { connectionsPage, isLoading, isError } = useConnectionList()
    if (isLoading) return <p>still loading</p>
    if (isError) return <p>error loading connections</p>
    let connectionsList = []
    console.log(connectionsPage)
    connectionsPage.map((c, idx) => {
        connectionsList.push(<div key={idx}>{c['connectionId']}</div>)
    })
    return connectionsList;
}

export default function List() {

    return (
        <div className={styles.container}>
            <Head>
                <title>OSCARS 1.1 List Connections</title>
                <link rel="icon" href="/favicon.ico" />
            </Head>

            <main className={styles.main}>
                <div><ConnectionListWidget/></div>
            </main>

        </div>
    )
}
