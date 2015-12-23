var sorty = document.getElementById('sortable');
var mySortable = Sortable.create(sorty, {
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

function updateDashboard(order) {
	var xhttp = new XMLHttpRequest();
        var urlToStar = encodeURIComponent(order.toString() + "--updateDashboard");
	xhttp.open("GET", urlToStar, true);
	xhttp.send();
}
