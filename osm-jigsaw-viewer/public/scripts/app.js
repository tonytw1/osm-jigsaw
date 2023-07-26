function resolvePoint(lat, lon) {
    $('#reverse').html("<i class=\"fa fa-spinner\" aria-hidden=\"true\"></i>")
    $('#node').html("")
    $.ajax({
        url: "/click?lat=" + lat + "&lon=" + lon
    }).done(function(data) {
        $('#reverse').html(data);
    });
}

mymap.on('click', function(e) {
    var latlng = e.latlng.wrap();
    resolvePoint(latlng.lat, latlng.lng);
});

mymap.on('locationfound', function(e) {
    var latlng = e.latlng.wrap();

    function showPosition(latlng) {
      resolvePoint(latlng.lat, latlng.lng);
      // TODO zoom to enclose the smallest component of the resolved name; API will need to make this available
      mymap.panTo([latlng.lat, latlng.lng]);
    }

    showPosition(latlng);
});

// TODO location error
function showLocationError(positionError) {
    $('#locate-error').html("<p>Failed to get position: " + positionError.message + "</p>");
}
