# KotCity 0.42

[![Build Status](https://semaphoreci.com/api/v1/kotcity/kotcity/branches/master/badge.svg)](https://semaphoreci.com/kotcity/kotcity) [![Build Status](https://travis-ci.org/kotcity/kotcity.svg?branch=master)](https://travis-ci.org/kotcity/kotcity)

## WARNING!

This is _pre alpha_ software with super obvious bugs, rough edges etc. In the spirit of "release early and release often", I am posting the code. This project is far from done but I prefer to get the code out there to be used by whomever. Disagree with the project? Fork it :).

![KotCity Screenshot](screenshot.jpg?raw=true "Screenshot of the game's UI and an example city")

## What is KotCity?

KotCity is a city simulator written in Kotlin inspired by the statistical city simulators of old. This game aims to achieve a mark somewhere between SimCity (1989) and SC2000. Hopefully this mark will be hit and we can set our sights higher. The game will be fully supported on Windows, macOS, and Linux.

## Gimme the Software!

A build for Windows is available at https://github.com/kotcity/kotcity/releases/tag/0.42.

macOS and Linux build available upon request (or build it yourself!)

(note, on Ubuntu do apt-get install openjfx)

## Quick Start

Be on the lookout for super easy to install package soon... until then...
* Install JDK 1.8+.
* Clone the project.
* Run Gradle using ./gradlew run.

It's easy to setting up the development environment. You can use either IntelliJ or other IDEs supporting Gradle, then import this as a Gradle project. Voilà, the project can be worked on.

The UI is done with FXML created with [Gluon's SceneBuilder](http://gluonhq.com/products/scene-builder/ "Gluon's SceneBuilder").

## FAQ

Q: Why 2D?  
A: This project is a lot of work already without having to worry about 3D modeling and so forth. One of my bottlenecks is art, so 2D is an easy way to sidestep that concern. Additionally, the actual "renderers" for the game are kept semi-separate from the simulation, so there's no reason why this couldn't turn into 3D later.

Q: Why Kotlin?  
A: It has a lot of libraries (pick any random Java library...) It's pretty productive! Gee-whiz functional stuff baked in. Besides, if this project gets to a place where it's really awesome but just needs extra speed we can reach for that C++ or Rust book.

## Why Another City Simulator?

After many years of not seeing any new city builders descend that are satisfactory, I decided to take matters into my own hands. Why? SimCity 2013 was REALLY disappointing. Cities Skylines is fun, but it doesn't seem to scratch that itch that SimCity 4 does. Even though there are still patches and new content coming out for SimCity 4, it's definitely on life support. I looked around at a few of the city simulators available but it doesn't seem like anyone is really working on a modern version of SimCity.

## Community

If you get stuck or want to suggest suggestions, you can discuss it in [our topic on Simtropolis](https://community.simtropolis.com/forums/topic/74899-announcement-kotcity-an-open-source-city-simulator/ "Our topic on Simtropolis"). Chat with the developers at https://gitter.im/kotcity/Lobby.

## Contribution

You can contribute buildings (see assets directory), ideas for the game, help with art and so on by creating issues or fork the repo and start to make pull requests.

By submitting the work to the repository, you're agree that your work should be compatible with Apache License 2.0 and assets license (undecided for now).

## Current Status

* GPU accelerated graphics.
* Map generation (simplex noise based).
* "Perfect" A* pathfinding.
* Zoning of residential, commercial, industrial, similar to SimCity.
* Moddable buildings.
* City saving and loading.
* Data overlays for traffic, desirability, natural resources.
* Multi-threaded engine that allows for speedy traffic / city calculations.
* As-you-want map size (Can your PC handle 100km^2? Go for it!).
* Power plants and coverage.
* Dynamic economy where goods, services and labor are exchanged.
* Happiness (available in separate branch).

## Future Plans

To make a game that "takes over" from SimCity 4. We have a loooooong way to go:
* Implement land values.
* Have traffic affect desirability.
* Implement "zots" showing what buildings are happy/sad about.
* Bus and rail and stations.
* More types of power plant (hydro, wind, etc).
* Create buildings that use resources under the ground (coal, etc).
* Implement "module upgrade" system from SimCity 2013 (upgrades to power plants etc).
* Improve graphics.
* Obtain sound effects / music.
* Add many, many additional types of buildings.
* Add "mod manager" (think Steam workshop... SC4 has many mods but they really suck to obtain/install).

## License

This project is licensed with Apache License 2.0. Assets license currently undecided. This project includes icons by various graphic designers from the Noun Project.
