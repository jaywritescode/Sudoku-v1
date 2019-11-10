const path = require('path');
const webpack = require('webpack');

const PATHS = {
  app: path.join(__dirname, 'app'),
  build: path.join(__dirname, 'src/main/resources/public'),
  style: path.join(__dirname, 'styles')
};

module.exports = {
  entry: {
    app: PATHS.app
  },
  resolve: {
    extensions: ['.js', '.jsx']
  },
  output: {
    path: PATHS.build,
    filename: 'bundle.js'
  },
  devServer: {
    contentBase: PATHS.build,
    historyApiFallback: true,
    hot: true,
    inline: true,
    progress: true,
    stats: 'errors-only',
    host: process.env.HOST,
    port: process.env.PORT,
    proxy: {
      '/': 'http://localhost:8080',     // the local backend runs on port 8080
    }
  },
  plugins: [
    new webpack.HotModuleReplacementPlugin()
  ],
  module: {
    rules: [
      { test: /\.jsx?$/, use: 'babel-loader' },
      {
        test: /\.s(a|c)ss$/,
        use: ['style-loader', 'css-loader', 'sass-loader'],
      },
    ]
  },
  devtool: 'eval-source-map'
};
