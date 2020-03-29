import react.dom.*
import vue.Vue
import vue.VueComponent
import kotlin.browser.document

import kotlinx.css.*
import styled.*
import vue.style


class ButtonCounter : VueComponent<ButtonCounter.CountModel>({
    template = """
        <button v-on:click="onClick" v-bind:style="buttonStyle()">
            You clicked me {{ count }} times.
        </button>
    """
    data = { CountModel() }
    styles {
        "test".style {
            color = Color.red
            fontSize = 200.px
        }
    }
}) {
    data class CountModel(var count: Int = 0)

    fun onClick() {
        data.count += 1
    }

    fun buttonStyle() = style {
        color = arrayOf(Color.red, Color("#00FF00"), rgb(0, 0, 255))[data.count % 3]
        fontSize = (20 + data.count).px
    }
}




fun main() {
    VueComponent("button-counter", ButtonCounter())
    Vue(object {
        val el = "#components-demo"
    })
}


data class Video(val id: Int, val title: String, val speaker: String, val videoUrl: String)

fun main2() {


    render(document.getElementById("root")) {
        h1 {
            +"KotlinConf Explorer"
        }
        div {
            styledDiv {
                css
            }
            h3 {
                +"Videos to watch"
            }
            p {
                +"John Doe: Building and breaking things"
            }
            p {
                +"Jane Smith: The development process"
            }
            p {
                +"Matt Miller: The Web 7.0"
            }

            h3 {
                +"Videos watched"
            }
            p {
                +"Tom Jerry: Mouseless development"
            }
        }
        div {
            h3 {
                +"John Doe: Building and breaking things"
            }
            img {
                attrs {
                    src = "https://via.placeholder.com/640x360.png?text=Video+Player+Placeholder"
                }
            }
        }
    }
}