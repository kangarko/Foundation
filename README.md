### Foundation is a library for bootstrapping Minecraft plugins.

Thousands of servers are running on Foundation since 2013. It has been battle tested and proven in plugins ChatControl, Boss, CoreArena, Confiscate, AutoPlay, Puncher, Winter, AnimeX and others.

### Sample usage

Please see [this link](https://github.com/kangarko/plugintemplate) for a sample plugin demostrating different Foundation abilities.

### Compatibility

We aim to provide extreme compatibility layer enabling these Minecraft versions to work:

- 1.2.5 (from 2012) - Of course, there are things that don't work due to lacking API, but it loads and you can build with Foundation!
- 1.3.2, 1.4.7, 1.5.2, 1.6.4
- 1.7.10, 1.8.8
- 1.9.x, 1.10.x, 1.11.x, 1.12.x
- 1.13.x, 1.14.x, 1.15.x, 1.16.x, 1.17.x
- We continously update for newer versions but sometimes forget to update this here, but it does not mean that the library is incompatible!

### Compiling and using

We use JitPack to automatically compile and host the latest release of Foundation for you.

#### a) Alternative A: If you don't have Foundation on your computer:

To install Foundation with Maven, open your pom.xml, locate the `<repositories>` section and place this repository within it:

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>
```

Then locate the `<dependencies>` section of your pom.xml and place the following inside of it. Replace the "REPLACE_WITH_LATEST_VERSION" string with the latest version from: https://github.com/Rubix327/Foundation/releases

```xml
<dependency>
    <groupId>com.github.Rubix327</groupId>
    <artifactId>Foundation</artifactId>
    <version>REPLACE_WITH_LATEST_VERSION</version>
</dependency>
```

For more information, including how to use Foundation with other tools than Maven, please visit: https://jitpack.io/#kangarko/Foundation/

#### b) Alternative B: If you have Foundation on your computer:

If you downloaded Foundation to your disk, do not place any repository to your pom.xml file, instead, only place the following dependency. Notice the groupId is different. You can use the LATEST keyword to automatically synchronize changes you make to your local copy of Foundation with your plugin source code (now that's fast!).

```xml
<dependency>
    <groupId>org.mineacademy</groupId>
    <artifactId>Foundation</artifactId>
    <version>LATEST</version>
</dependency>
```

### Important Licencing Information

This repository is forked from https://github.com/kangarko/Foundation.
See licencing information at the original repo.
