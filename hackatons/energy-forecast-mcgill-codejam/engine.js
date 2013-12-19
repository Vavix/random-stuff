var FUNC_CONSTANT = 12620.1;
var FUNC_NET_RADIATION_COEFFICIENT = 3.8711;
var FUNC_RELATIVE_HUMIDITY_COEFFICIENT = 1386.07;
var FUNC_TEMPERATURE_COEFFICIENT = 77.9583;
var FUNC_WIND_SPEED_COEFFICIENT = -13.0994;
var FUNC_YEARLY_CYCLE_COEFFICIENT = -9.39453;
var FUNC_WEEKlY_CYCLE_COEFFICIENT = 1761.95;
var FUNC_HOURLY_CYCLE_COEFFICIENT = 304.648;

var fs = require("fs");
var moment = require("moment");

function SourceData(date, netRadiation, relativeHumidity, temperature, windSpeed, powerConsumption) {

    this.date = moment(date);

    this.yearlyCycle = this.date.week();
    this.yearlyCycle += this.date.weekday() / 7;
    if (this.yearlyCycle > 52) {
        this.yearlyCycle -= 52;
    }
    if (this.yearlyCycle > 26) {
        this.yearlyCycle = 52 - this.yearlyCycle;
    }

    this.weeklyCycle = 0;
    if (this.date.weekday() >= 1 && this.date.weekday() <= 5)  {
        this.weeklyCycle = 1;
    }

    this.hourlyCycle = this.date.hour();
    this.hourlyCycle += this.date.minute() / 60;
    this.hourlyCycle -= 4;
    if (this.hourlyCycle < 0) {
        this.hourlyCycle += 24;
    }
    if (this.hourlyCycle > 12) {
        this.hourlyCycle = 24 - this.hourlyCycle;
    }

    this.netRadiation = parseFloat(netRadiation);
    this.relativeHumidity = parseFloat(relativeHumidity);
    this.temperature = parseFloat(temperature);
    this.windSpeed = parseFloat(windSpeed);

    this.powerConsumption = parseFloat(powerConsumption);
}

function PredictionData(date, powerConsumption) {

    this.date = date;
    this.powerConsumption = powerConsumption;
}

exports.createPrediction = function(sourceDataArray) {

    var predictionDataArray = [];

    for (var i = 0; i < sourceDataArray.length; i++) {
        if (isNaN(sourceDataArray[i].powerConsumption)) {

            var predictionData;
            if (i >= 16 && !isNaN(sourceDataArray[i - 16].powerConsumption)) {
                predictionData = new PredictionData(sourceDataArray[i].date,
                    6510.9 - 18.0991 * sourceDataArray[i].yearlyCycle + 963.634 * sourceDataArray[i].weeklyCycle + 93.2403 * sourceDataArray[i].hourlyCycle +
                        5.40352 * sourceDataArray[i].netRadiation + 1339.55 * sourceDataArray[i].relativeHumidity + 29.0862 * sourceDataArray[i].temperature - 10.6617 * sourceDataArray[i].windSpeed +
                        0.480158 * sourceDataArray[i - 16].powerConsumption)
            }
            else if (i >= 32 && !isNaN(sourceDataArray[i - 32].powerConsumption)) {

                predictionData = new PredictionData(sourceDataArray[i].date,
                    9357.09 - 19.7109 * sourceDataArray[i].yearlyCycle + 1454.76 * sourceDataArray[i].weeklyCycle + 274.63 * sourceDataArray[i].hourlyCycle +
                        5.59363 * sourceDataArray[i].netRadiation + 1441.94 * sourceDataArray[i].relativeHumidity + 53.834 * sourceDataArray[i].temperature -
                        11.8805 * sourceDataArray[i].yearlyCycle + 0.21905 * sourceDataArray[i - 32].powerConsumption);
            }
            else if (i >= 48 && !isNaN(sourceDataArray[i - 48].powerConsumption)) {

                predictionData = new PredictionData(sourceDataArray[i].date,
                    6404.99 - 21.9362 * sourceDataArray[i].yearlyCycle + 1395.7 * sourceDataArray[i].weeklyCycle + 433.186 * sourceDataArray[i].hourlyCycle +
                        + 5.17947 * sourceDataArray[i].netRadiation + 1125.6 * sourceDataArray[i].relativeHumidity + 44.7276 * sourceDataArray[i].temperature -
                        10.656 * sourceDataArray[i].windSpeed + 0.356718 * sourceDataArray[i - 48].powerConsumption);
            }
            else if (i >= 64 && !isNaN(sourceDataArray[i - 64].powerConsumption)) {

                predictionData = new PredictionData(sourceDataArray[i].date,
                    4053.53 - 10.534 * sourceDataArray[i].yearlyCycle + 1441.31 * sourceDataArray[i].weeklyCycle + 552.85 * sourceDataArray[i].hourlyCycle +
                        + 1.95492 * sourceDataArray[i].netRadiation + 431.812 * sourceDataArray[i].relativeHumidity + 44.3712 * sourceDataArray[i].temperature -
                        9.83887 * sourceDataArray[i].windSpeed + 0.48279 * sourceDataArray[i - 64].powerConsumption);
            }
            else {
                predictionData = new PredictionData(sourceDataArray[i].date,
                    FUNC_CONSTANT + FUNC_NET_RADIATION_COEFFICIENT * sourceDataArray[i].netRadiation +
                        FUNC_RELATIVE_HUMIDITY_COEFFICIENT * sourceDataArray[i].relativeHumidity +
                        FUNC_TEMPERATURE_COEFFICIENT * sourceDataArray[i].temperature +
                        FUNC_WIND_SPEED_COEFFICIENT * sourceDataArray[i].windSpeed +
                        FUNC_YEARLY_CYCLE_COEFFICIENT * sourceDataArray[i].yearlyCycle +
                        FUNC_WEEKlY_CYCLE_COEFFICIENT * sourceDataArray[i].weeklyCycle +
                        FUNC_HOURLY_CYCLE_COEFFICIENT * sourceDataArray[i].hourlyCycle);
            }
            predictionDataArray.push(predictionData);
        }
    }

    return predictionDataArray;
};

