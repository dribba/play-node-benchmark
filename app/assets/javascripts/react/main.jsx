var React = require("react");
var Test = require("./Test.jsx");


function getData(url, cb) {
	
	var request = new XMLHttpRequest();
	request.open("GET", url, true);

	request.onreadystatechange = function() {
		if (this.readyState === 4) {
			var data = JSON.parse(this.responseText);
			cb(data);
		}
	};

	request.send();
	request = null;
}

function getOrder() {
	var obj;
	var dataElem = document.getElementById("data");
	
	if (dataElem !== null) {
		obj = {
			then: function(then) {
				then(JSON.parse(dataElem.textContent));
			}
		};
	} else {
		obj = {
			then: function(then) {
				getData("/data", then);
			}
		};
	}
	return obj;
}

getOrder().then(function(data) {
	React.render(<Test info={data} />, document.getElementById("content"));
});


