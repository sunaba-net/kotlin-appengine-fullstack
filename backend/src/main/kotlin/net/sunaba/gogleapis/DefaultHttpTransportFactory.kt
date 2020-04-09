package net.sunaba.gogleapis

import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.auth.http.HttpTransportFactory

/**
 * NetHttpTransportはスレッドセーフなので同じインスタンスを使い回したほうが効率がよい.
 *
 * https://googleapis.dev/java/google-http-client/latest/com/google/api/client/http/javanet/NetHttpTransport.html
 */
object DefaultHttpTransportFactory:HttpTransportFactory {

    val transport = NetHttpTransport()
    override fun create(): HttpTransport = transport
}
