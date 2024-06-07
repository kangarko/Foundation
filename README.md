<a href="https://bit.ly/3GHdIQI">
  <img src="https://i.imgur.com/AeprAug.jpg" />
</a>

[![](https://jitpack.io/v/kangarko/Foundation.svg)](https://jitpack.io/#kangarko/Foundation)

Make Minecraft plugins faster, save boilerplate code to focus on putting your ideas out there instead of dealing with limited Spigot/Bukkit/Paper APIs.

Some features include:

- Folia, Bukkit/Spigot/Paper support (1.2.5 - 1.20.x) - automatic version wrappers (i.e. call one method to send title/animation packets on all Minecraft versions)
- GUI menu APIs
- Advanced commands without using plugin.yml
- Auto-updating configuration with comments support
- Automatic libraries support: Packets, Discord, Citizens, Towny, etc.
- Time-saving wrappers: Databases (flatfile SQL, HikariCP, MySQL), holograms API, custom items and skulls API, and so much more! 

Thousands of servers are running on Foundation since 2013. It has been battle tested and proven in plugins ChatControl, Boss, CoreArena, Confiscate, AutoPlay, Puncher, Winter, AnimeX and others.

## PLEASE READ THE QUICKSTART IN FULL - THERE IS AN EXTRA STEP THAT IF YOU MISS THE PLUGIN WILL BREAK

# Quick Start

**New**: Check out this video tutorial on installing Foundation: https://www.youtube.com/watch?v=gXbZnKYE7ww 

1. Import Foundation using Maven/Gradle (see the Importing section).
2. **IMPORTANT - DO NOT MISS**: Configure shading to only include Foundation and the libraries you need otherwise all of our dependencies will be shaded to your jar! [See this link](https://github.com/kangarko/PluginTemplate/blob/master/pom.xml#L130) for sample usage.
3. Change "**extends JavaPlugin**" to "**extends SimplePlugin**" (we need that to register things and listeners on our end automatically)
4. Change **onEnable()** to **onPluginStart()** and **onDisable()** to **onPluginStop()** (we occupy these methods to perform logic)
5. If you use a **static getInstance()** method in your main plugin's class, change it to return **(T) SimplePlugin.getInstance()** where T is your plugin instead. Delete the instance of your plugin from your class if you use it (if you have myPlugin = this anywhere, remove it).

For a sample plugin, see [PluginTemplate](https://github.com/kangarko/plugintemplate).

A complete tutorial on how to use this library is a part of our Project Orion training available [here](https://mineacademy.org/project-orion)

If you just want a quick start into Minecraft plugin development, [check out this guick gist](https://gist.github.com/kangarko/456d9cfce52dc971b93dbbd12a95f43c).

## Importing

We use JitPack to automatically compile and host the latest release of Foundation for you. To install Foundation with Maven, open your pom.xml, locate the `<repositories>` section and place this repository within it:

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>
```

Then locate the `<dependencies>` section of your pom.xml and place the following inside of it. Replace the "REPLACE_WITH_LATEST_VERSION" string with the latest version from: https://github.com/kangarko/Foundation/releases

```xml
<dependency>
    <groupId>com.github.kangarko</groupId>
    <artifactId>Foundation</artifactId>
    <version>REPLACE_WITH_LATEST_VERSION</version>
</dependency>
```

## Shading (important!)

See step 2 from the Quick Start guide above first.

Foundation comes with some plugins available for you such as WorldEdit, etc. so that you can access them when you are coding but don't need to include them as dependencies on your own.

Maven has a limitation whereby these plugins will end up in your plugin .jar file if you don't configure the maven-shade-plugin's includes section properly.

If you are a beginner all that's needed is copy paste the following section and drop it into your `<plugins>` section of pom.xml (if you already have such section there, remove it).

**Make sure to change your.plugin.main.package below to your own package name.**

If you want to compile a dependency to your jar, install it normally through the `<dependency>` directive, set it's scope to "compile" and then include it again. You can just duplicate the `<include>` and change it for your dependency.

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>

    <!-- Change version to the latest one from
         https://mvnrepository.com/artifact/org.apache.maven.plugins/maven-shade-plugin -->
    <version>3.5.1</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>shade</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <createDependencyReducedPom>false</createDependencyReducedPom>
        <artifactSet>
            <includes>
                <!-- Important: This will ensure only Foundation is shaded to your jar. If you have
                     other dependencies that should be compiled, duplicate this line for each. 
                     
                     ONLY ADD THE LIBRARIES HERE YOU WANT TO BE INCLUDED IN YOUR PLUGIN.JAR
                     -->
                <include>com.kangarko.github:Foundation*</include>
            </includes>
        </artifactSet>
        <relocations>
            <!-- This moves Foundation into your own package in "lib" subpackage to prevent interference. -->
            <relocation>
                <pattern>org.mineacademy.fo</pattern>
                <shadedPattern>your.plugin.main.package.lib</shadedPattern>
            </relocation>
        </relocations>
    </configuration>
</plugin>
```

For more information, including how to use Foundation with other tools than Maven, please visit: https://jitpack.io/#kangarko/Foundation/

# Compatibility

We aim to provide broad compatibility layer enabling the below Minecraft versions to work:

- 1.2.5 (from 2012) - Limited, see mineacademy.org/oldmcsupport for setup instructions.
- 1.3.2, 1.4.7, 1.5.2, 1.6.4 - Many APIs are missing due to lack of features/access.
- 1.7.10
- 1.8.8
- 1.9.x, 1.10.x, 1.11.x, 1.12.x
- 1.13.x, 1.14.x, 1.15.x, 1.16.x, 1.17.x, 1.18.x, 1.19.x, 1.20.x
- We continously update for newer versions but sometimes forget to update it here, but it does not mean that the library is incompatible!

Foundation works on Bukkit, Spigot, Paper and as of recently also Folia (see the Wiki).

# Licencing Information

© MineAcademy.org

Tl;dl: You can do whatever you want as long as you don't claim Foundation as your own or don't sell or resell parts of it. If you are not a paying student of MineAcademy however, you MUST place a link to this GitHub page in your sales pages (example Overview pages on Spigot) if your paid software is using Foundation.

1) **If you are a paying student of MineAcademy.org** then you can use, modify and
reproduce Foundation both commercially and non-commercially for yourself, your team
or network without attribution.

4) **If you are not a paying student of MineAcademy.org** then you may
use this library as stated above however you must clearly attribute that you
are using Foundation in your software by linking to this GitHub page.

In both of the above cases, do not sell or claim any part of this library as your own.

No guarantee - this software is provided AS IS, without any guarantee on its
functionality. We made our best efforts to make Foundation an enterprise-level
solution for anyone looking to accelerate his coding however we are not
taking any responsibility for the success or failure you achieve using it.
