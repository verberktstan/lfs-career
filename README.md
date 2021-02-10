# lfs-career
Lets you play single player career mode with the Live For Speed racing simulator.
For now its a quick/dirty method. Expect broken functionality, use at your own risk! If you have ideas for improvement, please file an issue!

## Getting started
Before playing the game, copy `config.edn.example` to `config.edn` and make shure the :setup-dir points to your LFS/data/setups directory.

Launch Live For Speed, choose `single player` and type `/insim 29999`.
Start lfs-career by running `clj -m lfs-career.core` or the bash script `bin/lfs-career`.

When you see the following;
`Type exit, quit or stop to quit lfs-career
Connected with LFS S3 / 0.6U (insim-version 8)`
You know that lfs-career is running and succesfully connected to LFS.

## Ingame commands

In LFS, type `!help` to see all the commands needed for lfs-career. As you can see, it uses a simple command interface, no fancy buttons. Be warned!

The core concepts are career, season & race. A career consists of multiple seasons. Completing a season can lead to unlocking new seasons and/or cars. A season consists of multiple races.

### !career
Type `!career`, lfs-career tells you that the requested career is unknown and lists the available careers for you. (`road` and `ow` by default)
Type `!career road` to take part in the road career.
You'll be shown a word of welcome, as well as your currently available cars and seasons. Type `!unlocked` at any moment to see the unlocked/available cars and seasons.

### !season
Type `!season uf1-sprint` to start the UF1 Sprint season. You'll be shown a word of welcome.

### !race
Type `!race` to load the next race of the season. lfs-career loads a track, downloads any needed setups from lfs.net and fills the grid with AIs. Join the race and have fun!
When you finish a race, lfs-career stores the results. You can type `!race` to start the next race of the season. When there are no more races left, no race will be loaded. Type `!end-season` to finish the season and unlock new seasons and/or cars.

### !save & !load
Type `!save my-career` to save the career. It will be stored as `/savegames/my-career.edn`. You can choose any name for your savegame. Type `!load my-career` to restore the savegame and continue racing!

## Configure your own career and seasons
Have a look at `careers/road.edn`. This is an annotated example, should be self explanatory. Make a copy of this file and tweak it to your liking. If you made any mistakes and try to load it in game, you'll see an error. In your terminal window you'll see some spec information, hopefully it'll be helpful.
