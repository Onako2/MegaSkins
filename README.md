# MegaSkins

Cooking with a big database of Minecraft skins 🧑‍🍳

Frontend repository: [MegaSkins-frontend](<https://github.com/Onako2/MegaSkins-frontend>)

## Features

* Getting descriptions of Minecraft skins (currently there aren't many, but the database is growing)
* Searching Minecraft skins based on the descriptions
* Checking how safe or unsafe a Minecraft skin is on a scale between 0 and 1 (NSFW-checker), this works by comparing input against known flagged skins
* Getting a skin's head such as ![Face of an example Minecraft skin head](https://nuc.de.majic.rs/api/megaskins/skin/head?hash=105020bea8081ddd92666ebc4b9b53ee972d2bc0bd3c87b33b08a2536c6a6440)
* And everything's documented on the [MegaSkins API](<https://nuc.de.majic.rs/megaskins/api>) page

## Setting up *(self-hosting)*

> [!NOTE]
> Self-hosting has the big downside of having to get the data (descriptions and skins) by yourself. Attempting to scrape or massively query public deployments might get you blocked.

1. Download the [latest release](<https://github.com/Onako2/MegaSkins/releases>) jar file
2. Put into an empty folder
3. run it with
    ```bash
    java -Xmx1G -jar the_best_filename_ever.jar
    ```
   > [!IMPORTANT]
   > You might need to allocate more memory if you have more flagged skins. Currently, all flagged skins are kept in memory in order to ensure great speed
4. Wait for the first initialization, then stop the server by typing shutdown or doing ctrl+c
5. Put your skin pngs into [skins/](skins) and your descriptions into [skins_description/](skins_description)
   > [!IMPORTANT]
   > The Minecraft skins must have a resolution of 64x64 pixels
6. Configure your [banned_words.txt](banned_words.txt) (this file contains all banned words, if a description contains any line of this document, it will be flagged)
7. Start the server and wait until flagged images have been loaded...
8. 🎉 Setup's finished

## Development

1. Open project with any gradle-compatible IDE. Recommended: JetBrains IntelliJ IDEA
2. 🎉 Start cooking

## Build

```bash
./gradlew build
```

## Deployment

Currently, the project is deployed on https://nuc.de.majic.rs/api/megaskins/  
You can open the web frontend on https://nuc.de.majic.rs/megaskins/

## Contact

How to contact in case issues arise:
* [Stardance](<https://stardance.space/r-ncs8m>) Slack (onako)
* GitHub Issues
* Contact email https://nuc.de.majic.rs/privacy/
