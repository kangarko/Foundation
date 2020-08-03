<p align="center">
  Do you want to code using this library?
  <a href="https://mineacademy.org/gh-join">
    <img src="https://i.imgur.com/SuIyaDV.png" />
  </a>
</p>

### Foundation is a library for bootstrapping Minecraft plugins.
[![](https://jitpack.io/v/kangarko/Foundation.svg)](https://jitpack.io/#kangarko/Foundation)

Thousands of servers are running on Foundation since 2013. It has been battle tested and proven in plugins ChatControl, Boss, CoreArena, Confiscate, AutoPlay, Puncher, Winter, AnimeX and others.

Foundation has never been publicly released before MineAcademy. We decided to release its sources to the public and teach it to enable people develop plugins faster, saving boilerplate code and thus focus on what matters the most, putting their ideas out there.

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

Then locate the `<dependencies>` section of your pom.xml and place the following inside of it. Replace the "REPLACE_WITH_LATEST_VERSION" string with the latest version from: https://github.com/kangarko/Foundation/releases/tag/5.5.1

```xml
<dependency>
    <groupId>com.github.kangarko</groupId>
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

2013 - 2020 © MineAcademy.org

If you are a paying student of MineAcademy.org then you are granted full
unlimited licence to use, modify and reproduce Foundation both commercially
and non-commercially, for yourself, your team or network. You can also
modify the library however you like and include it in your plugins you publish
or sell without stating that you are using this library.

If you are not a paying student of MineAcademy.org then you may
use this library for non-commercial purposes only. You are allowed
to make changes to this library however as long as those are only
minor changes you must clearly attribute that you are using Foundation
in your software.

For both parties, do not sell or claim any part of this library as your own.
All infringements will be prosecuted.

No guarantee - this software is provided AS IS, without any guarantee on its
functionality. We made our best efforts to make Foundation an enterprise-level
solution for anyone looking to accelerate their coding however we are not
taking any responsibility for the success or failure you achieve using it.

**A tutorial on how to use this library is a part of our Project Orion training available now at https://mineacademy.org**
