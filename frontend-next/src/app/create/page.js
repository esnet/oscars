import Head from 'next/head'
import styles from '../../styles/Home.module.css'

export default function List() {
    return (
        <div className={styles.container}>
            <Head>
                <title>OSCARS 1.1 List Connections</title>
                <link rel="icon" href="/favicon.ico" />
            </Head>

            <main className={styles.main}>
                <div>
                    List connections here

                </div>
            </main>

        </div>
    )
}
