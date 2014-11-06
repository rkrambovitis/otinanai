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
      plotAccordingToChoices();
   });

   $("#placeholder").bind("plotunselected", function (event, ranges) {
      xmin = null;
      xmax = null;
      ymin = null;
      ymax = null;
      plotAccordingToChoices();
   });

   $("#placeholder").bind("plothover",  function (event, pos, item) {
      latestPosition = pos;
      if (!updateLegendTimeout) {
         updateLegendTimeout = setTimeout(updateLegend, 50);
      }
      updateLegendTimeout = false;
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
         } else {
            for (j = 0; j < val.data.length; ++j) {
               if (val.data[j][0] > pos.x) {
                  break;
               }
            }
            var y = val.data[j][1];
            temp[i].innerHTML = key + " = " + addSuffix(y);
         }
         i++;
      });
   };

   function updatePlot(data) {
      if (data.length > 0) {
         console.log(data);
         myPlot=$.plot("#placeholder", data, {
            legend: { position: "nw", show: "true" },
            xaxis: { mode: "time", tickDecimals: 0, timezone: "browser", min: xmin, max:xmax},
            yaxis: { show: true, min: ymin, max: ymax},
            series: { stack: true, lines: {show: true, fill: true}},
            crosshair: { mode: "x"},
            grid: { hoverable: true, autoHighlight: false},
            selection: { mode: "x" }
         });
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
