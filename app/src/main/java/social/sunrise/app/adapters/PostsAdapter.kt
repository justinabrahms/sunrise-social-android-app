package social.sunrise.app.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.noties.markwon.Markwon
import social.sunrise.app.NavigationDirections
import social.sunrise.app.R
import social.sunrise.app.databinding.FragmentThreadSummaryBinding
import social.sunrise.app.models.LIVE_DIFF_CALLBACK
import social.sunrise.app.models.Post
import java.util.*


class PostsAdapter(
    val likePost: (String, Boolean) -> Unit,
    val lifecycleOwner: LifecycleOwner,
    val markwon: Markwon,
    val getBlob: (String) -> LiveData<ByteArray>
) :
    PagedListAdapter<LiveData<Post>, RecyclerView.ViewHolder>(LIVE_DIFF_CALLBACK) {

    inner class PostsViewHolder(
        private val binding: FragmentThreadSummaryBinding,
        private val navController: NavController
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bindTo(livePost: LiveData<Post>) {

            val likesIconImage = binding.fragmentPost.likesIconImage
            val authorImage = binding.fragmentPost.authorImage

            livePost.observe(lifecycleOwner, Observer {
                binding.fragmentPost.post = it

                val image =
                    if (it.likedByMe) R.drawable.ic_favorite_fuscia_24dp else R.drawable.ic_favorite_border_black_24dp
                likesIconImage.setImageResource(image)
            })


            val post = livePost.value!!

            val assertedTime = post.assertedTime
            if (assertedTime != null) {
                binding.fragmentPost.postTimeTextView.setReferenceTime(Date(assertedTime).time)
            }

            binding.fragmentPost.authorImage.setImageResource(R.drawable.ic_person_black_24dp)
            if (post.authorImageLink != null) {
                getBlob(post.authorImageLink).observe(lifecycleOwner, Observer {
                    val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                    binding.fragmentPost.authorImage.setImageBitmap(bitmap)
                })
            }

            binding.fragmentPost.post = post

            markwon.setMarkdown(binding.fragmentPost.rootPostText, post.text)

            likesIconImage.setOnClickListener {
                // Get the latest post from the observable
                val post = livePost.value!!
                likePost(post.id, !post.likedByMe)
            }

            authorImage.setOnClickListener {
                val post = livePost.value!!
                navigateToAuthor(post.authorId)

            }
        }

        private fun navigateToAuthor(authorId: String) {
            if (navController.currentDestination?.id != R.id.thread_fragment)
                return
            navController.navigate(
                NavigationDirections.actionGlobalProfileFragment(authorId)
            )
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PostsViewHolder {


        val inflater = LayoutInflater.from(parent.context)
        val binding = FragmentThreadSummaryBinding.inflate(inflater)
        val navController = parent.findNavController()

        return PostsViewHolder(
            binding,
            navController
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as PostsViewHolder).bindTo(getItem(position)!!)
    }

}