import kotlinx.coroutines.*
import kotlinx.css.*
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.*
import styled.css
import styled.styledButton
import styled.styledDiv
import kotlin.browser.document
import kotlin.browser.window

interface VideoListProps : RProps {
    var videos: List<Video>
    var selectedVideo:Video?
    var onSelectVideo:(Video)->Unit
}

interface VideoListState:RState {
}

class VideoList : RComponent<VideoListProps, VideoListState>() {
    override fun RBuilder.render() {
        for (video in props.videos) {
            p {
                key = video.id.toString()
                attrs {
                    onClickFunction = {
                        props.onSelectVideo(video)
                    }
                }
                if (video == props.selectedVideo) {
                    + "▶ "
                }
                +"${video.speaker}: ${video.title}"
            }
        }
    }
}

fun RBuilder.videoList(handler: VideoListProps.() -> Unit) = child(VideoList::class) { attrs(handler) }

data class Video(val id: Int, val title: String, val speaker: String, val videoUrl: String)

interface VideoPlayerProps : RProps {
    var video:Video
    var onWatchedButtonPressed:(Video)->Unit
    var unwatchedVideo:Boolean
}

class VideoPlayer: RComponent<VideoPlayerProps, RState>() {
    override fun RBuilder.render() {
        styledDiv {
            css {
                position = Position.absolute
                top = 10.px
                right = 10.px
            }
            h3 {
                + props.video.title
            }

            styledButton {
                css {
                    display = Display.block
                    backgroundColor = if (props.unwatchedVideo) Color.lightGreen else Color.red
                }
                attrs {
                    onClickFunction = {
                        props.onWatchedButtonPressed(props.video)
                    }
                }
                if (props.unwatchedVideo) {
                    + "Mark as watched"
                }
                else {
                    + "Mark as unwatched"
                }
            }

            styledDiv {
                css {
                    display = Display.flex
                    marginBottom = 10.px
                }
                EmailShareButton {
                    attrs.url = props.video.videoUrl
                    EmailIcon {
                        attrs.size = 32
                        attrs.round = true
                    }
                }

                TelegramShareButton {
                    attrs.url = props.video.videoUrl
                    TelegramIcon {
                        attrs.size = 32
                        attrs.round = true
                    }
                }
            }

            ReactPlayer {
                attrs.url = props.video.videoUrl
            }
        }
    }
}

fun RBuilder.videoPlayer(handler:VideoPlayerProps.()->Unit) = child(VideoPlayer::class) {attrs(handler)}

interface AppState: RState {
    var currentVideo: Video?
    var unwatchedVideos:List<Video>
    var watchedVideos:List<Video>
}



class App : RComponent<RProps, AppState>() {

    suspend fun fetchVideo(id: Int): Video =
            window.fetch("https://my-json-server.typicode.com/kotlin-hands-on/kotlinconf-json/videos/$id")
                    .await()
                    .json()
                    .await()
                    .unsafeCast<Video>()

    suspend fun fetchVideos() = coroutineScope {
        (1..25).map { id ->
            async {
                fetchVideo(id)
            }
        }.awaitAll()
    }

    override fun AppState.init() {
        unwatchedVideos = listOf()

        watchedVideos = listOf()
        val mainScope = MainScope()
        mainScope.launch {
            val videos = fetchVideos()
            setState {
                unwatchedVideos = videos
            }
        }

    }

    override fun RBuilder.render() {
        h1 {
            +"KotlinConf Explorer"
        }
        div {
            h3 {
                +"Videos to watch"
            }
            videoList {
                videos = state.unwatchedVideos
                selectedVideo = state.currentVideo
                onSelectVideo = {video->
                    setState {
                        currentVideo = video
                    }
                }
            }


            h3 {
                +"Videos watched"
            }
            videoList {
                videos = state.watchedVideos
                selectedVideo = state.currentVideo
                onSelectVideo = {video->
                    setState {
                        currentVideo = video
                    }
                }
            }
            state.currentVideo?.let {currentVideo->
                videoPlayer {
                    video = currentVideo
                    unwatchedVideo = currentVideo in state.unwatchedVideos
                    onWatchedButtonPressed = {
                        if (video in state.unwatchedVideos) {
                            setState {
                                unwatchedVideos -= video
                                watchedVideos += video
                            }
                        }
                        else {
                            setState {
                                unwatchedVideos += video
                                watchedVideos -= video
                            }
                        }
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