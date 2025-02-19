## [About](README.md)

<h1 align="center">tiered-cache</h1>

[![Github](https://img.shields.io/badge/GitHub-white.svg?style=flat-square&logo=github&logoColor=181717)](https://github.com/dawndev/tiered-cache)
![GitHub](https://img.shields.io/github/license/dawndev/tiered-cache)
![GitHub stars](https://img.shields.io/github/stars/dawndev/tiered-cache.svg)
![GitHub forks](https://img.shields.io/github/forks/dawndev/tiered-cache.svg)
![GitHub issues](https://img.shields.io/github/issues-raw/dawndev/tiered-cache?label=issues)
![GitHub last commit](https://img.shields.io/github/last-commit/dawndev/tiered-cache.svg)

<div align="center">

一个基于 `Caffeine` 和 `Redisson` 实现的轻量级分布式缓存框架。它提供了统一的缓存管理接口，并解决了分布式环境下的缓存一致性、缓存击穿、缓存雪崩等常见问题。


```
+----------------+  
|  应用层        |
+----------------+
        ↓
+----------------+     +-----------------+
| 本地缓存        |     |  Redis缓存       |
| (LOCAL)        |     |  (REMOTE)     |
+----------------+     +-----------------+

```

</div>

## Function
TODO

## Compile

如果你想要编译本项目, 你可能需要提前准备以下环境

| Target      | Version |
| ----------- |---------|
| JDK      | 11      |
| Kotlin   | 1.6.21  |
| Gradle   | 7.5.1   |

编译
```bash
gradlew clean buid
```
