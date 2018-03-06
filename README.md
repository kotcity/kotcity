# KotCity 0.4

## WARNING WARNING WARNING

This is _pre alpha_ software with super obvious bugs, rough edges etc. In the spirit of "release early and release often"
I am posting the code. This project is far from done but I prefer to get the code out there
to be used by whomever. Disagree with the project? Fork it :) 

## Obligatory Screenshot

![KotCity Screenshot](screenshot.jpg?raw=true "Optional Title")


## What is KotCity? 

KotCity is a city simulator inspired by the statistical city simulators of old.
This game aims to achieve a mark somewhere between SimCity(1989) and SC2000. Hopefully this mark will be hit and we can set our sights higher.

## How do I run it?

Be on the lookout for super easy to install package soon... until then...
* install JDK 1.8+
* Clone the project
* ./gradlew run

## How can I work on the project?

Easy... 
* grab the free version of Intellij from here... https://www.jetbrains.com/idea/
* Import this as Gradle project... 
* done! 

The UI is done with FXML created with Gluon's SceneBuilder http://gluonhq.com/products/scene-builder/

## Long Term Goal

To make a game that "takes over" from SimCity 4. We have a loooooong way to go.

## FAQ

Q: Why 2D?

A: This project is a lot of work already without having to worry about 3D modeling and so forth. One of my bottlenecks is art, so 2D is an easy way to sidestep that concern. Additionally, the actual "renderers" for the game are kept semi-separate from the simulation, so there's no reason why this couldn't turn into 3D later.

Q: Why Kotlin?

A: It has a lot of libraries (pick any random Java library...) It's pretty productive! Gee-whiz functional stuff baked in. Besides, if this project gets to a place where it's really awesome but just needs extra speed we can reach for that C++ or Rust book.

## Why another city project?

After many years of not seeing any new city builders descend that are satisfactory, I decided to take matters into my own hands. Why? SimCity 2013 was REALLY disappointing. Cities Skylines is fun... but it doesn't seem to scratch that itch that SimCity 4 does. Even though there are still patches and new content coming out for SimCity 4, it's definitely on life support. I looked around at a few of the city simulators available but it doesn't seem like anyone is really working on a modern version of SimCity. 

## Current Status

* GPU accelerated graphics
* Map generation (simplex noise based)
* "Perfect" A* pathfinding
* Zoning of residential, commercial, industrial, similar to SimCity
* Moddable / pluggable buildings (supply your own custom buildings)
* Save / Load city
* Data overlays for traffic, desirability, natural resources
* Multi-threaded engine that allows for speedy traffic / city calculations
* User-defined map size (can your PC handle 100km^2 ? Go for it!)
* Power plants and power coverage
* Dynamic supply / demand economy where goods / services / labor are exchanged

## Roadmap

* Implement land values
* Have traffic affect desirability
* Implement "zots" showing what buildings are happy/sad about
* Bus stations
* Rail station, rails
* More types of power plant (hydro, wind, etc)
* Create buildings that use resources under the ground (coal, etc)
* Implement "module upgrade" system from SimCity 2013 (upgrades to power plants etc)
* Improve graphics
* Obtain sound effects / music
* Add many, many additional types of buildings
* Add "mod manager" (think Steam workshop... SC4 has many mods but they really suck to obtain/install)

## How can I help?

* Contribute buildings (see assets directory)
* Ideas for game
* Help with art
* Performance improvements
* Spread word about the project!
* Any other ideas? Contact kotcity@zoho.com via email.