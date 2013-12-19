#!/bin/env node
var express = require('express');
var moment = require('moment');
var app = express();
var engine = require("./engine.js");
var pulse = require("./pulse.js");

var ipaddress = process.env.OPENSHIFT_NODEJS_IP || "127.0.0.1";
var port = process.env.OPENSHIFT_NODEJS_PORT || 8080;

// Support file uploading
app.use(express.bodyParser());

// Serve all pages in static directory
app.use("/", express.static("./static"));

// Create internal API
app.get("/internal_api", function(req, res) {

    pulse.getCachedPulseData(function(data) {
        engine.parsePulse(data, function(sourceDataArray) {

            var predictionDataArray = engine.createPrediction(sourceDataArray);

            var obj = [];
            for (var i = 0; i < predictionDataArray.length; i++) {

                var foundWeatherData = false;
                for (var j = 0; j < sourceDataArray.length; j++) {
                    if (sourceDataArray[j].date.isSame(predictionDataArray[i].date)) {

                        obj.push({
                            power: predictionDataArray[i].powerConsumption,
                            netRadiation: sourceDataArray[j].netRadiation,
                            relativeHumidity: sourceDataArray[j].relativeHumidity,
                            temperature: sourceDataArray[j].temperature,
                            windSpeed: sourceDataArray[j].windSpeed,
                            date: predictionDataArray[i].date.toISOString()
                        });
                        foundWeatherData = true;
                        break;
                    }
                }

                if (!foundWeatherData) {
                    obj.push({
                        power: predictionDataArray[i].powerConsumption,
                        netRadiation: "",
                        relativeHumidity: "",
                        temperature: "",
                        windSpeed: "",
                        date: predictionDataArray[i].date.toISOString()
                    });
                }
            }

            var objJson = JSON.stringify(obj);

            res.set("Content-Type", "application/json");
            res.set("Access-Control-Allow-Origin", "*");
            res.send(objJson);
        });
    })
});

app.get("/public_api", function(req, res) {

    res.send("No CSV file submitted");
});

// Create public API
app.post("/public_api", function(req, res) {

    engine.parseCsv(req.files.file.path, function(sourceDataArray) {

        var predictionDataArray = engine.createPrediction(sourceDataArray);
        var csvString = engine.createCsv(predictionDataArray);

        res.set("Content-Type", "text/plain");
        res.send(csvString);
    });
});

// Create server
app.listen(port, ipaddress);
