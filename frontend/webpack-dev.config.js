let packageJSON = require("./package.json");
let path = require("path");
let HtmlWebpackPlugin = require("html-webpack-plugin");
// let BundleAnalyzerPlugin = require("webpack-bundle-analyzer").BundleAnalyzerPlugin;

let webpack = require("webpack");

const PATHS = {
    build: path.join(__dirname, "build"),
};

let plugins = [
    new webpack.ContextReplacementPlugin(/moment[/\\]locale$/, /en/),
    new webpack.DefinePlugin({
        "process.env": {
            NODE_ENV: JSON.stringify("development")
        },
        __VERSION__: JSON.stringify(packageJSON.version)
    }),
    new HtmlWebpackPlugin({
        template: "./public/index.html",
        inject: "body",
        favicon: "./public/favicon.ico"
    })
    // show bundle sizes and whatnot
    // new BundleAnalyzerPlugin(),
];

let devtool = "eval";

module.exports = {
    entry: ["@babel/polyfill", "./src/main/js/index.js"],
    devtool: devtool,
    cache: true,
    mode: "development",

    output: {
        path: PATHS.build,
        publicPath: "/",
        filename: '[hash].js',
        chunkFilename: '[chunkhash].js',
    },
    performance: {
        hints: false
    },
    optimization: {
        nodeEnv: "production",
        minimize: true
    },
    module: {
        rules: [
            {
                test: /node_modules[\\\/]vis[\\\/].*\.js$/, // vis.js files
                loader: "babel-loader",
                query: {
                    cacheDirectory: true,
                    presets: [
                        [
                            "@babel/preset-env",
                            {
                                targets: {
                                    browsers: ["last 2 versions", "safari >= 7"]
                                }
                            }
                        ]
                    ],
                    plugins: [
                        "transform-es3-property-literals",
                        "transform-es3-member-expression-literals",
                        "transform-runtime"
                    ]
                }
            },
            {
                test: /\.js|\.jsx/,
                exclude: /node_modules/,
                loader: "babel-loader",
                query: {
                    cacheDirectory: true,
                    presets: [
                        [
                            "@babel/preset-env",
                            {
                                targets: {
                                    browsers: ["last 2 versions", "safari >= 7"]
                                }
                            }
                        ]
                    ],
                    plugins: [["@babel/plugin-proposal-decorators", { legacy: true }], "lodash"]
                }
            },
            {
                test: /\.(gif|png|jpe?g|ttf|eot|svg)$/i,
                use: ["url-loader"]
            },
            {
                // Match woff2 and patterns like .woff?v=1.1.1.
                test: /\.(woff|woff2)?(\?v=\d+\.\d+\.\d+)?$/,
                use: {
                    loader: "url-loader",
                    options: {
                        mimetype: "application/font-woff"
                    }
                }
            },
            {
                test: /\.css$/,
                use: [
                    {
                        loader: "style-loader"
                    },
                    "css-loader"
                ]
            }
        ]
    },
    plugins: plugins,

    devServer: {
        port: 3000,
        contentBase: PATHS.build,
        historyApiFallback: true,
        watchOptions: {
            poll: 1000
        },
        proxy: {
            "/api/*": {
                secure: false,
                changeOrigin: true,
                target: "http://oscars-backend:8201/"
            },
            "/protected/*": {
                secure: false,
                changeOrigin: true,
                target: "http://oscars-backend:8201/"
            },
            "/services/*": {
                secure: false,
                changeOrigin: true,
                target: "http://oscars-backend:8201/"
            },
            "/admin/*": {
                secure: false,
                changeOrigin: true,
                target: "http://oscars-backend:8201/"
            }
        }
    }
};
