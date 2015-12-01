<!DOCTYPE html>
<html>
	<head>
		<meta charset="utf-8">
		<title>Heatmaps</title>
		<style>
			html, body, #map-canvas {
				height: 100%;
				margin: 0px;
				padding: 0px
			}
			#panel {
				position: absolute;
				top: 5px;
				left: 50%;
				margin-left: -180px;
				z-index: 5;
				background-color: #fff;
				padding: 5px;
				border: 1px solid #999;
			}
		</style>
		<script src="https://maps.googleapis.com/maps/api/js?v=3.exp&signed_in=true&libraries=visualization"></script>
		<script>
			var map, pointarray, heatmap, pointarray2, heatmap2;

			var tweetData = [];
			var tweetData2 = [];

			//var wsUri = "ws://localhost:8080/Twitt-Map/echo";
			var wsUri = "ws://ec2-54-175-27-83.compute-1.amazonaws.com:8080/echo"
		
			function initialize() {

				var mapOptions = {
					zoom: 2,
					center: new google.maps.LatLng(37.774546, -122.433523)
				};

				map = new google.maps.Map(document.getElementById('map-canvas'),
						mapOptions);

				var pointArray = new google.maps.MVCArray(tweetData);
				var pointArray2 = new google.maps.MVCArray(tweetData2);

				heatmap = new google.maps.visualization.HeatmapLayer({
					data: pointArray
				});
				
				heatmap2 = new google.maps.visualization.HeatmapLayer({
						data: pointArray2
					});

				heatmap.setMap(map);
				heatmap2.setMap(map);
				
			}

			setInterval(function(){ update() }, 1000);
				function update() {
					var pointArray = new google.maps.MVCArray(tweetData);
					heatmap.set('data', pointArray);
					var pointArray2 = new google.maps.MVCArray(tweetData2);
					heatmap2.set('data', pointArray2);
				} 

			function toggleHeatmap() {
				heatmap.setMap(heatmap.getMap() ? null : map);
			}

			function changeGradient() {
				var gradient = [
					'rgba(0, 255, 255, 0)',
					'rgba(0, 255, 255, 1)',
					'rgba(0, 191, 255, 1)',
					'rgba(0, 127, 255, 1)',
					'rgba(0, 63, 255, 1)',
					'rgba(0, 0, 255, 1)',
					'rgba(0, 0, 223, 1)',
					'rgba(0, 0, 191, 1)',
					'rgba(0, 0, 159, 1)',
					'rgba(0, 0, 127, 1)',
					'rgba(63, 0, 91, 1)',
					'rgba(127, 0, 63, 1)',
					'rgba(191, 0, 31, 1)',
					'rgba(255, 0, 0, 1)'
				]
				heatmap2.set('gradient', heatmap2.get('gradient') ? null : gradient);
			}

			function changeRadius() {
				heatmap.set('radius', heatmap.get('radius') ? null : 20);
			}

			function changeOpacity() {
				heatmap.set('opacity', heatmap.get('opacity') ? null : 0.2);
			}

			function clearMap() {
					tweetData = [];
					var pointArray = new google.maps.MVCArray(tweetData);
					heatmap.set('data', pointArray);
			}

			
			function sentiment() {
					tweetData = [];
					tweetData2 = [];
					var pointArray = new google.maps.MVCArray(tweetData);
					var pointArray2 = new google.maps.MVCArray(tweetData2);
					heatmap.set('data', pointArray);
					heatmap2.set('data', pointArray2);
					
					var inputString = document.getElementById("filter").value;
					
					websocket = new WebSocket(wsUri);
					websocket.onopen = function() {
							websocket.send("SENTIMENT "+ inputString);
					};
					websocket.onmessage = function(evt) {
							var message = evt.data.split(" ", 3);
							var latitude = parseFloat(message[0]);
							var longitude = parseFloat(message[1]);
							var sentiment = parseInt(message[2]);
							
							if (sentiment > 0) {
								tweetData.push(new google.maps.LatLng(parseFloat(message[0]), parseFloat(message[1])));
							} else if (sentiment < 0) {
								tweetData2.push(new google.maps.LatLng(parseFloat(message[0]), parseFloat(message[1])));
						    }
							
					};
					websocket.onerror = function(evt) {
					};
					websocket.onclose = function(evt) {
					};
					
					
			}

			google.maps.event.addDomListener(window, 'load', initialize);
		</script>
	</head>

	<body>
		<div id="panel">
			<p>Assignment 2 Twitt-Map Sentiment Analysis Wenxin Wang (ww2373) Su Shen (ss4716)</p>
			<p><small>To use the tool, first fill in a filter word (you may leave it blank to show all twitters), then click Tweet Sentiment to get twitter in realtime. By click change color you can get the positive and negative twitter in different color.</small></p>
			<button onclick="sentiment()">Tweet Sentiment</button>
			<i>Filter:</i>
			<input id="filter" type="text">
			<button onclick="clearMap()">Clear map</button>
			<button onclick="toggleHeatmap()">Toggle heatmap</button>
			<button onclick="changeGradient()">Change color</button>
			<button onclick="changeRadius()">Change radius</button>
			<button onclick="changeOpacity()">Change opacity</button>
		</div>
		<div id="map-canvas"></div>
	</body>
</html>