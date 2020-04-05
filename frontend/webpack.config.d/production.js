//karmaでテストしているときはconfig.devtool== falseになるのを利用して判定(Kotlin/JSのバージョンが変わると判定できなくなるかもしれない)
var isKarmaTest = !config.devtool

if (!isKarmaTest) {
    config.externals = {
        'firebase': 'firebase',
        'firebaseui': 'firebaseui'
    }
}

if (config.devServer) {
    config.devServer.proxy = {
        '/hello': 'http://localhost:8081'
    }
}