import kotlinx.css.*
import kotlinx.html.js.onClickFunction
import kotlinx.html.onClick
import react.*
import react.dom.*
import styled.css
import styled.styledDiv
import kotlin.browser.document
import kotlin.browser.window

val unwatchedVideos = listOf(
        Video(1, "Building and breaking things", "John Doe", "https://youtu.be/PsaFVLr8t4E"),
        Video(2, "The development process", "Jane Smith", "https://youtu.be/PsaFVLr8t4E"),
        Video(3, "The Web 7.0", "Matt Miller", "https://youtu.be/PsaFVLr8t4E")
)

val watchedVideos = listOf(
        Video(4, "Mouseless development", "Tom Jerry", "https://youtu.be/PsaFVLr8t4E")
)

interface VideoListProps : RProps {
    var videos: List<Video>
}

interface VideoListState:RState {
    var selectedVideo:Video?
}

class VideoList : RComponent<VideoListProps, VideoListState>() {
    override fun RBuilder.render() {
        for (video in props.videos) {
            p {
                key = video.id.toString()
                attrs {
                    onClickFunction = {
                        setState { selectedVideo = video }
                    }
                }
                if (video == state.selectedVideo) {
                    + "▶ "
                }
                +"${video.speaker}: ${video.title}"
            }
        }
    }
}

fun RBuilder.videoList(handler: VideoListProps.() -> Unit): ReactElement = child(VideoList::class) {
    attrs(handler)
}

data class Video(val id: Int, val title: String, val speaker: String, val videoUrl: String)

interface AppState: RState {
    var currentVideo: Video?
}

class App : RComponent<RProps, AppState>() {
    override fun RBuilder.render() {
        h1 {
            +"KotlinConf Explorer"
        }
        div {
            h3 {
                +"Videos to watch"
            }
            videoList { videos = unwatchedVideos }


            h3 {
                +"Videos watched"
            }
            videoList { videos = watchedVideos }

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