function resolvePoint(lat, lon) = {
    $.ajax({
        url: "/click?lat=" + lat + "&lon=" + lng
    }).done(function(data) {
        $('#reverse').html(data);
        $('#node').html("");
    });
}

var mymap = L.map('mapid').setView([51.505, -0.09], 13);

@areaBoundingBox.map { boundingBox =>
    mymap.fitBounds([
            [@boundingBox._1, @boundingBox._2],
            [@boundingBox._3, @boundingBox._4]
        ]
    );
}

L.tileLayer('https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token={accessToken}', {
    attribution: 'Map data &copy; <a href="https://www.openstreetmap.org/">OpenStreetMap</a> contributors, <a href="https://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="https://www.mapbox.com/">Mapbox</a>',
    maxZoom: 18,
    id: 'mapbox.streets',
    accessToken: '@mapBoxApiKey'
}).addTo(mymap);

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