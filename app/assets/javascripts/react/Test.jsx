var React = require("react");

var Test = React.createClass({
	render: function() {
		var email = this.props.info.email;
		var type = this.props.info.type;
		var test = this.props.info.test;

		return (
			<div>
				<h1>{test} - {type}</h1>
				<span>{email}</span>
			</div>
		);
	}
});

module.exports = Test;