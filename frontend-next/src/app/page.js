import Head from 'next/head'
import styles from '../styles/Home.module.css'
import Link from 'next/link'

export default function Home() {
    return (
        <div className={styles.container}>
            <Head>
                <title>OSCARS 1.1</title>
                <link rel="icon" href="/favicon.ico"/>
            </Head>

            <main className={styles.main}>
                <div>
                    <p>
                        <Link href="/list">List</Link>
                    </p>
                    <p>
                        <Link href="/create">Create</Link>
                    </p>
                    <p>Welcome to OSCARS 1.1</p>



                </div>
            </main>

        </div>
    )
}
