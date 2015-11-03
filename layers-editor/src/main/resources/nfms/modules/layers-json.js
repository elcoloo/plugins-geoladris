define(["text!../layers.json", "message-bus"], function(layersjson, bus) {

	var config = JSON.parse(layersjson);

	function getPortalLayer(id) {
		return find(config.portalLayers, "id", id);
	}

	function getWmsLayer(id) {
		return find(config.wmsLayers, "id", id);
	}

	function getGroup(id) {
		return find(config.groups, "id", id, "items");
	}

	function updateGroup(input, callback) {
		var group = getGroup(input.id);
		copy(group, input);

		upload(callback);
	}

	function updateLayer(wmsLayerInput, portalLayerInput, callback) {
		var wmsLayer = getWmsLayer(wmsLayerInput.id);
		copy(wmsLayer, wmsLayerInput);

		var portalLayer = getPortalLayer(portalLayerInput.id);
		copy(portalLayer,  portalLayerInput);

		upload(callback);
	}

	function copy(target, source) {
		// Copy over new values
		for (var name in source) {
			target[name] = source[name];
		}

		// Delete all properties not in source
		for (var key in target) {
			if(!source.hasOwnProperty(key)) {
				delete target[key];
			}
		}

		return target;
	}

	function find(arr, key, value, recurse) {
		for (var i = 0; i < arr.length; i++) {
			var el = arr[i];
			if (el.hasOwnProperty(key) && el[key] == value) {
				return el;
			} else if (recurse && el.hasOwnProperty(recurse)) {
				el = find(el[recurse], key, value, recurse);
				if (el) {
					return el;
				}
			}
		}
	}

	function upload(onSuccess) {
		bus.send("ajax", {
			type: 'PUT',
			url: 'layers.json',
			contentType: "application/json; charset=utf-8",
			data: JSON.stringify(config, null, 4),
			success: function(data, textStatus, jqXHR) {
				if (onSuccess) {
					onSuccess.call(null, config);
				}
			},
			errorMsg: "Error uploading layers.json to the server" // TODO i18n
		});
	}

	return {
		getPortalLayer: getPortalLayer,
		getWmsLayer: getWmsLayer,
		getGroup: getGroup,
		updateGroup: updateGroup,
		updateLayer: updateLayer
	};
});
