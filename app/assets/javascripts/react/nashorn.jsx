var React = require("react");

var global = this;

console = {};
console.debug = function() {};
console.warn = function() {};
console.log = function() {};

var Test = require("./Test.jsx");
var DataScript = require("./DataScript.jsx");

//var test = React.createFactory(Test);
//var script = React.createFactory(DataScript);

//var dataScript = React.renderToString(script({
//	idTag: "data",
//	jsonData: data
//}));

//var content = React.renderToString(test({
//	info: data
//}));

var Server = React.createClass({
    render: function() {
        return (
            <div>
                <DataScript idTag="data" jsonData={data} />
                <Test info={data} />
            </div>
        );
    }
});

//print(content);
var serv = React.createFactory(Server);
//print(JSON.stringify(serv({})));
print(React.renderToString(serv({})));
