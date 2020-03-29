import kotlinx.css.*
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.dom.*
import styled.css
import styled.styledDiv
import kotlin.browser.document

val unwatchedVideos = listOf(
        Video(1, "Building and breaking things", "John Doe", "https://youtu.be/PsaFVLr8t4E"),
        Video(2, "The development process", "Jane Smith", "https://youtu.be/PsaFVLr8t4E"),
        Video(3, "The Web 7.0", "Matt Miller", "https://youtu.be/PsaFVLr8t4E")
)

val watchedVideos = listOf(
        Video(4, "Mouseless development", "Tom Jerry", "https://youtu.be/PsaFVLr8t4E")
)

interface VideoListProps: RProps {
    var videos:List<Video>
}

class VideoList:RComponent<VideoListProps, RState>() {
    override fun RBuilder.render() {
        for(video in props.videos) {
            p {
                key = video.id.toString()
                +"${video.speaker}: ${video.title}"
            }
        }
    }
}

data class Video(val id: Int, val title: String, val speaker: String, val videoUrl: String)

class App: RComponent<RProps, RState>() {
    override fun RBuilder.render() {
        h1 {
            +"KotlinConf Explorer"
        }
        div {
            h3 {
                +"Videos to watch"
            }
            child(VideoList::class) {
                attrs.videos = unwatchedVideos
            }

            h3 {
                +"Videos watched"
            }
            child(VideoList::class) {
                attrs.videos = watchedVideos
            }

        }
        div {
            styledDiv {
                css {
                    position = Position.absolute
                    top = 10.px
                    right = 10.px
                }
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
}

fun main() {
    render(document.getElementById("root")) {
        child(App::class) {}
    }
}