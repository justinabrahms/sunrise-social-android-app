package nz.scuttlebutt.android_go.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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

            markwon.setMarkdown(root.findViewById(R.id.root_post_text), thread.root.text)
            root.findViewById<TextView>(R.id.author_name_text).text = thread.root.authorName
            root.findViewById<TextView>(R.id.likes_count_text).text =
                thread.root.likesCount.toString()
            root.findViewById<TextView>(R.id.replies_count_text).text =
                thread.repliesLength.toString()

            root.findViewById<ImageView>(R.id.likes_icon_image).setOnClickListener {

                GlobalScope.launch {
                    withContext(Dispatchers.IO) {
                        val response = CompletableDeferred<Long>()
                        ssbServer.await().send(PublishLikeMessage(thread.root.id, true, response))
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