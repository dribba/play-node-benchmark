var React = require("react");

var DataScript = React.createClass({
	getDefaultProps: function() {
		return {
			idTag: "data",
			jsonData: {}
		};
	},
	getJson: function() {
		return {
			__html: JSON.stringify(this.props.jsonData)
		};
	},

	render: function() {
		return (
			<script id={this.props.idTag} dangerouslySetInnerHTML={this.getJson()} type="application/json">
			</script>
		);

	}
});




module.exports = DataScript;