package nz.scuttlebutt.android_go.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sunrisechoir.patchql.Params
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.SendChannel
import nz.scuttlebutt.android_go.SsbServerMsg
import nz.scuttlebutt.android_go.models.PatchqlBackgroundMessage

class PostsViewModelFactory(
    private val patchqlParams: Params,
    private val ssbServer: CompletableDeferred<SendChannel<SsbServerMsg>>,
    private val patchqlBackgroundActor: CompletableDeferred<SendChannel<PatchqlBackgroundMessage>>
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return PostsViewModel(patchqlParams, ssbServer, patchqlBackgroundActor) as T
    }
}