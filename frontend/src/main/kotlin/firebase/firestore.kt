@file:JsModule("firebase")
@file:JsQualifier("firestore")

package firebase

import firebase.app.App
import kotlin.js.Promise

external interface Firestore {

    fun <T> collection(collectionPath: String): CollectionReference<T>

    val CACHE_SIZE_UNLIMITED: Number
}

external interface CollectionReference<T> {
    val id: String
    val path: String
    fun get(options: GetOptions? = definedExternally): Promise<QuerySnapshot<T>>
}

external interface QueryDocumentSnapshot<T> {
    fun data(options: SnapshotOptions? = definedExternally): T
}

external interface SnapshotOptions

external interface QuerySnapshot<T> {
    val docs: Array<QueryDocumentSnapshot<T>>
    val empty: Boolean
    val metadata: SnapshotMetadata
}

external interface SnapshotMetadata {}

external interface GetOptions

external interface DocumentData