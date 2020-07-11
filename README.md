# AutoAssist ([Demo](https://www.youtube.com/watch?v=zMs16MfMwWg))

AutoAssist is a proof of concept app which was developed in the context of the
TK3: Ubiquitous Systems lecture in the summer term of 2020.
The app was developed by group 25 (Bock, Moritz; Nazir, Sibgha; Sell, Jonas).

## Scenarios

The app supports the user in three scenarios:

 * when in need to buy something
 * when running and listening to music (i.e. jogging)
 * when going to the cinema.

### Shopping ([Demo](https://www.youtube.com/watch?v=zMs16MfMwWg&t=13))

The user needs to buy something but is not in immediate need of
it.
They add the wanted items to their shopping list and enable the shopping
reminder in the app.

Next time they are near a shop, AutoAssists reminds them that a shop is near and
they wanted to buy something. Also - if available - the app offers via a button
the possibility to directly open the preferred notes-app.

**Used sensors:** GPS (hard sensor), action-enabled-button (soft sensor)

**Used context types:** Sensed context (location, action-enabled)

**Issues:** Often, a preferred notes-app is not set in the operating system. The
button is then hidden.

**Potential improvements¹:** The app could also implement the shopping list
itself or use an API to access the current notes of the user. Then, the app
could check if the shopping list is empty and automatically turn itself on/off.
This would replace the action-enabled-button and its context. The new context
would then be inferred via soft virtual sensors (the notes-app).

### Running ([Demo](https://www.youtube.com/watch?v=zMs16MfMwWg&t=66))

This scenario bases on the idea of speed-dependent volume control in cars.
When the user is running, the user hears more noise than when normal walking
(e.g. pumping blood, headphones-cable rubbing the clothes).
Thus, AutoAssist increases the music volume by a certain percentage when
the user starts running and decreases the volume when the they stop running.

**Used sensors:** activity-state (hard sensor), action-enabled-button
(soft sensor)

**Used context types:** Sensed context (action-enabled), inferred context
(activity-state)

**Potential improvements¹:** Increase the volume depending on the running speed.
This would require the usage of either the accelerometer or the GPS sensor
(both hard sensors in an inferred context).

### Cinema ([Demo](https://www.youtube.com/watch?v=zMs16MfMwWg&t=89))

The user wants to watch a movie in a cinema. They buy a ticket at the counter,
walk into the auditorium and sits down, waiting for the movie to start.
AutoAssist detects the new state, still, checks if the user is near a cinema
and (if that is the case) then enables the do-not-disturb mode.

**Used sensors:** activity-state, GPS (hard sensors), action-enabled-button
(soft sensor)

**Used context types:** Sensed context (GPS, action-enabled), inferred context
(activity-state)

**Potential improvements¹:** Improve detecting whether the user is really in a
cinema. E.g. by using the light sensors to detect whether it is dark, checking
the calendar if there is an entry for the cinema, ...


¹: Potential improvements are not implemented into the app due to the scope of
the project or time constraints.

## Further notes

### Permissions

The app only requires the permissions it really needs:

 * `ACTIVITY_RECOGNITION`: Detect the users activity state and changes.
 * `ACCESS_NOTIFICATION_POLICY`: Automatically enable do-not-disturb.
 * `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`: Get the users location.

### Proof of concept

This app is only a proof of concept and should not be used outside of
demonstrating the concepts. The power consumption may be high because it always
requests location updates and state-changes even when the scenarios are turned
off.
Thus, we also decided not to add an app icon or change the package-name of the
app (i.e. replacing the `com.example`) and we decided to add a fairly generic
name.

### Attribution

#### OSM

The app uses and contains data
from [OpenStreetMap](https://www.openstreetmap.org/):
Store and cinema locations across Hesse,
Germany. Because an API for finding those locations on the fly over the world
costs money, we simply downloaded locations of those entities from OSM via
[overpass turbo](http://overpass-turbo.eu/) using the following two queries:

Cinemas in Hesse:
```
[out:json][timeout:25];
{{geocodeArea:Hessen}}->.searchArea;
(
  node["amenity"="cinema"](area.searchArea);
  way["amenity"="cinema"](area.searchArea);
  relation["amenity"="cinema"](area.searchArea);
);
out center;
```

Supermarkets in Hesse:
```
[out:json][timeout:180];
{{geocodeArea:Hessen}}->.searchArea;
(
  node["shop"="supermarket"](area.searchArea);
  way["shop"="supermarket"](area.searchArea);
  relation["shop"="supermarket"](area.searchArea);
);
out center;
```

#### Haversine implementation

For the approximate distance calculation we use a
[haversine implementation](https://github.com/jasonwinn/haversine/blob/master/Haversine.java)
by [Jason Winn](http://jasonwinn.org).
