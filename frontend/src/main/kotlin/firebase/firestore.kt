@file:JsModule("firebase")
@file:JsQualifier("firestore")

package firebase

import kotlin.js.Promise

external interface Firestore {

    fun <T> collection(collectionPath: String): CollectionReference<T>

    val CACHE_SIZE_UNLIMITED: Number
}

external interface Query<T> {
    fun get(options: GetOptions? = definedExternally): Promise<QuerySnapshot<T>>
    fun onSnapshot(next:(snapshot:QuerySnapshot<T>)->Unit ,error:(error:dynamic)->Unit = definedExternally, complete:()->Unit = definedExternally):()->Unit
}

external interface CollectionReference<T>:Query<T> {
    val id: String
    val path: String
    fun add(data: T): Promise<DocumentReference<T>>
    fun doc(documentPath: String? = definedExternally): DocumentReference<T>
}

external interface DocumentReference<T> {
    fun get(options: GetOptions? = definedExternally): Promise<DocumentSnapshot<T>>
    fun onSnapshot(next:(snapshot:DocumentSnapshot<T>)->Unit ,error:(error:dynamic)->Unit = definedExternally, complete:()->Unit = definedExternally):()->Unit
}

external interface DocumentSnapshot<T> {
    fun data(options: SnapshotOptions? = definedExternally): T?
}

external interface QueryDocumentSnapshot<T> {
    val exists: Boolean
    val id: String
    val metadata: SnapshotMetadata
    val ref: DocumentReference<T>
    fun data(options: SnapshotOptions? = definedExternally): T
}

external interface SnapshotOptions

external interface QuerySnapshot<T> {
    val docs: Array<QueryDocumentSnapshot<T>>
    val empty: Boolean
    val metadata: SnapshotMetadata
    fun forEach(callback: (data:T)->Unit)
}

external interface SnapshotMetadata {}

external interface GetOptions

external interface DocumentData