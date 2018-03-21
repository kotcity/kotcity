# KotCity 0.45

[![Build Status](https://semaphoreci.com/api/v1/kotcity/kotcity/branches/master/badge.svg)](https://semaphoreci.com/kotcity/kotcity) [![Build Status](https://travis-ci.org/kotcity/kotcity.svg?branch=master)](https://travis-ci.org/kotcity/kotcity)

## WARNING!

This is _pre-alpha_ software with super obvious bugs, rough edges etc. In the spirit of "release early and release often", I am posting the code. This project is far from done but I prefer to get the code out there to be used by whomever. Disagree with the project? Fork it :)

![KotCity Screenshot](screenshot.jpg?raw=true "Screenshot of the game's UI and an example city")

## New in this version

95% of these are from my main man @sabieber, who is the KotCity hero.

See our roadmap at https://github.com/kotcity/kotcity/wiki/Roadmap

* Displaying population.
* Buildings construct a little more often.
* Tweaked required kots (equivalent to *sims*) for industrial plant.
* Minimal map size reduced (you can start a 32*32 map).
* Right click and drag to pan map.
* Update cursor on map pan.
* Dynamic framerate adjustment during panning & zooming.
* Zoom to mouse position on zoom.

## What is KotCity?

KotCity is a city simulator written in Kotlin inspired by the statistical city simulators of old. This game aims to achieve a mark somewhere between SimCity (1989) and SC2000. Hopefully this mark will be hit and we can set our sights higher. The game will be fully supported on Windows, macOS, and Linux.

## Gimme the Software!

A build for Windows, macOS and Linux is available at https://github.com/kotcity/kotcity/releases/tag/0.45

Java 8+ is required. On Windows it will look for JRE and bring you to download page if you don't have it.

On Ubuntu, you should do ```apt-get install openjfx```.

## Next up!

* Level 2 to 5 buildings.
* Pollution.
* Land value.

## Quick Start

Be on the lookout for super easy to install package soon... until then...
* Install JDK 1.8+.
* Clone the project.
* Run Gradle using ./gradlew run.

It's easy to setting up the development environment. You can use either IntelliJ or other IDEs supporting Gradle, then import this as a Gradle project. Voilà, the project can be worked on.

The UI is done with FXML created with [Gluon's SceneBuilder](http://gluonhq.com/products/scene-builder/ "Gluon's SceneBuilder").

## FAQ

### Q: Why 2D?  
**A:** This project is a lot of work already without having to worry about 3D modeling and so forth. One of my bottlenecks is art, so 2D is an easy way to sidestep that concern. Additionally, the actual "renderers" for the game are kept semi-separate from the simulation, so there's no reason why this couldn't turn into 3D later.

### Q: Why Kotlin?  
**A:** It has a lot of libraries (pick any random Java library...) It's pretty productive! Gee-whiz functional stuff baked in. Besides, if this project gets to a place where it's really awesome but just needs extra speed we can reach for that C++ or Rust book.

### Q: Why Another City Simulator?

**A:** After many years of not seeing any new city builders descend that are satisfactory, I decided to take matters into my own hands. Why? SimCity 2013 was REALLY disappointing. Cities Skylines is fun, but it doesn't seem to scratch that itch that SimCity 4 does. Even though there are still patches and new content coming out for SimCity 4, it's definitely on life support. I looked around at a few of the city simulators available but it doesn't seem like anyone is really working on a modern version of SimCity.

## Community

If you get stuck or want to suggest suggestions, you can discuss it in [our topic on Simtropolis](https://community.simtropolis.com/forums/topic/74899-announcement-kotcity-an-open-source-city-simulator/ "Our topic on Simtropolis"). Chat with the developers at https://gitter.im/kotcity/Lobby. You can also contact the project team by sending an email to kotcity at zoho dot com.

## Contribution

Please refer to CONTRIBUTING.md file for more information.

## License

This project is licensed with Apache License 2.0. This project includes icons by various graphic designers from the Noun Project.
