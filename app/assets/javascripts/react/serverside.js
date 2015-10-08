var React = require("react");

require("node-jsx").install();

var Test = React.createFactory(require("./Test.jsx"));
var DataScript = React.createFactory(require("./DataScript.jsx"));


// take data from parameters
var data = JSON.parse(process.argv[2]);

var dataScript = React.renderToString(DataScript({
	idTag: "data",
	jsonData: data
}));
var container = React.renderToString(Test({
	info: data
}));

console.log(dataScript + container);