package com.example.annotation

import com.squareup.javapoet.ClassName

data class AptManInfo(
    var name: String = "",
    var age: Int = -1,
    var country: ClassName? = null,
    var bodyInfo: BodyInfo? = null,
    var algorithm: ClassName? = null,
    var getInstance: Any? = null
) {
}
