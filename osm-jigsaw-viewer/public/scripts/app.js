function resolvePoint(lat, lon) {
    $.ajax({
        url: "/click?lat=" + lat + "&lon=" + lon
    }).done(function(data) {
        $('#reverse').html(data);
        $('#node').html("");
    });
}

mymap.on('click', function(e) {
    var latlng = e.latlng.wrap();
    resolvePoint(latlng.lat, latlng.lng);
});

var geoOptions = {
    enableHighAccuracy : true,
    timeout : 10000,
    maximumAge : 3000
};

var locate = $('#locate');
locate.on('click', function(e) {
  e.preventDefault();

  function showPosition(position) {
      resolvePoint(position.coords.latitude,  position.coords.longitude);
      // TODO zoom to enclose the smallest component of the resolved name; API will need to make this available
  }

  function showError() {
    // TODO Show field error message
  }

  navigator.geolocation.getCurrentPosition(showPosition, showError, geoOptions);
})