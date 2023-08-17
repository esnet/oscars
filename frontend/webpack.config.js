let packageJSON = require("./package.json");
let path = require("path");
let HtmlWebpackPlugin = require("html-webpack-plugin");
let MiniCssExtractPlugin = require("mini-css-extract-plugin");

let webpack = require("webpack");

let LodashModuleReplacementPlugin = require("lodash-webpack-plugin");
// let BundleAnalyzerPlugin = require("webpack-bundle-analyzer").BundleAnalyzerPlugin;

const PATHS = {
    build: path.join(__dirname, "build"),
};

// best for prod
let devtool = false;

let plugins = [
    new webpack.DefinePlugin({
        "process.env": {
            NODE_ENV: JSON.stringify("production")
        },
        __VERSION__: JSON.stringify(packageJSON.version)
    }),
    new webpack.ContextReplacementPlugin(/moment[/\\]locale$/, /en/),
    new webpack.optimize.OccurrenceOrderPlugin(),
    new webpack.optimize.AggressiveMergingPlugin(),
    new LodashModuleReplacementPlugin({ currying: true, flattening: true }),
    new MiniCssExtractPlugin({
        filename: "styles.css"
    }),
    new HtmlWebpackPlugin({
        template: "./public/index.html",
        inject: "body",
        favicon: "./public/favicon.ico"
    })
    // enable to view module sizes on a browser window
    //    new BundleAnalyzerPlugin(),
];

module.exports = {
    entry: ["@babel/polyfill", "./src/main/js/index.js"],
    devtool: devtool,
    cache: true,
    mode: "production",

    output: {
        path: PATHS.build,
        publicPath: '/',
        filename: "bundle.js"
    },
    performance: {
        hints: false
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
                        "transform-es3-property-literals", // see https://github.com/almende/vis/pull/2452
                        "transform-es3-member-expression-literals", // see https://github.com/almende/vis/pull/2566
                        "transform-runtime", // see https://github.com/almende/vis/pull/2566
                        "lodash"
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
                use: [MiniCssExtractPlugin.loader, "css-loader"]
            }
        ]
    },
    plugins: plugins
};
