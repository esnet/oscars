import '../styles/globals.css'
import OscarsNav from "../components/OscarsNav";


export default function RootLayout({ children }) {
    return (
        <html lang="en">
        <body>
        <OscarsNav />
        {children}
        </body>
        </html>
    )
}