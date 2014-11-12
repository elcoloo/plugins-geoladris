define([ "jquery", "message-bus", "toolbar", "jquery-ui", "i18n" ], function($, bus, toolbar, ui, i18n) {

	var timestampSet = {};
	var divTimeSlideContainer;
	
	var getLocalizedDate = function(date) {
		var defaultMonths = [ "Jan.", "Feb.", "Mar.", "Apr.", "May", "June", "July", "Aug.", "Sep.", "Oct.", "Nov.", "Dec." ];
		var months = i18n.months ? eval(i18n.months) : defaultMonths;
		var arr = date.split("-");

		if (arr[1]) {
			arr[1] = months[arr[1] - 1];
		}

		return arr[1] + " " + arr[0];
	};

	divTimeSlideContainer = $("<div/>").attr("id", "time_slider_pane");
	divTimeSlideContainer.hide();
	toolbar.append(divTimeSlideContainer);
	
	bus.listen("add-layer", function(event, layerInfo) {
		$.each(layerInfo.wmsLayers, function(index, wmsLayer) {
			if (wmsLayer.hasOwnProperty("timestamps")) {
				var layerTimestamps = wmsLayer.timestamps;

				for (var i = 0; i < layerTimestamps.length; i++) {
					timestampSet[layerTimestamps[i]] = true;
				}
			}
		});
	});

	bus.listen("layers-loaded", function() {
		var timestamps, div, divTimeSliderLabel, lastTimestampIndex;

		timestamps = $.map(timestampSet, function(value, key) {
			return key;
		}).sort();
		lastTimestampIndex = timestamps.length - 1;

		if (timestamps.length > 0) {
			div = divTimeSlideContainer;

			var divTimeSlider = $('<div id="time_slider"/>');
			div.append(divTimeSlider);

			divTimeSliderLabel = $('<div id="time_slider_label"/>');
			div.append(divTimeSliderLabel);

			divTimeSlider.slider({
				change : function(event, ui) {
					var d = new Date();
					d.setISO8601(timestamps[ui.value]);
					bus.send("time-slider.selection", d);
				},
				slide : function(event, ui) {
					divTimeSliderLabel.text(getLocalizedDate(timestamps[ui.value]));
				},
				max : lastTimestampIndex,
				value : lastTimestampIndex
			});

			divTimeSliderLabel.text(getLocalizedDate(timestamps[lastTimestampIndex]));

			div.show();

			// Send time-slider.selection message to show the date on the layer selection pane
			// right after page load
			divTimeSlider.slider("value", lastTimestampIndex);
		}
	});
});
