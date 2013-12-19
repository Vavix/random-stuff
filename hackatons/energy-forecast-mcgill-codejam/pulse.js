var fs = require("fs");
var http = require("http");
var moment = require("moment");

var TIMESTAMP_PATH = "./pulse.timestamp";
var DATA_PATH = "./pulse.json";

var POWER_POINT = 50578;
var NET_RADIATION_POINT = 66094;
var RELATIVE_HUMIDITY_POINT = 66095;
var TEMPERATURE_POINT = 66077;
var WIND_SPEED_POINT = 66096;

var API_KEY = "8A1BA2350ECDEE7F939AEB86D747C09B";

exports.getCachedPulseData = function(callback) {

    fs.exists(TIMESTAMP_PATH, function(exists) {
        if (exists) {
            fs.readFile(TIMESTAMP_PATH, {
                encoding: "UTF-8"
            }, function(err, data) {
                var timestamp = moment(data);
                if (moment().isAfter(timestamp.add("hours", 1))) {
                    exports.getLatestPulseData(callback);
                }
                else {
                    fs.readFile(DATA_PATH, {
                        encoding: "UTF-8"
                    }, function(err, data) {
                        var obj = JSON.parse(data.replace(/\n/g, ""));
                        callback(obj);
                    });
                }
            });
        }
        else {
            exports.getLatestPulseData(callback);
        }
    });
};

exports.getLatestPulseData = function(callback) {

    var latestData = {
        power: null,
        netRadiation: null,
        relativeHumidity: null,
        temperature: null,
        windSpeed: null
    };

    var date = moment().subtract("hours", 8).toISOString();

    createPulseApiRequest(POWER_POINT, date, function(data) {
        latestData.power = data.data;
        createPulseApiRequest(NET_RADIATION_POINT, date, function(data) {
            latestData.netRadiation = data.data;
            createPulseApiRequest(RELATIVE_HUMIDITY_POINT, date, function(data) {
                latestData.relativeHumidity = data.data;
                createPulseApiRequest(TEMPERATURE_POINT, date, function(data) {
                    latestData.temperature = data.data;
                    createPulseApiRequest(WIND_SPEED_POINT, date, function(data) {
                        latestData.windSpeed = data.data;

                        fs.writeFile(TIMESTAMP_PATH, moment().toISOString(), {
                            encoding: "UTF-8"
                        }, function() {
                            fs.writeFile(DATA_PATH, JSON.stringify(latestData), {
                                encoding: "UTF-8"
                            }, function() {
                                callback(latestData);
                            })
                        });
                    });
                });
            });
        });
    });
};

function createPulseApiRequest(point, date, callback) {

    http.get("http://api.pulseenergy.com/pulse/1/points/" + point +
        "/data.json?key=" + API_KEY + "&interval=day&start=" + date, function(res) {

        var output = "";

        res.on("data", function(chunk) {
            output += chunk;
        });

        res.on("end", function() {
            var obj = JSON.parse(output);
            callback(obj);
        })
    });
}