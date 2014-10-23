$(function() {
   var i = 0;
   $.each(datasets, function(key, val) {
      val.color = i;++i;
   });

   var choiceContainer = $("#choices");

   $.each(datasets, function(key, val) {
      choiceContainer.append("<br/><input type='checkbox' name='" + key + "' checked='checked' id='id" + key + "'></input>" 
         + "<label for='id" + key + "'>" + key + "</label> ");
   });

   choiceContainer.find("input").click(plotAccordingToChoices);
   var updateLegendTimeout = null;
   var latestPosition = null;

   var myPlot = null;
   var myOverview = null;
   updatePlot(datasets);

   var legends = $("#placeholder .legendLabel");

   legends.each(function () {
      $(this).css('width', $(this).width());
   });

   var xmin = null;
   var xmax = null;
   var ymin = null;
   var ymax = null;
   $("#placeholder").bind("plotselected", function (event, ranges) {
      xmin = ranges.xaxis.from;
      xmax = ranges.xaxis.to;
//   ymin = ranges.yaxis.from;
//     ymax = ranges.yaxis.to;
      plotAccordingToChoices();
   });

   /*
   $("#overview").bind("plotselected", function (event, ranges) {
      xmin = ranges.xaxis.from;
      xmax = ranges.xaxis.to;
      ymin = ranges.yaxis.from;
      ymax = ranges.yaxis.to;
      plotAccordingToChoices();
   });
   */

   $("#placeholder").bind("plotunselected", function (event, ranges) {
      xmin = null;
      xmax = null;
      ymin = null;
      ymax = null;
      plotAccordingToChoices();
   });

   /*
   $("#overview").bind("plotunselected", function (event, ranges) {
      xmin = null;
      xmax = null;
      ymin = null;
      ymax = null;
      plotAccordingToChoices();
   });
   */

   $("#placeholder").bind("plothover",  function (event, pos, item) {
      latestPosition = pos;
      if (!updateLegendTimeout) {
         updateLegendTimeout = setTimeout(updateLegend, 50);
      }
      updateLegendTimeout = null;
   });

   function updateLegend() {
      var temp = $('#placeholder .legendLabel');
      var pos = latestPosition;
      var axes = myPlot.getAxes();
      if (pos.x < axes.xaxis.min || pos.x > axes.xaxis.max || pos.y < axes.yaxis.min || pos.y > axes.yaxis.max) {
         return;
      }
      var j,i = 0;
      $.each(datasets, function(key, val) {
         if (val.data.length <= 2) {
            //console.log("Not enough data");
         } else {
            for (j = 0; j < val.data.length; ++j) {
               if (val.data[j][0] > pos.x) {
                  break;
               }
            }
            var y = val.data[j][1];
            temp[i].innerHTML = key + " = " + addSuffix(y);
            //console.log(i+" "+val.data.length);
         }
         i++;
      });
   };

   function updatePlot(data) {
      if (data.length > 0) {
         currentData=data;
         myPlot=$.plot("#placeholder", data, {
            legend: { position: "nw", show: "true" },
            xaxis: { mode: "time", tickDecimals: 0, timezone: "browser", min: xmin, max:xmax},
            yaxis: {show: true, min: ymin, max: ymax},
            series: { lines: {show: true, fill: false}},
            crosshair: { mode: "x"},
            grid: { hoverable: true, autoHighlight: false},
            selection: { mode: "x" }
         });
         /*
         myOverview=$.plot("#overview", data, {
            legend: { show: false},
            series: { lines: {show: true, lineWidth: 1 }, shadowSize: 0 },
            xaxis: {show: false, ticks: 4, min: null, max: null},
            yaxis: {show: false, ticks: 3, min: null, max: null},
            grid: {color: "#999" },
            selection: { mode: "xy" }
         });
         */
      }
   }

   function plotAccordingToChoices() {
      var data = [];
      choiceContainer.find("input:checked").each(function () {
         var key = $(this).attr("name");
         if (key && datasets[key]) {
            data.push(datasets[key]);
         }
      });
      updatePlot(data);
      updateLegend();
   }

   plotAccordingToChoices();

});
