package com.github.dawndev.tieredcache.constg

enum class TypeEnum(
    val label: String
) {
    NULL("null"),

    STRING("string"),

    OBJECT("Object 对象"),

    LIST("List集合"),

    SET("Set集合"),

    ARRAY("数组"),

    ENUM("枚举"),

    OTHER("其他类型"),

    ;

}