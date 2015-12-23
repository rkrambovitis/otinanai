var el = document.getElementById('sortable');
var mySortable = Sortable.create(el, { 
	handle: ".draggable",
	store: {
		get: function (sortable) {
			var order = localStorage.getItem(sortable.options.group);
			return order ? order.split('|') : [];
		},
		set: function(sortable) {
			var order = sortable.toArray();
			updateDashboard(order);
		}
	} 
});

function updateDashboard(foo) {
/*
	for (var blah of foo) {
		console.log(blah);
	}
*/
	var xhttp = new XMLHttpRequest();
        var urlToStar = encodeURIComponent(foo.toString() + "--updateDashboard");
	xhttp.open("GET", urlToStar, true);
/*
	xhttp.onreadystatechange = function() {
		if (xhttp.responseText == "true") {
			element.className = "starGraph fa fa-star fa-2x";
		} else {
			element.className = "starGraph fa fa-star-o fa-2x";
		}
	}
*/
	xhttp.send();
}
