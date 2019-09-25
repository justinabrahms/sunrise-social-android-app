package nz.scuttlebutt.android_go.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.noties.markwon.Markwon
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import nz.scuttlebutt.android_go.PublishLikeMessage
import nz.scuttlebutt.android_go.R
import nz.scuttlebutt.android_go.SsbServerMsg
import nz.scuttlebutt.android_go.databinding.FragmentThreadSummaryBinding
import nz.scuttlebutt.android_go.fragments.ThreadsFragmentDirections
import nz.scuttlebutt.android_go.models.Thread


class ThreadsAdapter(val ssbServer: CompletableDeferred<SendChannel<SsbServerMsg>>) :
    PagedListAdapter<Thread, RecyclerView.ViewHolder>(Thread.DIFF_CALLBACK) {

    private lateinit var markWon: Markwon

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder.
    // Each data item is just a string in this case that is shown in a TextView.
    class ThreadsViewHolder(
        private val binding: FragmentThreadSummaryBinding,
        private val markwon: Markwon,
        private val ssbServer: CompletableDeferred<SendChannel<SsbServerMsg>>,
        val navController: NavController
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bindTo(thread: Thread) {

            val root = binding.root

            binding.fragmentPost.thread = thread


            markwon.setMarkdown(binding.fragmentPost.rootPostText, thread.root.text)

            val likesIconImage = binding.fragmentPost.likesIconImage
            val image =
                if (thread.root.likedByMe) R.drawable.ic_favorite_fuscia_24dp else R.drawable.ic_favorite_border_black_24dp
            likesIconImage.setImageResource(image)


            likesIconImage.setOnClickListener {

                GlobalScope.launch {
                    withContext(Dispatchers.IO) {
                        val response = CompletableDeferred<Long>()
                        ssbServer.await().send(
                            PublishLikeMessage(
                                thread.root.id,
                                !thread.root.likedByMe,
                                response
                            )
                        )
                        println("got reponse from publishing message: ${response.await()}")
                    }
                }
            }

            root.setOnClickListener {
                if (navController.currentDestination?.id == R.id.threads_fragment) {
                    navController.navigate(
                        ThreadsFragmentDirections.actionThreadsFragmentToThreadFragment(
                            thread.root.id
                        )
                    )
                }
            }

        }

    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ThreadsViewHolder {
        markWon = Markwon.create(parent.context)

        val inflater = LayoutInflater.from(parent.context)
        val binding = FragmentThreadSummaryBinding.inflate(inflater)
        val navController = parent.findNavController()

        return ThreadsViewHolder(
            binding,
            markWon,
            ssbServer,
            navController
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        (holder as ThreadsViewHolder).bindTo(getItem(position)!!)
    }

}