exports.createCsv = function(predictionDataArray) {
    var csvString = "Date,Power Demand\n";

    for (var i = 0; i < predictionDataArray.length; i++) {
        csvString += predictionDataArray[i].date.toISOString() + "," + predictionDataArray[i].powerConsumption;
        csvString += "\n"
    }

    return csvString;
};

exports.parsePulse = function(data, callback) {

    var sourceDataArray = [];

    for (var i = 0; i < data.power.length; i++) {

        var date = moment(data.power[i][0]);

        var power = data.power[i][1];

        var netRadiation = "";
        for (var j = 0; j < data.netRadiation.length; j++) {
            if (date.isSame(moment(data.netRadiation[j][0]))) {
                netRadiation = data.netRadiation[j][1];
                break;
            }
        }

        var relativeHumidity = "";
        for (var j = 0; j < data.relativeHumidity.length; j++) {
            if (date.isSame(moment(data.relativeHumidity[j][0]))) {
                relativeHumidity = data.relativeHumidity[j][1];
                break;
            }
        }

        var temperature = "";
        for (var j = 0; j < data.temperature.length; j++) {
            if (date.isSame(moment(data.temperature[j][0]))) {
                temperature = data.temperature[j][1];
                break;
            }
        }

        var windSpeed = "";
        for (var j = 0; j < data.windSpeed.length; j++) {
            if (date.isSame(moment(data.windSpeed[j][0]))) {
                windSpeed = data.windSpeed[j][1];
                break;
            }
        }

        sourceDataArray.push(new SourceData(date.toISOString(), netRadiation, relativeHumidity, temperature, windSpeed,
            power));
    }

    interpolate(sourceDataArray, "netRadiation");
    interpolate(sourceDataArray, "relativeHumidity");
    interpolate(sourceDataArray, "temperature");
    interpolate(sourceDataArray, "windSpeed");

    callback(sourceDataArray);
};

exports.parseCsv = function(filename, callback) {

    fs.readFile(filename, {encoding: "utf-8"}, function(err, data) {

        var sourceDataArray = [];

        var lines = data.split("\n");
        for (var i = 0; i < lines.length; i++) {
            // Skip header and footer line
            if (i == 0 || i == lines.length - 1) {
                continue;
            }

            var values = lines[i].split(",");
            var sourceData = new SourceData(values[0], values[1], values[2], values[3], values[4], values[5]);
            sourceDataArray.push(sourceData);
        }

        interpolate(sourceDataArray, "netRadiation");
        interpolate(sourceDataArray, "relativeHumidity");
        interpolate(sourceDataArray, "temperature");
        interpolate(sourceDataArray, "windSpeed");

        callback(sourceDataArray);
    });
};

// Linear interpolation of weather data
function interpolate(sourceDataArray, attrib) {
    for (var i = 0; i < sourceDataArray.length; i++) {
        var sourceData = sourceDataArray[i];

        if (isNaN(sourceData[attrib])) {

            var start = null;
            var end = null;

            for (var j = i; j >= 0; j--) {
                if (!isNaN(sourceDataArray[j][attrib])) {
                    start = j;
                    break;
                }
            }

            for (var j = i; j < sourceDataArray.length; j++) {
                if (!isNaN(sourceDataArray[j][attrib])) {
                    end = j;
                    break;
                }
            }

            if (start !== null && end !== null) {
                sourceData[attrib] = sourceDataArray[start][attrib] +
                    (sourceDataArray[end][attrib] - sourceDataArray[start][attrib]) *
                        ((i - start) / (end - start))
            }
            else if (start !== null && end === null) {
                sourceData[attrib] = sourceDataArray[start][attrib];
            }
            else if (start === null && end !== null) {
                sourceData[attrib] = sourceDataArray[end][attrib];
            }
            else {
                // Linear interpolation not possible
            }
        }
    }
}