function createChart(chartData, dateData) {

    //Get the context of the canvas element we want to select
    var ctx = document.getElementById("myChart").getContext("2d");

    //Create the data object to pass to the chart
    var data = {
        labels : dateData,
        datasets : [{
            fillColor : "rgba(151,187,205,0.5)",
            strokeColor : "rgba(151,187,205,1)",
            data : chartData
        }]
    };
    var options = {

    };

    //Create the chart
    new Chart(ctx).Line(data, options);
}

